import { expect, test } from "bun:test";
import { Effect } from "effect";
import { createPrismaClient } from "@outcomego/db";
import { pluginCatalog } from "@outcomego/plugins";
import { PluginInstallations } from "../../src/services/plugin/installations";
import { PluginQueries } from "../../src/services/plugin/queries";
import { processEnvironment, protectedPluginEnvironment, retainPluginEnvironment } from "../../src/services/process-secrets";

const databaseUrl = process.env.OUTCOMEGO_TEST_DATABASE_URL;

test.skipIf(!databaseUrl)("Bitwarden runtime fields install, rotate, stay private, inherit, conflict and uninstall in a disposable DB", async () => {
  const db = createPrismaClient(databaseUrl!);
  const original = pluginCatalog.find(plugin => plugin.key === "bitwarden")!;
  const suffix = crypto.randomUUID();
  const plugin = { ...original, key: `bitwarden-test-${suffix}` };
  const duplicate = { ...plugin, key: `bitwarden-duplicate-${suffix}`, name: "Duplicate vault" };
  const keys = [plugin.key, duplicate.key];
  const botId = crypto.randomUUID();
  const childId = crypto.randomUUID();
  const values = { serverUrl: "https://fixture-vault.example.test", email: "fixture@example.test", clientId: "fixture-client", clientSecret: "fixture-api-secret", masterPassword: "  fixture-master\n密碼  " };
  const installations = new PluginInstallations(db, async key => [plugin, duplicate].find(entry => entry.key === key), async () => {}, async () => {});
  const queries = new PluginQueries(db, async () => [plugin], async () => plugin, "https://app.example.test");
  const env = () => processEnvironment(db, botId);
  try {
    await db.bot.create({ data: { id: botId, name: "Vault fixture", defaultDirectory: "/workspace", conversation: { create: {} } } });
    const installed = await Effect.runPromise(installations.install(plugin.key, values));
    expect(await Effect.runPromise(installations.install(plugin.key, { masterPassword: "ignored" }))).toEqual(installed);
    expect(await env()).not.toHaveProperty("BW_SERVER_URL");
    expect(await protectedPluginEnvironment(db, plugin.key)).toMatchObject({ BW_SERVER_URL: values.serverUrl, BW_EMAIL: values.email, BW_CLIENT_ID: values.clientId, BW_CLIENT_SECRET: values.clientSecret, BW_MASTER_PASSWORD: values.masterPassword });
    const settings = await Effect.runPromise(queries.settings());
    expect(settings.installs.find(entry => entry.pluginKey === plugin.key)?.configuredEnvironment).toHaveLength(5);
    for (const value of Object.values(values)) expect(JSON.stringify(settings).includes(value)).toBe(false);
    await expect(Effect.runPromise(installations.install(duplicate.key, {}))).rejects.toThrow("already provides");
    await Effect.runPromise(installations.updateEnvironment(plugin.key, { clientSecret: "fixture-rotated" }));
    expect(await env()).not.toHaveProperty("BW_CLIENT_SECRET");
    expect(await protectedPluginEnvironment(db, plugin.key)).toMatchObject({ BW_CLIENT_SECRET: "fixture-rotated", BW_MASTER_PASSWORD: values.masterPassword });
    await db.processSecret.upsert({ where: { ownerKey_name: { ownerKey: `bot:${botId}`, name: "BW_EMAIL" } }, create: { ownerKey: `bot:${botId}`, name: "BW_EMAIL", value: "bot@example.test", botId }, update: { value: "bot@example.test" } });
    expect((await env()).BW_EMAIL).toBe("bot@example.test");
    await retainPluginEnvironment(db, plugin.key, ["BW_EMAIL"]);
    expect((await env()).BW_CLIENT_SECRET).toBeUndefined();
    await Effect.runPromise(installations.updateEnvironment(plugin.key, { clientSecret: "fixture-restored" }));
    await Effect.runPromise(installations.uninstall(plugin.key));
    expect((await env()).BW_CLIENT_SECRET).toBeUndefined();
    expect((await env()).BW_EMAIL).toBe("bot@example.test");
    expect(await db.processSecret.count({ where: { ownerKey: `plugin:${plugin.key}` } })).toBe(0);
  } finally {
    await db.pluginInstallation.deleteMany({ where: { pluginKey: { in: keys } } });
    await db.processSecret.deleteMany({ where: { ownerKey: { in: [...keys.map(key => `plugin:${key}`), `bot:${botId}`] } } });
    await db.bot.deleteMany({ where: { id: { in: [botId, childId] } } });
    await db.$disconnect();
  }
});
