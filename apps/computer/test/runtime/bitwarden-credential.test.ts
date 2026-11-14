import { expect, test } from "bun:test";
import { chmod, mkdir, mkdtemp, rm, writeFile } from "node:fs/promises";
import { tmpdir } from "node:os";
import { join } from "node:path";
import { ComputerRuntime } from "../../src/runtime";

const turn = (overrides: Record<string, unknown> = {}): any => ({
  runId: "run-1",
  botId: "bot-1",
  contextSessionId: "context-1",
  screenBotId: "screen-1",
  conversationId: "conversation-1",
  channelId: "channel-1",
  deliveryId: null,
  cwd: "/tmp",
  runtimeProfile: "agent",
  subagentType: null,
  requestSource: "turn",
  readOnly: false,
  pluginNamespaces: [],
  ...overrides,
});

test("Bitwarden reads keep the credential out of the tool result and return only a browser reference", async () => {
  const root = await mkdtemp(join(tmpdir(), "outcomego-bitwarden-runtime-"));
  const previousRoot = process.env.OUTCOMEGO_AGENT_DATA_ROOT;
  const previousFetch = globalThis.fetch;
  const secret = "credential-never-enters-context";
  const installPath = join(root, "plugins", "cache", "outcomego", "bitwarden", "test-revision");
  const cachedInstallPath = "/Users/jackson/.openteam-dev/mnt/agent-data/plugins/cache/outcomego/bitwarden/test-revision";
  const wrapper = join(installPath, "skills", "bitwarden", "scripts", "bw.sh");
  await mkdir(join(root, "plugin-skills"), { recursive: true });
  await mkdir(join(installPath, "skills", "bitwarden", "scripts"), { recursive: true });
  await writeFile(join(root, "plugin-skills", "cache.json"), JSON.stringify({
    packages: [{ pluginId: "bitwarden", installPath: cachedInstallPath }],
  }));
  await writeFile(wrapper, `#!/bin/sh\nprintf '%s\\n' '{"ok":true,"value":"${secret}"}'\n`);
  await chmod(wrapper, 0o755);
  process.env.OUTCOMEGO_AGENT_DATA_ROOT = root;
  globalThis.fetch = (async (_url: string, init: RequestInit) => {
    const request = JSON.parse(String(init.body)) as { tool: string };
    if (request.tool === "ReadBitwardenCredential") return Response.json({ leaseToken: "read-lease" });
    if (request.tool === "ConsumeBitwardenReadLease") {
      return Response.json({
        environment: {
          BW_SERVER_URL: "https://vault.example.test",
          BW_EMAIL: "bot@example.test",
          BW_CLIENT_ID: "client-id",
          BW_CLIENT_SECRET: "client-secret",
          BW_MASTER_PASSWORD: "master-password",
        },
      });
    }
    throw new Error(`Unexpected private tool ${request.tool}`);
  }) as typeof fetch;

  try {
    const runtime = new ComputerRuntime();
    const tools = (runtime as any).tools;
    const result = await tools.executeOutcomeGoTool(
      turn(),
      "call-1",
      "CallDynamicTool",
      {
        namespace: "cursor",
        toolName: "ReadBitwardenCredential",
        arguments: {
          itemId: "00000000-0000-0000-0000-000000000001",
          field: "password",
        },
      },
    );
    const text = result.content[0].text as string;
    expect(text).toContain('"stored":true');
    expect(text).toContain('"browserReference":"$OUTCOMEGO_BITWARDEN_PASSWORD_');
    expect(text).not.toContain(secret);
    expect(result.details).not.toHaveProperty("value");
    const variable = result.details.environmentVariable as string;
    expect(process.env[variable]).toBe(secret);
    tools.clearBitwardenSecrets(turn());
    expect(process.env[variable]).toBeUndefined();
  } finally {
    globalThis.fetch = previousFetch;
    if (previousRoot === undefined) delete process.env.OUTCOMEGO_AGENT_DATA_ROOT;
    else process.env.OUTCOMEGO_AGENT_DATA_ROOT = previousRoot;
    await rm(root, { recursive: true, force: true });
  }
});
