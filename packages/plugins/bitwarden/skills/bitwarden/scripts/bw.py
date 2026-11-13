"""Field-only Bitwarden CLI workflow. Provider output never becomes diagnostics."""
import json
import os
import re
import selectors
import signal
import subprocess
import sys
import tempfile
import time
from pathlib import Path
from urllib.parse import urlsplit

MAX_OUTPUT = 8 * 1024 * 1024
TIMEOUT = 90
UUID = re.compile(r"^[0-9a-fA-F]{8}(?:-[0-9a-fA-F]{4}){3}-[0-9a-fA-F]{12}$")
FIELDS = ("BW_SERVER_URL", "BW_EMAIL", "BW_CLIENT_ID", "BW_CLIENT_SECRET", "BW_MASTER_PASSWORD")
ACTIONS = ("status", "search", "username", "password", "totp")


class Failure(Exception):
    def __init__(self, code, stage, message, retryable=False, **detail):
        self.result = dict(ok=False, errorCode=code, stage=stage, message=message, retryable=retryable)
        if detail:
            self.result["detail"] = detail


def fail(code, stage, message, retryable=False, **detail):
    raise Failure(code, stage, message, retryable, **detail)


def origin(value):
    try:
        url = urlsplit(value)
        if (not value.lower().startswith("https://") or re.search(r"[\s\x00-\x1f\x7f\\?#]", value)
                or not url.hostname or url.username is not None or url.password is not None
                or url.path not in ("", "/") or url.query or url.fragment or not url.port and url.netloc.endswith(":")):
            raise ValueError()
        return "https://" + url.netloc
    except ValueError:
        fail("plugin_setup_invalid", "configure", "BW_SERVER_URL must be an HTTPS origin without credentials, path, query or fragment.", field="BW_SERVER_URL")


def parse_args(args):
    if not args or args[0] not in ACTIONS:
        fail("read_only_violation", "configure", "Supported actions: status, search, username, password, totp.")
    action = args[0]
    title = action == "search" and len(args) == 3 and args[1] == "--title"
    expected = 1 if action == "status" else 3 if title else 2
    if len(args) != expected:
        fail("plugin_setup_invalid", "configure", "Invalid action arguments; search accepts a hostname/HTTPS URL or --title query, field reads require an item UUID.")
    value = args[-1] if action != "status" else ""
    if action != "status" and (not value.strip() or len(value) > 2048 or re.search(r"[\x00-\x1f\x7f]", value) or value.startswith("-")):
        fail("plugin_setup_invalid", "configure", "Invalid query or item identifier.")
    if action in ("username", "password", "totp") and not UUID.fullmatch(value):
        fail("plugin_setup_invalid", "configure", "Field reads require an item UUID from search.")
    if action == "search" and not title:
        value = website(value)
    return action, value, title


def website(value):
    try:
        url = urlsplit(value if "://" in value else "https://" + value)
        if (url.scheme != "https" or not url.hostname or url.username is not None or url.password is not None
                or re.search(r"[\s\x00-\x1f\x7f\\]", value)):
            raise ValueError()
        # Access the port to reject malformed port input before invoking the CLI.
        _ = url.port
        return url
    except ValueError:
        fail("plugin_setup_invalid", "configure", "Search requires a hostname or HTTPS website URL; use --title for a title query.")


def json_value(raw, stage):
    try:
        return json.loads(raw)
    except (ValueError, UnicodeError):
        fail("provider_incompatible", stage, "The CLI returned an unrecognized response.")


def cli_error(stage, raw, code):
    text = raw.decode("utf-8", errors="replace").lower()
    detail = dict(exitCode=code)
    if stage == "install" and ("checksum mismatch" in text or "version mismatch" in text or "unexpected cli version" in text):
        fail("cli_integrity_failed", stage, "The managed CLI failed its integrity or version check.", **detail)
    if any(marker in text for marker in ("certificate", "cert_has_expired", "self signed", "unable_to_verify", "err_tls", "ssl")):
        fail("tls_untrusted", stage, "TLS verification failed; configure a trusted certificate on the bot computer.", **detail)
    for reason, markers in (("dns", ("enotfound", "eai_again", "getaddrinfo")),
                            ("connection_refused", ("econnrefused", "connection refused")),
                            ("timeout", ("etimedout", "timed out", "timeout")),
                            ("connection", ("econnreset", "fetch failed", "network", "socket hang up"))):
        if any(marker in text for marker in markers):
            fail("server_unreachable", stage, "The bot computer could not reach the vault server.", True, reason=reason, **detail)
    if any(marker in text for marker in ("two-step", "two factor", "two-factor", "captcha", "device approval", "sso required")):
        fail("interactive_auth_required", stage, "The account requires authentication this skill cannot automate.", **detail)
    if "403" in text or "forbidden" in text or "permission denied" in text:
        fail("permission_denied", stage, "The provider denied access.", **detail)
    if stage == "read" and ("not found" in text or "404" in text):
        fail("item_not_found", stage, "The requested login item was not found.", **detail)
    error = {"install": "cli_unavailable", "server": "server_unreachable", "login": "login_failed", "unlock": "vault_locked", "sync": "sync_failed"}.get(stage, "read_failed")
    fail(error, stage, "The CLI could not complete the " + stage + " step.", stage in ("server", "sync"), **detail)


