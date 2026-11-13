import { expect, test } from "bun:test";
import { chmod, copyFile, mkdir, mkdtemp, readFile, rm, stat, writeFile } from "node:fs/promises";
import { tmpdir } from "node:os";
import { join } from "node:path";
import { exportPackage, importPackage, skillIsForAgent, validateValues } from "@outcomego/plugin-sdk";
import { pluginCatalog } from "../src";

const scripts = new URL("../bitwarden/skills/bitwarden/scripts/", import.meta.url).pathname;
const itemId = "11111111-1111-1111-1111-111111111111";
const plugin = pluginCatalog.find((entry) => entry.key === "bitwarden")!;
const runtime = {
  BW_SERVER_URL: "https://runtime-only-vault.fixture.invalid", BW_EMAIL: "fixture@example.test",
  BW_CLIENT_ID: "fixture-client-id", BW_CLIENT_SECRET: "fixture-client-secret",
  BW_MASTER_PASSWORD: "  fixture-master-密\n  ",
};

test("Bitwarden packages a portable main/computerUse skill, five runtime fields and no connector", () => {
  expect(plugin.components).toEqual(["skills"]);
  expect(plugin.connections).toEqual([]);
  expect(plugin.setupFields?.map((field) => field.environment)).toEqual(Object.keys(runtime));
  expect(plugin.setupFields?.find((field) => field.key === "masterPassword")?.preserveWhitespace).toBe(true);
  expect(skillIsForAgent(plugin.skills[0]!, "main")).toBe(true);
  expect(skillIsForAgent(plugin.skills[0]!, "computerUse")).toBe(true);
  expect(skillIsForAgent(plugin.skills[0]!, "executor")).toBe(false);
  const imported = importPackage(exportPackage(plugin)).definition;
  for (const file of ["bw.sh", "bw.py", "ensure-cli.sh"])
    expect(imported.files?.[`skills/bitwarden/scripts/${file}`]).toBeTruthy();
  expect(plugin.skills[0]!.body).toContain("secret: true");
  expect(plugin.skills[0]!.body).toContain("RequestBitwardenReadApproval");
  expect(plugin.skills[0]!.body).toContain("每个 item、每次任务");
  expect(plugin.skills[0]!.body).toContain("多个候选");
  expect(Object.values(runtime).some(value => JSON.stringify(plugin).includes(value))).toBe(false);
});

test("HTTPS origin validation rejects credentials, path, query, fragments and control characters", () => {
  const field = plugin.setupFields!.find((field) => field.key === "serverUrl")!;
  for (const value of ["https://vault.example.test", "https://vault.example.test/", "https://[::1]:8443"])
    expect(validateValues([field], { serverUrl: value }).serverUrl).toBe(value);
  for (const value of ["http://vault.example.test", "https://user:password@vault.test", "https://vault.test/path", "https://vault.test?", "https://vault.test#", "https://vault.test/\n", "https://vault.test\\evil", "file:///vault", "https://vault.test:bad"])
    expect(() => validateValues([field], { serverUrl: value })).toThrow("HTTPS origin");
});

async function fixture(run: (f: { root: string; env: Record<string, string | undefined>; log: string }) => Promise<void>) {
  const root = await mkdtemp(join(tmpdir(), "bitwarden-wrapper-"));
  const cli = join(root, "managed CLI");
  const log = join(root, "calls.jsonl");
  await mkdir(cli);
  await copyFile(new URL("./fixtures/bitwarden-cli.py", import.meta.url), join(cli, "bw"));
  await chmod(join(cli, "bw"), 0o700);
  await writeFile(log, "");
  try {
    await run({ root, log, env: { ...process.env, ...runtime, OUTCOMEGO_BW_CLI_DIR: cli, FAKE_BW_LOG: log,
      BW_SESSION: "ambient-wrong-session", BITWARDENCLI_APPDATA_DIR: "/unrelated-user-config",
      NODE_TLS_REJECT_UNAUTHORIZED: "0" } });
  } finally { await rm(root, { recursive: true, force: true }); }
}
function start(args: string[], env: Record<string, string | undefined>) {
  return Bun.spawn(["bash", join(scripts, "bw.sh"), ...args], { env, stdout: "pipe", stderr: "pipe" });
}
async function result(child: ReturnType<typeof start>) {
  const [code, stdout, stderr] = await Promise.all([child.exited, new Response(child.stdout).text(), new Response(child.stderr).text()]);
  expect(stderr).toBe("");
  return { code, stdout, json: JSON.parse(stdout) };
}
const records = async (log: string): Promise<any[]> => (await readFile(log, "utf8")).trim().split("\n").filter(Boolean).map(line => JSON.parse(line));

test("every action configures, logs in, validates the account, unlocks and syncs without persisting a session", async () => {
  await fixture(async ({ env, log }) => {
    const { code, json } = await result(start(["password", itemId], env));
    expect(code).toBe(0);
    expect(json).toEqual({ ok: true, kind: "password", itemId, value: "  fixture-password-密\nline  " });
    const calls = await records(log);
    expect(calls.map(call => call.stage)).toEqual(["server", "login", "status", "unlock", "sync", "read"]);
    expect(calls.every(call => call.mode === 0o700 && !call.tlsOverride && !call.ambientSession)).toBe(true);
    expect(calls[1].clientId && calls[1].clientSecret).toBe(true);
    expect(calls[3].masterPassword).toBe(true);
    expect(calls[4].session).toBe(true);
    expect(JSON.stringify(calls)).not.toContain("fixture-session-key");
    expect(JSON.stringify(calls)).not.toContain("fixture-client-secret");
    expect(await stat(calls[0].private).catch(() => null)).toBeNull();
    const status = await result(start(["status"], env));
    expect(status.json).toEqual({ ok: true, kind: "status", serverOrigin: runtime.BW_SERVER_URL, authenticated: true, unlocked: true, synced: true });
    expect((await result(start(["username", itemId], env))).json.value).toBe("fixture-user");
    expect((await result(start(["totp", itemId], env))).json.value).toBe("123456");
  });
});

