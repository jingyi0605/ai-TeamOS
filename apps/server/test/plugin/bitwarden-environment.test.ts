import { expect, test } from "bun:test";
import { Effect } from "effect";
import { pluginCatalog } from "@outcomego/plugins";
import { assertPluginEnvironmentAvailable, PluginInstallations } from "../../src/services/plugin/installations";
import { processEnvironment, protectedPluginEnvironment } from "../../src/services/process-secrets";

const bitwarden = pluginCatalog.find(plugin => plugin.key === "bitwarden")!;
const onePassword = pluginCatalog.find(plugin => plugin.key === "1password")!;

// Exercise the real service boundary while recording writes; real transaction tests are separate.
function fixture(plugin = bitwarden) {
  const writes: Array<{ name: string; value: string }> = [];
  const tx = {
    $queryRaw: async () => [{ manifest: plugin }],
    processSecret: { upsert: async ({ create }: any) => { writes.push(create); } },
  };
  const db = { $transaction: async (operation: (db: unknown) => Promise<unknown>) => operation(tx) };
  return { writes, service: new PluginInstallations(db as never, async () => plugin, async () => {}, async () => {}) };
}

test("Bitwarden runtime updates preserve master password whitespace and update only provided fields", async () => {
  const { writes, service } = fixture();
  const value = "  fixture master with spaces\n密碼  ";
  await Effect.runPromise(service.updateEnvironment("bitwarden", { masterPassword: value }));
  expect(writes).toHaveLength(1);
  expect(writes[0]).toMatchObject({ ownerKey: "plugin:bitwarden", name: "BW_MASTER_PASSWORD", value });
  await Effect.runPromise(service.updateEnvironment("bitwarden", { email: "  user@example.test  " }));
  expect(writes[1]).toMatchObject({ name: "BW_EMAIL", value: "user@example.test" });
  expect(writes[0]?.value).toBe(value);
});

test("invalid runtime URLs fail before storage with a field error that omits the value", async () => {
  const { writes, service } = fixture();
  for (const value of ["https://vault.example.test/path", "https://vault.example.test\n", "https://user:fixture-sensitive@vault.test", "https://vault.test?token=fixture-sensitive"]) {
    const error = await Effect.runPromise(service.updateEnvironment("bitwarden", { serverUrl: value })).catch(error => error);
    expect(String(error)).toContain("Server URL must be an HTTPS origin");
    expect(String(error)).not.toContain(value);
  }
  expect(writes).toEqual([]);
});

test("the existing 1Password token keeps its trim behavior", async () => {
  const { writes, service } = fixture(onePassword);
  await Effect.runPromise(service.updateEnvironment("1password", { serviceAccountToken: "  ops_fixture\n" }));
  expect(writes[0]).toMatchObject({ name: "OP_SERVICE_ACCOUNT_TOKEN", value: "ops_fixture" });
});

test("install and update share the runtime environment conflict check, excluding their own installation", async () => {
  const seen: unknown[] = [];
  const db = { pluginInstallation: { findMany: async (input: unknown) => {
    seen.push(input);
    return [{ name: "Existing vault", manifest: { ...bitwarden, key: "existing-vault" } }];
  } } };
  await expect(assertPluginEnvironmentAvailable(db as never, bitwarden)).rejects.toMatchObject({ code: "plugin_environment_conflict" });
  expect(seen[0]).toMatchObject({ where: { pluginKey: { not: "bitwarden" } } });
  await expect(assertPluginEnvironmentAvailable(db as never, onePassword)).resolves.toBeUndefined();
});

test("protected Bitwarden fields never enter ordinary process environments", async () => {
  const records = [
    { ownerKey: "plugin:bitwarden", name: "BW_EMAIL", value: "user@example.test", protected: true },
    { ownerKey: "plugin:bitwarden", name: "BW_MASTER_PASSWORD", value: "master", protected: true },
    { ownerKey: "personal", name: "PERSONAL_TOKEN", value: "visible", protected: false },
  ];
  const db = {
    subagent: { findUnique: async () => null },
    processSecret: { findMany: async (input: any) => input?.where?.ownerKey ? records.filter((record) => record.ownerKey === input.where.ownerKey && record.protected) : records },
  };
  expect(await processEnvironment(db as never, "bot")).toEqual({ PERSONAL_TOKEN: "visible" });
  expect(await protectedPluginEnvironment(db as never)).toEqual({ BW_EMAIL: "user@example.test", BW_MASTER_PASSWORD: "master" });
});