class Runner:
    def __init__(self):
        self.child = None

    def stop(self):
        if self.child is not None:
            try:
                os.killpg(self.child.pid, signal.SIGKILL)
            except ProcessLookupError:
                pass
            self.child.wait()
            self.child = None

    def cancel(self, _number, _frame):
        self.stop()
        fail("cancelled", "runtime", "The credential read was cancelled.")

    def run(self, args, env, stage, timeout=TIMEOUT):
        try:
            self.child = subprocess.Popen(args, env=env, stdin=subprocess.DEVNULL,
                                          stdout=subprocess.PIPE, stderr=subprocess.PIPE, start_new_session=True)
        except OSError:
            fail("cli_unavailable", stage, "The managed CLI could not be started.")
        out, err = bytearray(), bytearray()
        deadline = time.monotonic() + timeout
        try:
            with selectors.DefaultSelector() as poll:
                poll.register(self.child.stdout, selectors.EVENT_READ, out)
                poll.register(self.child.stderr, selectors.EVENT_READ, err)
                while poll.get_map():
                    if time.monotonic() >= deadline:
                        fail("server_unreachable" if stage != "install" else "cli_unavailable", stage, "The CLI command timed out.", True, reason="timeout")
                    for key, _ in poll.select(min(0.2, max(0, deadline - time.monotonic()))):
                        chunk = os.read(key.fileobj.fileno(), 65536)
                        if chunk:
                            key.data.extend(chunk)
                            if len(out) + len(err) > MAX_OUTPUT:
                                fail("output_limit", stage, "The CLI response exceeded the output limit; narrow the query.")
                        else:
                            poll.unregister(key.fileobj)
                remaining = deadline - time.monotonic()
                if remaining <= 0:
                    fail("server_unreachable", stage, "The CLI command timed out.", True, reason="timeout")
                try:
                    code = self.child.wait(timeout=remaining)
                except subprocess.TimeoutExpired:
                    fail("server_unreachable", stage, "The CLI command timed out.", True, reason="timeout")
            if code:
                cli_error(stage, bytes(err + out), code)
            return bytes(out)
        finally:
            self.stop()


def login_item(item):
    if not isinstance(item, dict) or not UUID.fullmatch(str(item.get("id", ""))):
        fail("provider_incompatible", "read", "The CLI returned an invalid login item.")
    if item.get("type") != 1:
        fail("item_type_unsupported", "read", "The selected item is not a Login item.")
    if item.get("deletedDate"):
        fail("item_not_found", "read", "The login item is in the trash.")
    if not isinstance(item.get("login"), dict):
        fail("provider_incompatible", "read", "The login item has an invalid schema.")
    return item


def item_urls(item):
    result = []
    for uri in item.get("login", {}).get("uris") or []:
        try:
            url = urlsplit(uri.get("uri") or "")
            if url.scheme in ("https", "http") and url.hostname:
                result.append(url)
        except (ValueError, AttributeError):
            continue
    return result


def find_items(items, query, title):
    if not isinstance(items, list):
        fail("provider_incompatible", "search", "The CLI returned an invalid item list.")
    candidates, exact = [], []
    for item in items:
        if not isinstance(item, dict):
            fail("provider_incompatible", "search", "The CLI returned an invalid item list.")
        if item.get("type") != 1 or item.get("deletedDate"):
            continue
        login_item(item)
        urls = item_urls(item)
        if title:
            matched = query.casefold() in str(item.get("name", "")).casefold()
        else:
            matched = any(url.hostname.lower().rstrip(".") == query.hostname.lower().rstrip(".") for url in urls)
        if matched:
            candidates.append((item, urls))
            if not title and any(url.geturl() == query.geturl() for url in urls):
                exact.append((item, urls))
    candidates = exact or candidates
    if not candidates:
        fail("item_not_found", "search", "No matching Login item was found.")
    if len(candidates) > 50:
        fail("output_limit", "search", "More than 50 logins match; narrow the title query or specify an item UUID.")
    summaries = [dict(itemId=item["id"], title=str(item.get("name", ""))[:200],
                      hostnames=sorted({url.hostname.lower() for url in urls})[:20]) for item, urls in candidates]
    summaries.sort(key=lambda item: (item["title"].casefold(), item["itemId"]))
    return dict(ok=True, kind="search", ambiguous=len(summaries) > 1, candidates=summaries)


