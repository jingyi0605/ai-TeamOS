import { TaskProgressInput, RequestProgressInput } from "@outcomego/contracts/task-progress";
import { reportTaskProgress, reportRequestProgress, requestProgressSnapshot } from "@outcomego/messaging";
import { readAgentActivity, readTranscript } from "@outcomego/messaging";
import { normalizeMainToolArguments } from "@outcomego/contracts/reference-main-parsers";
import { subagentUsesParentConnectors } from "@outcomego/contracts/task-configuration";
import {
  AgentSendToUserInput,
  validateProcessSecretName,
  AUTOMATION_PARENT_ONLY_TOOLS,
  AUTOMATION_RUN_VISIBLE_TRANSCRIPT_BLOCKED,
  WakeParentInput,
  ApiError,
  CheckSubagentInput,
  CreateAgentInput,
  CreateChannelInput,
  type DynamicToolCallRequest,
  MessageSubagentInput,
  ReactToMessageInput,
  RequestBoxHelpInput,
  SendToAgentInput,
  StopSubagentInput,
  TaskInput,
  TodoWriteInput,
  UpdateAgentInput,
  UpdateChannelInput,
  isStateTool,
} from "@outcomego/contracts";
import type { PrismaClient } from "@outcomego/db";
import { type AgentMessaging, validateSendToUserInput, wakeAutomationParent, automationContextRunId } from "@outcomego/messaging";
import { Schema } from "effect";
import type { DurableStateService } from "../update-state";
import { formatMemoryToolResult } from "../memory-tool-result";
import type { AdministrationService } from "./administration-service";
import type { PluginService } from "./plugin-service";
import { serviceEffect } from "./service-utils";
import { botSubagentId, type SubagentService } from "./subagent/service";
import type { TodoService } from "./todo-service";
import type { RichMessageService } from "./rich-message-service";
import { processEnvironment, protectedPluginEnvironment } from "./process-secrets";
import { withConnectorDraftCardReminder } from "./connector-draft-reminder";
import { createHash, randomUUID } from "node:crypto";

const UUID = /^[0-9a-f]{8}(?:-[0-9a-f]{4}){3}-[0-9a-f]{12}$/i;
const bitwardenCandidate = (value: unknown): { itemId: string; title: string; hostname: string } => {
  if (!value || typeof value !== "object" || Array.isArray(value)) throw new ApiError(400, "bitwarden_candidate_invalid", "A Bitwarden candidate is invalid");
  const candidate = value as Record<string, unknown>;
  if (typeof candidate.itemId !== "string" || !UUID.test(candidate.itemId) ||
      typeof candidate.title !== "string" || !candidate.title.trim() || candidate.title.length > 200 ||
      typeof candidate.hostname !== "string" || !candidate.hostname.trim() || candidate.hostname.length > 200)
    throw new ApiError(400, "bitwarden_candidate_invalid", "A Bitwarden candidate is invalid");
  return { itemId: candidate.itemId, title: candidate.title.trim(), hostname: candidate.hostname.trim() };
};
const approvalReference = (value: unknown): string | null => {
  const text = typeof value === "string" ? value : "";
  const match = /BITWARDEN_APPROVAL:([0-9a-f-]{36})/i.exec(text);
  return match?.[1] ?? null;
};

export class InternalToolService {
  constructor(
    private readonly prisma: PrismaClient,
    private readonly messaging: AgentMessaging,
    private readonly durableState: DurableStateService,
    private readonly interruptNonUserRun: (runId: string) => Promise<void>,
    private readonly todos: TodoService,
    private readonly subagents: SubagentService,
    private readonly administration: AdministrationService,
    private readonly plugins: PluginService,
    private readonly richMessages?: RichMessageService,
    private readonly onVisibleMessage?: (runId: string, messageId: string) => Promise<void>,
    private readonly onSpeechChunk?: (runId: string, callId: string, text: string) => Promise<boolean>
  ) {}

