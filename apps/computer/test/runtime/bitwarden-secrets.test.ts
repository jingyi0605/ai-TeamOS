import { expect, test } from "bun:test";
import { BitwardenSecretStore } from "../../src/runtime/bitwarden-secrets";
import { ComputerRuntime } from "../../src/runtime";

test("stores an approved value under an opaque environment variable and resolves only the reference", () => {
  const environment: Record<string, string | undefined> = {};
  const removed: Array<[string, string]> = [];
  const store = new BitwardenSecretStore(environment, 60_000, (scope, value) => removed.push([scope, value]));
  const variable = store.put("screen-1", "password", "vault-value");

  expect(variable).toMatch(/^OUTCOMEGO_BITWARDEN_PASSWORD_\w+$/);
  expect(environment[variable]).toBe("vault-value");
  expect(store.resolve("screen-1", `$${variable}`)).toEqual({ variable, value: "vault-value" });
  expect(store.resolve("screen-1", "vault-value")).toBeNull();
  expect(store.resolve("screen-2", `$${variable}`)).toBeNull();

  store.clearScope("screen-1");
  expect(environment[variable]).toBeUndefined();
  expect(removed).toEqual([["screen-1", "vault-value"]]);
});

test("replaces a previous value for the same field and removes expired values", async () => {
  const environment: Record<string, string | undefined> = {};
  const removed: string[] = [];
  const store = new BitwardenSecretStore(environment, 1_000, (_scope, value) => removed.push(value));
  const first = store.put("screen-1", "username", "first");
  const second = store.put("screen-1", "totp", "second");

  expect(environment[first]).toBe("first");
  expect(store.resolve("screen-1", `$${first}`)?.value).toBe("first");
  expect(store.resolve("screen-1", `\${${second}}`)?.value).toBe("second");

  await new Promise((resolve) => setTimeout(resolve, 1_100));
  expect(environment[second]).toBeUndefined();
  expect(store.resolve("screen-1", `$${second}`)).toBeNull();
  expect(removed).toContain("first");
  expect(removed).toContain("second");
});

test("browser arguments resolve the reference inside Computer and cleanup removes it", () => {
  const runtime = new ComputerRuntime();
  const tools = (runtime as any).tools;
  const active = { botId: "bot-1", runId: "run-1", screenBotId: "screen-1" };
  const variable = tools.bitwardenSecrets.put(tools.bitwardenScope(active), "password", "browser-only-value");

  expect(tools.resolveBitwardenBrowserArguments(active, "browser_type", {
    target: "e1",
    text: `$${variable}`,
  })).toEqual({
    target: "e1",
    text: "browser-only-value",
  });
  expect(tools.resolveBitwardenComputerAction(active, {
    action: "type",
    text: `$${variable}`,
  })).toEqual({
    action: "type",
    text: "browser-only-value",
  });

  tools.clearBitwardenSecrets(active);
  expect(process.env[variable]).toBeUndefined();
  expect(tools.resolveBitwardenBrowserArguments(active, "browser_type", {
    target: "e1",
    text: `$${variable}`,
  })).toEqual({
    target: "e1",
    text: `$${variable}`,
  });
});
