import { expect, test } from "bun:test";
import { Effect } from "effect";
import { InternalToolService } from "../../src/services/internal-tool-service";

const request = (overrides: Record<string, unknown> = {}) => ({
  runId: "run-1",
  botId: "bot-1",
  conversationId: "conversation-1",
  channelId: "channel-1",
  deliveryId: null,
  callId: "read-1",
  tool: "ReadBitwardenCredential",
  arguments: { itemId: "00000000-0000-0000-0000-000000000001", field: "password" },
  ...overrides,
});

const service = (child: Record<string, unknown> | null = null) => new InternalToolService(
  {
    run: { findUnique: async () => ({ id: "run-1", botId: "bot-1", conversationId: "conversation-1", channelId: "channel-1", deliveryId: null, status: "running", origin: "user", inboxEvents: [] }) },
    subagent: { findUnique: async () => child },
  } as never,
  {} as never,
  {} as never,
  async () => {},
  {} as never,
  {} as never,
  {} as never,
  {} as never,
);

test("rejects a Bitwarden field read without a server-side approval reference", async () => {
  await expect(Effect.runPromise(service().execute(request()))).rejects.toThrow("approval is required");
});

test("does not expose Bitwarden read tools to executor subagents", async () => {
  await expect(Effect.runPromise(service({ id: "child", subagentType: "executor" }).execute(request()))).rejects.toThrow("available only to the user-facing agent");
});