  execute = (request: DynamicToolCallRequest, transfer: {upload?:import("@outcomego/plugin-sdk/file-spool").StagedFile;stream?:boolean} = {}) =>
    serviceEffect(async (signal) => {
      const run = await this.prisma.run.findUnique({
        where: { id: request.runId },
        include: {
          inboxEvents: {
            orderBy: { createdAt: "desc" },
            select: { payload: true },
            take: 1,
          },
        },
      });
      if (
        !run ||
        run.botId !== request.botId ||
        run.conversationId !== request.conversationId ||
        run.channelId !== request.channelId ||
        run.deliveryId !== request.deliveryId ||
        !["running"].includes(run.status)
      ) {
        throw new ApiError(409, "tool_context_invalid", "Dynamic tool context is not active");
      }
      const inboxPayload = run.inboxEvents[0]?.payload as Record<string, unknown> | undefined;
      const context = {
        runId: request.runId,
        botId: request.botId,
        conversationId: request.conversationId,
        channelId: request.channelId,
        deliveryId: request.deliveryId,
        origin: run.origin,
        callId: request.callId,
        timeZone: typeof inboxPayload?.timeZone === "string" ? inboxPayload.timeZone : undefined,
        replyToMessageId:
          typeof inboxPayload?.replyToMessageId === "string" ? inboxPayload.replyToMessageId : null,
        isFork: inboxPayload?.isFork === true,
      };
      const childIdentity = await this.prisma.subagent.findUnique({
        where: { childBotId: request.botId },
        select: { id: true, parentBotId: true, parentRunId: true, subagentType: true },
      });
      // Private supervisor operation, never registered in any model catalog.
      if (request.tool === "RefreshToolCatalog") return {namespaces:await this.plugins.dynamicNamespaces(childIdentity?.parentBotId ?? request.botId)};
      if (request.tool === "ReadProcessSecrets") return { environment: await processEnvironment(this.prisma, request.botId) };
      const bitwardenCallerAllowed = !childIdentity || childIdentity.subagentType === "computerUse";
      if (["SearchBitwardenItems", "RequestBitwardenReadApproval", "ReadBitwardenCredential", "ConsumeBitwardenReadLease", "ConsumeBitwardenSearchLease"].includes(request.tool) &&
          (!bitwardenCallerAllowed || run.origin === "routine"))
        throw new ApiError(403, "bitwarden_tool_forbidden", "Bitwarden reads are available only to the user-facing agent");
      if (request.tool === "SearchBitwardenItems") {
        const input = request.arguments && typeof request.arguments === "object" ? request.arguments as Record<string, unknown> : {};
        const query = typeof input.query === "string" ? input.query.trim() : "";
        const title = input.title === true;
        if (!query || query.length > 2048 || /[\x00-\x1f\x7f]/.test(query)) throw new ApiError(400, "bitwarden_search_invalid", "A Bitwarden search query is required");
        const leaseToken = randomUUID();
        await this.prisma.bitwardenReadApproval.create({ data: { botId: request.botId, channelId: request.channelId, sourceRunId: request.runId, title: "__search__", hostname: title ? "title" : query, status: "approved", expiresAt: new Date(Date.now() + 30_000), leaseTokenHash: createHash("sha256").update(leaseToken).digest("hex"), leaseExpiresAt: new Date(Date.now() + 30_000) } });
        return { leaseToken, title };
      }
      if (request.tool === "RequestBitwardenReadApproval") {
        const input = request.arguments && typeof request.arguments === "object" ? request.arguments as Record<string, unknown> : {};
        const rawCandidates = Array.isArray(input.candidates) ? input.candidates : [input];
        const candidates = rawCandidates.map(bitwardenCandidate);
        if (!candidates.length || candidates.length > 6) throw new ApiError(400, "bitwarden_candidates_required", "Provide one to six Bitwarden candidates");
        const approval = await this.prisma.bitwardenReadApproval.create({
          data: {
            botId: request.botId,
            channelId: request.channelId,
            sourceRunId: request.runId,
            itemId: candidates.length === 1 ? candidates[0]!.itemId : null,
            title: candidates.length === 1 ? candidates[0]!.title : "Multiple Bitwarden items",
            hostname: candidates.length === 1 ? candidates[0]!.hostname : "Multiple hostnames",
            expiresAt: new Date(Date.now() + 15 * 60_000),
          },
        });
        try {
          const sent = await this.messaging.sendVisible(context, {
            type: "widget",
            end_turn: true,
            widget: {
              prompt: candidates.length === 1
                ? `Allow reading the Bitwarden item "${candidates[0]!.title}" for ${candidates[0]!.hostname}?`
                : "Which Bitwarden item may be read for this task?",
              helpText: "This permission applies to this item for the current task and expires in 15 minutes.",
              options: candidates.map((candidate) => ({
                label: `${candidate.title} - ${candidate.hostname}`,
                value: candidate.itemId,
              })),
            },
          });
          const messageId = sent.acknowledgement && typeof sent.acknowledgement === "object" && typeof sent.acknowledgement.message_id === "string"
            ? sent.acknowledgement.message_id : null;
          if (!messageId) throw new Error("Bitwarden approval widget was not persisted");
          await this.prisma.bitwardenReadApproval.update({ where: { id: approval.id }, data: { widgetMessageId: messageId } });
          return { sent: true, waiting_for_user: true };
        } catch (error) {
          await this.prisma.bitwardenReadApproval.update({ where: { id: approval.id }, data: { status: "denied" } }).catch(() => undefined);
          throw error;
        }
      }
      if (request.tool === "ReadBitwardenCredential") {
        const input = request.arguments && typeof request.arguments === "object" ? request.arguments as Record<string, unknown> : {};
        const itemId = typeof input.itemId === "string" ? input.itemId : "";
        const field = input.field;
        if (!UUID.test(itemId) || !["username", "password", "totp"].includes(String(field)))
          throw new ApiError(400, "bitwarden_read_invalid", "A Bitwarden itemId and field are required");
        const payload = run.inboxEvents[0]?.payload as Record<string, unknown> | undefined;
        const reference = approvalReference(payload?.content);
        if (!reference) throw new ApiError(403, "bitwarden_read_not_approved", "User approval is required before reading this Bitwarden item");
        const approval = await this.prisma.bitwardenReadApproval.findUnique({ where: { id: reference } });
        if (approval?.status === "approved" && approval.expiresAt <= new Date()) {
          await this.prisma.bitwardenReadApproval.update({ where: { id: approval.id }, data: { status: "expired" } });
        }
        if (!approval || approval.botId !== request.botId || approval.channelId !== request.channelId || approval.status !== "approved" || approval.itemId?.toLowerCase() !== itemId.toLowerCase() || approval.expiresAt <= new Date())
          throw new ApiError(403, "bitwarden_read_not_approved", "User approval is missing, expired, or for a different Bitwarden item");
        const leaseToken = randomUUID();
        await this.prisma.bitwardenReadApproval.create({ data: { botId: request.botId, channelId: request.channelId, sourceRunId: request.runId, itemId, title: "__lease__", hostname: String(field), status: "approved", expiresAt: new Date(Date.now() + 30_000), leaseTokenHash: createHash("sha256").update(leaseToken).digest("hex"), leaseExpiresAt: new Date(Date.now() + 30_000) } });
        return { leaseToken };
      }
      if (request.tool === "ConsumeBitwardenReadLease") {
        const input = request.arguments && typeof request.arguments === "object" ? request.arguments as Record<string, unknown> : {};
        const leaseToken = typeof input.leaseToken === "string" ? input.leaseToken : "";
        if (!leaseToken || leaseToken.length > 100) throw new ApiError(400, "bitwarden_lease_invalid", "The Bitwarden read lease is invalid");
        const leaseHash = createHash("sha256").update(leaseToken).digest("hex");
        const environment = await this.prisma.$transaction(async (tx) => {
          const approval = await tx.bitwardenReadApproval.findFirst({ where: { leaseTokenHash: leaseHash, botId: request.botId, channelId: request.channelId, status: "approved", title: "__lease__", leaseExpiresAt: { gt: new Date() } } });
          if (!approval) throw new ApiError(403, "bitwarden_lease_invalid", "The Bitwarden read lease is invalid or expired");
          await tx.bitwardenReadApproval.delete({ where: { id: approval.id } });
          return protectedPluginEnvironment(tx);
        });
        return { environment };
      }
      if (request.tool === "ConsumeBitwardenSearchLease") {
        const input = request.arguments && typeof request.arguments === "object" ? request.arguments as Record<string, unknown> : {};
        const leaseToken = typeof input.leaseToken === "string" ? input.leaseToken : "";
        const leaseHash = createHash("sha256").update(leaseToken).digest("hex");
        const environment = await this.prisma.$transaction(async (tx) => {
          const approval = await tx.bitwardenReadApproval.findFirst({ where: { leaseTokenHash: leaseHash, botId: request.botId, channelId: request.channelId, status: "approved", itemId: null, title: "__search__", leaseExpiresAt: { gt: new Date() } } });
          if (!approval) throw new ApiError(403, "bitwarden_lease_invalid", "The Bitwarden search lease is invalid or expired");
          await tx.bitwardenReadApproval.delete({ where: { id: approval.id } });
          return protectedPluginEnvironment(tx);
        });
        return { environment };
      }
      if (run.origin === "routine" && AUTOMATION_PARENT_ONLY_TOOLS.has(request.tool)) {
        throw new ApiError(403, "automation_tool_forbidden", AUTOMATION_RUN_VISIBLE_TRANSCRIPT_BLOCKED);
      }
      request={...request,arguments:normalizeMainToolArguments(request.tool,request.arguments)};
      if (request.tool === "ReportRequestProgress") {
        if (childIdentity || run.origin === "routine") throw new ApiError(403, "request_progress_unavailable", "Only the user-facing parent may track a request");
        const result = await reportRequestProgress(this.prisma, context, Schema.decodeUnknownSync(RequestProgressInput)(request.arguments));
        return { ...(result as Record<string, unknown>), requests: await requestProgressSnapshot(this.prisma, request.botId, request.channelId) };
      }
      if (request.tool === "ReportTaskProgress") {
        if (!childIdentity) throw new ApiError(403, "task_progress_unavailable", "Only a delegated worker may report task progress");
        return reportTaskProgress(this.prisma, this.messaging, context, Schema.decodeUnknownSync(TaskProgressInput)(request.arguments));
      }
      if (request.tool === "WakeParent") {
        if (childIdentity) throw new ApiError(403, "wake_parent_unavailable", "Only the automation itself can wake its parent");
        const woken = await wakeAutomationParent(this.messaging, context, Schema.decodeUnknownSync(WakeParentInput)(request.arguments));
        // The handoff ends the automation, so the subagents it started stop with it.
        const children = await this.prisma.subagent.findMany({
          where: { parentBotId: request.botId, parentRunId: { in: woken.automation_run_ids }, status: { in: ["provisioning", "queued", "running", "waiting"] } },
          select: { id: true },
        });
        await Promise.allSettled(children.map((child) => this.subagents.stop(request.botId, request.callId, { subagent_id: botSubagentId(child.id) })));
        return woken;
      }
      if (run.origin === "routine" && request.tool !== "AcknowledgeCardOutcomes" &&
        (await this.prisma.automationResult.findUnique({ where: { runId: automationContextRunId(run.id, inboxPayload) }, select: { wakeRunId: true } }))?.wakeRunId) {
        throw new ApiError(409, "automation_turn_ended", "This automation already handed control to its parent");
      }
      if (request.tool === "PrepareConnectorTransfer" || request.tool === "ExecuteConnectorTransfer") {
        const args = request.arguments as Record<string, unknown>;
        if (childIdentity && !subagentUsesParentConnectors(childIdentity.subagentType))
          throw new ApiError(403, "parent_transfer_required", "This file transfer must be performed by the parent bot");
        return request.tool === "PrepareConnectorTransfer" ? this.plugins.fileTransfers.prepare(context, args) : this.plugins.fileTransfers.execute(context, args, signal, transfer);
      }
      if (request.tool === "AcknowledgeCardOutcomes") {
        await this.messaging.acknowledgeCardOutcomes(request.botId, (request.arguments as { receipts: Array<{ messageId: string; outcomeId: string }> }).receipts);
        return { acknowledged: true };
      }
      // Private to the computer: a finished sentence of a SendToUser message still being written, spoken at once.
      if (request.tool === "VoiceSpeak") {
        if (childIdentity || run.origin === "routine") throw new ApiError(403, "speech_unavailable", "Only the user-facing parent may speak");
        const input = (request.arguments ?? {}) as { callId?: unknown; text?: unknown };
        if (typeof input.callId !== "string" || typeof input.text !== "string" || !input.text.trim()) throw new ApiError(400, "invalid_speech", "A call id and text are required");
        return { spoken: this.onSpeechChunk ? await this.onSpeechChunk(request.runId, input.callId, input.text.slice(0, 2_000)) : false };
      }
      // Private to the computer's narrator, never registered in any model catalog.
      if (request.tool === "VoiceNarration") {
        if (childIdentity || run.origin === "routine") throw new ApiError(403, "narration_unavailable", "Only the user-facing parent may narrate");
        const text = (request.arguments as { text?: unknown } | null)?.text;
        if (typeof text !== "string") throw new ApiError(400, "invalid_narration", "Narration text is required");
        const { acknowledgement } = await this.messaging.sendNarration(context, text);
        const messageId = typeof acknowledgement === "object" ? acknowledgement.message_id : undefined;
        // The persisted chat message is authoritative; the voice session speaks it through the same path as an answer.
        if (typeof messageId === "string" && this.onVisibleMessage) await this.onVisibleMessage(request.runId, messageId).catch(() => undefined);
        return typeof acknowledgement === "object" ? acknowledgement : { sent: true };
      }
      if (request.tool === "RecordUserFormRemap" && !childIdentity && this.richMessages) {
        return this.richMessages.recordFormRemap(request.botId, request.arguments as import("@outcomego/contracts").UserFormReceipt);
      }
      const parentOnlyTools = new Set([
        "DraftExternalMessage", "CreateBotShareJson", "SendIMessage",
        "RequestUserForm",
        "RemapUserFormTargets",
        "RecallMemory",
        "ReadTranscript",
        "ReadAgentActivity",
        "ListSections",
        "Task",
        "CheckSubagent",
        "MessageSubagent",
        "StopSubagent",
        "CreateAgent",
        "UpdateAgent",
        "CreateChannel",
        "UpdateChannel",
        "SendToAgent",
        "SearchPlugins",
        "GetPlugin",
        "GetMcpServerStatus",
        "InstallPlugin",
        "UninstallPlugin",
        "AddMcpServer",
        "UninstallMcpServer",
        "AuthenticateMcpServer",
        "RestartMcpServers",
        "RenameMcpAccount",
        "RemoveMcpAccount",
        "request_box_help",
        "SetMcpInstructions",
      ]);
      if (childIdentity && parentOnlyTools.has(request.tool)) {
        throw new ApiError(403, "subagent_tool_forbidden", "This tool is parent-agent only");
      }
      if (request.tool === "ReadTranscript") return readTranscript(this.prisma, context, request.arguments);
      if (request.tool === "ReadAgentActivity") return readAgentActivity(this.prisma, { botId: childIdentity?.parentBotId ?? request.botId }, request.arguments);
      if (request.tool === "RecallMemory") {
        return this.messaging.agentData.recallMemory(request.botId, request.arguments, run.channelId);
      }
      if (request.tool === "CreateBotShareJson") {
        if (!this.richMessages) throw new Error("Review service unavailable");
        return this.richMessages.sharedTemplates.publish(context, request.arguments);
      }
      if (request.tool === "DraftExternalMessage") {
        if (!this.richMessages) throw new Error("Draft service unavailable");
        return this.richMessages.externalDrafts.create(context, request.arguments);
      }
      if (request.tool === "SendIMessage") {
        if (!this.richMessages?.messagesSends) throw new Error("Messages sending is unavailable");
        return this.richMessages.messagesSends.create(context, request.arguments);
      }
      if (request.tool === "RequestUserForm") {
        if (!this.richMessages) throw new Error("User form service is unavailable");
        return this.richMessages.createUserForm(context, request.arguments);
      }
      if (request.tool === "ListSections") {
        const sections = await this.messaging.agentData.listSections();
        return sections.length ? sections.map((section) => `- ${section.name} (id: ${section.id})`).join("\n")
          : "If there are no custom sections, CreateAgent should omit section_id.";
      }
      if (request.tool === "SearchPlugins") {
        const input =
          request.arguments && typeof request.arguments === "object"
            ? (request.arguments as Record<string, unknown>)
            : {};
        return this.plugins.searchCatalog(typeof input.query === "string" ? input.query : "");
      }
      if (request.tool === "GetPlugin") {
        const input =
          request.arguments && typeof request.arguments === "object"
            ? (request.arguments as Record<string, unknown>)
            : {};
        const pluginKey = input.plugin_id ?? input.pluginKey;
        if (typeof pluginKey !== "string") {
          throw new ApiError(400, "plugin_key_required", "pluginKey is required");
        }
        return this.plugins.catalogDetail(pluginKey);
      }
      if (request.tool === "GetMcpServerStatus") {
        const input =
          request.arguments && typeof request.arguments === "object"
            ? (request.arguments as Record<string, unknown>)
            : {};
        const resolved = await this.plugins.resolveToolArguments(request.tool, input).catch((error) => {
          // An unknown server_id is answered by the listing formatter, as in the reference.
          if (error instanceof ApiError && error.code === "connection_not_found") return null;
          throw error;
        });
        if (!resolved) return { connections: [] };
        return this.plugins.connectionStatuses(typeof resolved.connectionId === "string" ? resolved.connectionId : undefined);
      }
      if (
        [
          "InstallPlugin",
          "UninstallPlugin",
          "AddMcpServer",
          "UninstallMcpServer",
          "AuthenticateMcpServer",
          "RestartMcpServers",
          "RenameMcpAccount",
          "RemoveMcpAccount",
          "SetMcpInstructions",
        ].includes(request.tool)
      ) {
        return this.plugins.requestAction({
          runId: request.runId,
          botId: request.botId,
          callId: request.callId,
          action: request.tool,
          arguments: request.arguments,
          origin: run.origin,
        }, signal);
      }
      if (request.tool === "PluginCall") {
        const input =
          request.arguments && typeof request.arguments === "object"
            ? (request.arguments as Record<string, unknown>)
            : {};
        if (typeof input.connectionId !== "string" || typeof input.toolName !== "string") {
          throw new ApiError(400, "invalid_plugin_call", "Plugin call is missing identifiers");
        }
        const automationChild = childIdentity && (await this.prisma.run.findUnique({
          where: { id: childIdentity.parentRunId }, select: { origin: true },
        }))?.origin === "routine";
        return withConnectorDraftCardReminder(this.prisma, { connectionId: input.connectionId, toolName: input.toolName }, !childIdentity && run.origin !== "routine", await this.plugins.invoke({
          connectionId: input.connectionId,
          namespace: typeof input.namespace === "string" ? input.namespace : undefined,
          botId:
            subagentUsesParentConnectors(childIdentity?.subagentType) ? childIdentity!.parentBotId : request.botId,
          runId: request.runId,
          callId: request.callId,
          toolName: input.toolName,
          arguments: input.arguments ?? {},
        }));
      }
      if (isStateTool(request.tool)) {
        const result = await this.durableState.executeTool(
          request.botId,
          request.callId,
          request.tool,
          request.arguments,
          request.runId
        );
        return formatMemoryToolResult(result);
      }
      if (request.tool === "ReactToMessage") {
        return this.messaging.reactToMessage(
          context,
          Schema.decodeUnknownSync(ReactToMessageInput)(request.arguments)
        );
      }
      if (request.tool === "TodoWrite") {
        return this.todos.write(
          request.botId,
          request.callId,
          Schema.decodeUnknownSync(TodoWriteInput)(request.arguments)
        );
      }
      if (request.tool === "Task") {
        return this.subagents.task(context, Schema.decodeUnknownSync(TaskInput)(request.arguments));
      }
      if (request.tool === "CheckSubagent") {
        return this.subagents.check(
          request.botId,
          Schema.decodeUnknownSync(CheckSubagentInput)(request.arguments)
        );
      }
      if (request.tool === "MessageSubagent") {
        return this.subagents.message(
          request.botId,
          request.callId,
          Schema.decodeUnknownSync(MessageSubagentInput)(request.arguments)
        );
      }
      if (request.tool === "StopSubagent") {
        return this.subagents.stop(
          request.botId,
          request.callId,
          Schema.decodeUnknownSync(StopSubagentInput)(request.arguments)
        );
      }
      if (request.tool === "CreateAgent") {
        return this.administration.createAgent(
          request.botId,
          request.callId,
          Schema.decodeUnknownSync(CreateAgentInput)(request.arguments)
        );
      }
      if (request.tool === "UpdateAgent") {
        return this.administration.updateAgent(
          request.botId,
          request.callId,
          Schema.decodeUnknownSync(UpdateAgentInput)(request.arguments)
        );
      }
      if (request.tool === "CreateChannel") {
        return this.administration.createChannel(
          request.botId,
          request.callId,
          Schema.decodeUnknownSync(CreateChannelInput)(request.arguments)
        );
      }
      if (request.tool === "UpdateChannel") {
        return this.administration.updateChannel(
          request.botId,
          request.callId,
          Schema.decodeUnknownSync(UpdateChannelInput)(request.arguments)
        );
      }
      if (request.tool === "request_box_help") {
        const input = Schema.decodeUnknownSync(RequestBoxHelpInput)(request.arguments);
        const reason = input.instruction ?? (input.reason && !["auth", "captcha", "payment", "other"].includes(input.reason)
          ? input.reason : "Please complete the manual step on the computer.");
        const result = await this.messaging.sendVisible(context, {
          type: "computer-handoff",
          content: reason,
          computerHandoff: { reason, ...(input.reason ? { category: ["auth", "captcha", "payment", "other"].includes(input.reason) ? input.reason : "other" } : {}),
            ...(input.domain ? { domain: input.domain } : {}), ...(input.idp_domain ? { idpDomain: input.idp_domain } : {}) },
        });
        return {
          sent: true,
          acknowledgement: result.acknowledgement,
          waiting_for_user: true,
          instruction: "Stop this turn. You will be resumed when the user finishes or skips.",
        };
      }
      let result;
      if (request.tool === "SendToAgent") {
        result = await this.messaging.sendToAgent(
          context,
          Schema.decodeUnknownSync(SendToAgentInput)(request.arguments)
        );
      } else if (request.tool === "SendToUser") {
        validateSendToUserInput(request.arguments);
        const input = Schema.decodeUnknownSync(AgentSendToUserInput)(request.arguments);
        if (input.type === "secret-request" && input.secret?.name) {
          validateProcessSecretName(input.secret.name);
          if (input.channel || childIdentity || !await this.prisma.channel.count({ where: { id: request.channelId, kind: "bot_dm", directKey: `bot:${request.botId}`, archivedAt: null } }))
            throw new ApiError(403, "secret_owner_dm_required", "Ask for this secret only in the owner's bot DM");
        }
        if (!input.channel) {
          result = await this.messaging.sendVisible(context, input);
          const messageId = result && typeof result === "object" && "acknowledgement" in result && result.acknowledgement && typeof result.acknowledgement === "object" && "message_id" in result.acknowledgement && typeof result.acknowledgement.message_id === "string" ? result.acknowledgement.message_id : null;
          if (messageId && this.onVisibleMessage) await this.onVisibleMessage(request.runId, messageId).catch(() => undefined);
        } else {
          const content = input.type === "text" ? input.content ?? "" : input.alt ?? "";
          const sources = input.type === "text" ? input.images ?? [] : input.type === "attachment" ? [{url:input.url!,alt:input.alt}] : [];
          try {
            const files = await Promise.all(sources.map(async source => {
              const asset = await this.messaging.assets.ingestSource(source);
              return {assetId:asset.assetId,name:asset.fileName,mimeType:asset.mimeType,bytes:Buffer.from(await Bun.file(this.messaging.assets.contentPath(asset.assetId)).arrayBuffer())};
            }));
            const delivery = await this.plugins.deliverConnectedChannel({
              botId: request.botId,
              runId: request.runId,
              callId: request.callId,
              address: input.channel,
              content,
              files,
            });
            result = {
              acknowledgement: {
                sent: true,
                channel: input.channel,
                connection_id: delivery.connectionId,
                tool: delivery.toolName,
              },
              interruptRunId: null,
            };
          } catch (error) {
            if(error instanceof ApiError&&error.code==="external_file_review_required")throw error;
            // Nothing left the box for these, so refuse now with the reason.
            if (error instanceof ApiError && error.code === "connected_channel_address_invalid")
              throw new ApiError(400, error.code, `"${input.channel}" is not a valid channel address, so nothing was sent. An address is shaped platform:chat.`);
            if (error instanceof ApiError && ["connected_channel_unavailable", "connected_channel_ambiguous", "connected_channel_delivery_unsupported"].includes(error.code))
              throw new ApiError(error.status, error.code, `${error.message}. Nothing was sent.`);
            const recoveryRunId = await this.messaging.enqueueChannelDeliveryFailure(
              context,
              input.channel,
              error
            );
            result = {
              acknowledgement: {
                sent: false,
                channel: input.channel,
                delivery_failed: true,
                reason: (error instanceof Error ? error.message : String(error)).slice(0, 2_000),
                recovery_run_id: recoveryRunId,
              },
              interruptRunId: null,
            };
          }
        }
      } else {
        throw new ApiError(400, "unknown_dynamic_tool", `Unknown tool ${request.tool}`);
      }
      if (result.interruptRunId) await this.interruptNonUserRun(result.interruptRunId);
      return result.acknowledgement;
    });
}
