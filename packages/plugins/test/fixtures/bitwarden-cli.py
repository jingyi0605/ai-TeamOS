#!/usr/bin/env python3
"""Fake CLI for deterministic wrapper tests; contains only synthetic credentials."""
import json
import os
import signal
import subprocess
import sys
import time
from pathlib import Path

args = [arg for arg in sys.argv[1:] if arg != "--nointeraction"]
if args == ["--version"]:
    print("2026.10.1")
    sys.exit(0)
stage = {"config": "server", "list": "search", "get": "read"}.get(args[0], args[0])
private = Path(os.environ["BITWARDENCLI_APPDATA_DIR"])
record = dict(args=args, stage=stage, private=str(private), pid=os.getpid(),
              mode=private.stat().st_mode & 0o777,
              clientId=os.environ.get("BW_CLIENTID") == "fixture-client-id",
              clientSecret=os.environ.get("BW_CLIENTSECRET") == "fixture-client-secret",
              masterPassword=os.environ.get("BW_MASTER_PASSWORD") == "  fixture-master-密\n  ",
              session=os.environ.get("BW_SESSION") == "fixture-session-key",
              tlsOverride="NODE_TLS_REJECT_UNAUTHORIZED" in os.environ,
              ambientSession=os.environ.get("BW_SESSION") == "ambient-wrong-session")
with open(os.environ["FAKE_BW_LOG"], "a") as log:
    log.write(json.dumps(record) + "\n")
(private / "data.json").write_text("synthetic-cli-state")
if stage == os.environ.get("FAKE_BW_FAIL_STAGE"):
    print(os.environ.get("FAKE_BW_ERROR", "provider error") + " fixture-client-secret fixture-session-key", file=sys.stderr)
    sys.exit(1)
if stage == "sync" and os.environ.get("FAKE_BW_SLEEP"):
    descendant = subprocess.Popen([sys.executable, "-c", "import time; time.sleep(60)"])
    with open(os.environ["FAKE_BW_LOG"], "a") as log:
        log.write(json.dumps(dict(descendant=descendant.pid, private=str(private))) + "\n")
    time.sleep(60)
if stage == "server":
    print("configured")
elif stage == "login":
    print("logged in")
elif stage == "status":
    print(json.dumps(dict(userEmail="other@example.test" if os.environ.get("FAKE_BW_WRONG_ACCOUNT") else "fixture@example.test", status="locked")))
elif stage == "unlock":
    print("fixture-session-key")
elif stage == "sync":
    print("synced")
else:
    item = dict(id="11111111-1111-1111-1111-111111111111", type=1, name="Example login",
                login=dict(username="fixture-user", password="  fixture-password-密\nline  ", totp="synthetic-totp-seed",
                           uris=[dict(uri="https://example.test/login")]))
    scenario = os.environ.get("FAKE_BW_CASE", "")
    if scenario == "missing":
        item["login"]["password"] = ""
    if scenario == "non-login":
        item["type"] = 2
    if scenario == "trash":
        item["deletedDate"] = "2026-11-13"
    if scenario == "mismatched-id":
        item["id"] = "22222222-2222-2222-2222-222222222222"
    if scenario == "unknown-schema":
        print('{"unexpected":"schema"}')
    elif scenario == "oversized":
        print("x" * (9 * 1024 * 1024))
    elif args[:2] == ["list", "items"]:
        second = dict(item, id="22222222-2222-2222-2222-222222222222", name="Second login",
                      login=dict(item["login"], uris=[dict(uri="https://example.test/other")]))
        foreign = dict(item, id="33333333-3333-3333-3333-333333333333", name="Other site",
                       login=dict(item["login"], uris=[dict(uri="https://example.test.evil.test/login"), dict(uri="https://sub.example.test/login")]))
        print(json.dumps([item, second, foreign] if scenario == "ambiguous" else [item, foreign]))
    elif args[:2] == ["get", "totp"]:
        print("123456")
    else:
        print(json.dumps(item))