def workflow(args, environment=None):
    action, query, title = parse_args(args)
    configured = dict(os.environ if environment is None else environment)
    for field in FIELDS:
        if not configured.get(field):
            fail("plugin_setup_invalid", "configure", "A required runtime field is missing.", field=field)
    server = origin(configured["BW_SERVER_URL"].strip())
    runner = Runner()
    previous = {sig: signal.signal(sig, runner.cancel) for sig in (signal.SIGTERM, signal.SIGINT)}
    # Do not reuse ambient sessions, CLI storage or disabled TLS verification.
    env = {key: value for key, value in configured.items() if not key.startswith(("BW_", "BITWARDENCLI_"))
           and key not in ("NODE_TLS_REJECT_UNAUTHORIZED", "NODE_OPTIONS")}
    env["BITWARDENCLI_DEBUG"] = "false"
    try:
        installer = str(Path(__file__).with_name("ensure-cli.sh"))
        executable = runner.run(["bash", installer, "--install"], env, "install", 150).decode().strip()
        if not executable or not os.path.isabs(executable):
            fail("cli_unavailable", "install", "The installer did not return a managed CLI path.")
        temporary_root = "/dev/shm" if sys.platform == "linux" and os.access("/dev/shm", os.W_OK) else None
        with tempfile.TemporaryDirectory(prefix="outcomego-bw-", dir=temporary_root) as private:
            env["BITWARDENCLI_APPDATA_DIR"] = private
            def cli(arguments, stage, extra=None):
                return runner.run([executable, *arguments, "--nointeraction"], {**env, **(extra or {})}, stage)
            cli(["config", "server", server], "server")
            cli(["login", "--apikey"], "login", dict(BW_CLIENTID=configured["BW_CLIENT_ID"], BW_CLIENTSECRET=configured["BW_CLIENT_SECRET"]))
            status = json_value(cli(["status"], "login"), "login")
            if not isinstance(status, dict) or not isinstance(status.get("userEmail"), str):
                fail("provider_incompatible", "login", "The CLI did not identify the authenticated account.")
            if status["userEmail"].casefold() != configured["BW_EMAIL"].strip().casefold():
                fail("login_failed", "login", "The API key belongs to a different account than BW_EMAIL.")
            session = cli(["unlock", "--passwordenv", "BW_MASTER_PASSWORD", "--raw"], "unlock", dict(BW_MASTER_PASSWORD=configured["BW_MASTER_PASSWORD"])).decode().strip()
            if not session or len(session) > 4096 or re.search(r"\s", session):
                fail("vault_locked", "unlock", "The CLI did not return a valid session key.")
            env["BW_SESSION"] = session
            cli(["sync"], "sync")
            if action == "status":
                return dict(ok=True, kind="status", serverOrigin=server, authenticated=True, unlocked=True, synced=True)
            if action == "search":
                return find_items(json_value(cli(["list", "items"], "search"), "search"), query, title)
            item = login_item(json_value(cli(["get", "item", query], "read"), "read"))
            if item["id"].lower() != query.lower():
                fail("provider_incompatible", "read", "The CLI returned a different item.")
            value = item["login"].get(action)
            if action == "totp" and value:
                value = cli(["get", "totp", query], "read").decode().strip()
            if not isinstance(value, str) or not value:
                fail("field_missing", "read", "The Login item does not contain the requested field.")
            if len(value) > 20000:
                fail("output_limit", "read", "The requested field exceeds the output limit.")
            return dict(ok=True, kind=action, itemId=query, value=value)
    finally:
        runner.stop()
        for sig, handler in previous.items():
            signal.signal(sig, handler)


def main():
    try:
        result = workflow(sys.argv[1:])
    except Failure as error:
        result = error.result
    except Exception:
        # Never print a provider traceback, raw response, argument or environment value.
        result = dict(ok=False, errorCode="provider_incompatible", stage="runtime", message="The credential read could not be completed.", retryable=False)
    print(json.dumps(result, ensure_ascii=True))
    return 0 if result["ok"] else 1


if __name__ == "__main__":
    sys.exit(main())
