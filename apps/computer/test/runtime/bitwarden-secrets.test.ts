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

test("browser arguments resolve the reference across a delegated computerUse scope", () => {
  const runtime = new ComputerRuntime();
  const tools = (runtime as any).tools;
  const parent = { botId: "bot-1", runId: "run-1", credentialScopeId: "run-1", screenBotId: "screen-1" };
  const child = { botId: "child-1", runId: "run-2", credentialScopeId: "run-1", screenBotId: "screen-1" };
  const variable = tools.bitwardenSecrets.put(tools.bitwardenScope(parent), "password", "browser-only-value");

  expect(tools.resolveBitwardenBrowserArguments(child, "browser_type", {
    target: "e1",
    text: `$${variable}`,
  })).toEqual({
    target: "e1",
    text: "browser-only-value",
    __outcomeGoSecretFill: true,
  });
  expect(tools.resolveBitwardenComputerAction(child, {
    action: "type",
    text: `$${variable}`,
  })).toEqual({
    action: "type",
    text: "browser-only-value",
  });

  tools.clearBitwardenSecrets(parent);
  expect(process.env[variable]).toBe("browser-only-value");
  expect(tools.resolveBitwardenBrowserArguments(child, "browser_type", {
    target: "e1",
    text: `$${variable}`,
  })).toMatchObject({ text: "browser-only-value" });

  tools.clearBitwardenSecrets(child);
  expect(process.env[variable]).toBeUndefined();
});

test("unavailable Bitwarden references fail closed instead of reaching the browser", () => {
  const runtime = new ComputerRuntime();
  const tools = (runtime as any).tools;
  const active = { botId: "bot-1", runId: "run-1", credentialScopeId: "run-1", screenBotId: "screen-1" };

  expect(() => tools.resolveBitwardenBrowserArguments(active, "browser_type", {
    target: "e1",
    text: "$OUTCOMEGO_BITWARDEN_PASSWORD_expired",
  })).toThrow("unavailable or expired");
});