test("search uses exact hostnames, explicit titles and complete URI priority, and only returns metadata", async () => {
  await fixture(async ({ env, log }) => {
    const unique = await result(start(["search", "example.test"], env));
    expect(unique.json.candidates.map((item: any) => item.itemId)).toEqual([itemId]);
    expect(unique.stdout).not.toContain("fixture-password");
    expect(unique.stdout).not.toContain("fixture-user");
    const multiple = await result(start(["search", "example.test"], { ...env, FAKE_BW_CASE: "ambiguous" }));
    expect(multiple.json.ambiguous).toBe(true);
    expect(multiple.json.candidates).toHaveLength(2);
    const exact = await result(start(["search", "https://example.test/login"], { ...env, FAKE_BW_CASE: "ambiguous" }));
    expect(exact.json.ambiguous).toBe(false);
    expect(exact.json.candidates).toHaveLength(1);
    expect((await result(start(["search", "--title", "Example login"], env))).json.candidates).toHaveLength(1);
    expect((await result(start(["search", "absent.test"], env))).json.errorCode).toBe("item_not_found");
    expect((await records(log)).some(call => call.stage === "read")).toBe(false);
  });
});

test("invalid actions and arguments fail before installing or accessing a vault", async () => {
  await fixture(async ({ env, log }) => {
    for (const args of [["create"], ["delete", itemId], ["export"], ["password", "--help"], ["password", itemId, "--session", "bad"], ["search", ""], ["search", "https://user:pass@example.test"], ["status", "extra"]]) {
      expect((await result(start(args, env))).code).toBe(1);
    }
    expect((await result(start(["status"], { ...env, BW_MASTER_PASSWORD: "" }))).json.detail.field).toBe("BW_MASTER_PASSWORD");
    expect((await result(start(["status"], { ...env, BW_SERVER_URL: "http://vault.test" }))).json.errorCode).toBe("plugin_setup_invalid");
    expect(await records(log)).toEqual([]);
  });
});

test("network, authentication, sync and provider failures never forward raw CLI output or stale results", async () => {
  await fixture(async ({ env, log }) => {
    for (const [stage, error, expected] of [
      ["login", "invalid API key", "login_failed"], ["unlock", "could not unlock", "vault_locked"],
      ["sync", "unknown provider failure", "sync_failed"], ["server", "getaddrinfo ENOTFOUND", "server_unreachable"],
      ["login", "ECONNREFUSED", "server_unreachable"], ["login", "ETIMEDOUT", "server_unreachable"],
      ["login", "self signed certificate", "tls_untrusted"], ["login", "two-factor required", "interactive_auth_required"],
      ["read", "403 forbidden", "permission_denied"], ["read", "404 not found", "item_not_found"],
    ]) {
      const failure = await result(start(["password", itemId], { ...env, FAKE_BW_FAIL_STAGE: stage, FAKE_BW_ERROR: error }));
      expect(failure.code).toBe(1);
      expect(failure.json.errorCode).toBe(expected);
      expect(failure.json.stage).toBe(stage);
      expect(failure.stdout).not.toContain("fixture-client-secret");
      expect(failure.stdout).not.toContain("fixture-session-key");
      expect(failure.json.value).toBeUndefined();
    }
    const calls = await records(log);
    for (const call of calls) expect(await stat(call.private).catch(() => null)).toBeNull();
    expect((await result(start(["password", itemId], env))).code).toBe(0);
    expect((await result(start(["status"], { ...env, FAKE_BW_WRONG_ACCOUNT: "1" }))).json.errorCode).toBe("login_failed");
  });
});

test("field reads reject non-login, deleted, mismatched, missing and unbounded provider data", async () => {
  await fixture(async ({ env }) => {
    for (const [scenario, expected] of [["non-login", "item_type_unsupported"], ["trash", "item_not_found"], ["mismatched-id", "provider_incompatible"], ["missing", "field_missing"], ["unknown-schema", "provider_incompatible"], ["oversized", "output_limit"]])
      expect((await result(start(["password", itemId], { ...env, FAKE_BW_CASE: scenario }))).json.errorCode).toBe(expected);
  });
});

test("parallel reads use separate private CLI directories", async () => {
  await fixture(async ({ env, log }) => {
    const reads = await Promise.all([result(start(["username", itemId], env)), result(start(["totp", itemId], env))]);
    expect(reads.every(read => read.code === 0)).toBe(true);
    const dirs = new Set((await records(log)).map(call => call.private));
    expect(dirs.size).toBe(2);
    for (const dir of dirs) expect(await stat(dir).catch(() => null)).toBeNull();
  });
});

test("cancelling a read kills CLI descendants and cleans transient state", async () => {
  await fixture(async ({ env, log }) => {
    const child = start(["password", itemId], { ...env, FAKE_BW_SLEEP: "1" });
    const completion = result(child);
    let descendant: any;
    try {
      for (let attempt = 0; attempt < 200; attempt++) {
        descendant = (await records(log)).find(call => call.descendant);
        if (descendant) break;
        await Bun.sleep(20);
      }
      expect(descendant).toBeTruthy();
      child.kill("SIGTERM");
      expect((await completion).json.errorCode).toBe("cancelled");
      expect(await stat(descendant.private).catch(() => null)).toBeNull();
      await Bun.sleep(50);
      expect(() => process.kill(descendant.descendant, 0)).toThrow();
    } finally { child.kill("SIGKILL"); }
  });
}, 10000);
