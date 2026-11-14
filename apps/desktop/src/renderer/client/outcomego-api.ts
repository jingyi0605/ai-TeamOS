import { createOutcomeGoClient } from "@outcomego/client-core";
import { authHeaders, getDesktopMachineId } from "./auth";
import { API_BASE, desktopTransportOptions } from "./http";
import { request } from "./http";
import type {
  VoiceSessionCreateInput,
  VoiceSessionTicketView,
  VoiceSessionView,
  VoiceSettingsInput,
  VoiceSettingsView,
} from "@outcomego/contracts/voice";
import type {
  AutomationWebhookPublicUrlCheck,
  AutomationWebhookPublicUrlView,
} from "@outcomego/contracts/automation-webhooks";

const localTimeZone = () => Intl.DateTimeFormat().resolvedOptions().timeZone || "UTC";

/** Portable product API plus the desktop lifecycle adapter below. */
export const outcomeGoClient = createOutcomeGoClient({
  ...desktopTransportOptions,
  createId: () => crypto.randomUUID(),
  timeZone: localTimeZone,
  sourceMachineId: getDesktopMachineId,
});

export type { ChannelClientState, ClientBootstrapView } from "@outcomego/contracts";

export const api = {
  ...outcomeGoClient,
  authenticatePlugin: (connectionId: string, force = false) =>
    window.outcomego?.pluginOAuth
      ? window.outcomego.pluginOAuth.start(connectionId, force)
      : outcomeGoClient.authenticatePlugin(connectionId, force),
  cancelPluginAuthentication: async (connectionId: string, state: string) => {
    if (await window.outcomego?.pluginOAuth?.cancel(connectionId, state)) return { cancelled: true };
    // A server callback, or an attempt owned by a different desktop, can still be cancelled.
    const settings = await outcomeGoClient.pluginConnectionStatuses([connectionId]);
    const authorizationUrl = settings.connections[0]?.authorizationUrl;
    const redirect = authorizationUrl ? new URL(authorizationUrl).searchParams.get("redirect_uri") : null;
    const local = redirect?.startsWith("http://127.0.0.1:") && new URL(redirect).pathname === "/callback";
    return outcomeGoClient.cancelPluginAuthentication(connectionId, state, local ? redirect! : undefined);
  },
  sendMessage: outcomeGoClient.sendDirectMessage,
  voice: {
    createSession: (input: VoiceSessionCreateInput) => request<VoiceSessionTicketView>("/api/voice/sessions", { method: "POST", body: JSON.stringify(input) }),
    getSession: (sessionId: string) => request<{ session: VoiceSessionView; turns: unknown[] }>(`/api/voice/sessions/${encodeURIComponent(sessionId)}`),
    reconnectTicket: (sessionId: string) => request<VoiceSessionTicketView>(`/api/voice/sessions/${encodeURIComponent(sessionId)}/tickets`, { method: "POST" }),
    endSession: (sessionId: string) => request<{ ok: true }>(`/api/voice/sessions/${encodeURIComponent(sessionId)}`, { method: "DELETE" }),
    settings: () => request<VoiceSettingsView>("/api/server-settings/voice"),
    updateSettings: (input: VoiceSettingsInput) =>
      request<VoiceSettingsView>("/api/server-settings/voice", {
        method: "PUT",
        body: JSON.stringify(input),
      }),
  },
  automationWebhookPublicUrl: () =>
    request<AutomationWebhookPublicUrlView>(
      "/api/server-settings/automation-webhook-public-url"
    ),
  updateAutomationWebhookPublicUrl: (publicUrl: string) =>
    request<AutomationWebhookPublicUrlView & { updatedWebhooks: number }>(
      "/api/server-settings/automation-webhook-public-url",
      { method: "PATCH", body: JSON.stringify({ publicUrl }) }
    ),
  checkAutomationWebhookPublicUrl: (publicUrl: string) =>
    request<AutomationWebhookPublicUrlCheck>(
      "/api/server-settings/automation-webhook-public-url/check",
      { method: "POST", body: JSON.stringify({ publicUrl }) }
    ),
  routineWebhook: (routineId: string) =>
    outcomeGoClient.routineWebhook(routineId),
  screenTakeover: outcomeGoClient.setScreenTakeover,
  /** Best-effort unload path; keepalive is a browser lifecycle concern. */
  releaseScreenTakeover: (botId: string) => {
    void fetch(`${API_BASE}/api/v0/bots/${encodeURIComponent(botId)}/screen/takeover`, {
      method: "POST",
      headers: { "content-type": "application/json", ...authHeaders() },
      body: JSON.stringify({ active: false }),
      keepalive: true,
    }).catch(() => undefined);
  },
  /** Best-effort navigation/app-close path for an active secure handoff. */
  releaseComputerHandoff: (messageId: string) => {
    void fetch(
      `${API_BASE}/api/v0/channel-messages/${encodeURIComponent(messageId)}/computer-handoff`,
      {
        method: "POST",
        headers: { "content-type": "application/json", ...authHeaders() },
        body: JSON.stringify({ action: "dismiss", clientId: crypto.randomUUID() }),
        keepalive: true,
      }
    ).catch(() => undefined);
  },
};
