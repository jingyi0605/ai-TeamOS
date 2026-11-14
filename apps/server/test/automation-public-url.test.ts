import { expect, test } from "bun:test";
import { AutomationWebhooksService } from "../src/services/automation-webhooks";

test("automation webhook service reads the current OutcomeGo public URL environment setting", () => {
  const previousOutcomeGo = process.env.OUTCOMEGO_PUBLIC_URL;
  const previousOpenTeam = process.env.OPENTEAM_PUBLIC_URL;
  process.env.OUTCOMEGO_PUBLIC_URL = "https://configured.example.test";
  delete process.env.OPENTEAM_PUBLIC_URL;
  try {
    const service = new AutomationWebhooksService(
      {
        automationWebhookSettings: { findUnique: async () => null },
        automationWebhook: { findMany: async () => [] },
      } as unknown as ConstructorParameters<typeof AutomationWebhooksService>[0],
      async () => {}
    );
    expect(service.publicUrlView()).toMatchObject({
      publicUrl: "https://configured.example.test",
      source: "environment",
      valid: true,
    });
  } finally {
    if (previousOutcomeGo === undefined) delete process.env.OUTCOMEGO_PUBLIC_URL;
    else process.env.OUTCOMEGO_PUBLIC_URL = previousOutcomeGo;
    if (previousOpenTeam === undefined) delete process.env.OPENTEAM_PUBLIC_URL;
    else process.env.OPENTEAM_PUBLIC_URL = previousOpenTeam;
  }
});

test("automation webhook public URL settings override the environment and apply immediately", async () => {
  let saved: string | null = null;
  const checked: string[] = [];
  const db: any = {
    automationWebhookSettings: {
      findUnique: async () => (saved ? { publicUrl: saved } : null),
      upsert: async ({ create }: any) => {
        saved = create.publicUrl;
      },
      deleteMany: async () => {
        saved = null;
      },
    },
    automationWebhook: {
      findMany: async () => [],
    },
  };
  const service = new AutomationWebhooksService(
    db,
    async () => {},
    (async (input: RequestInfo | URL) => {
      checked.push(String(input));
      return Response.json({ status: "ok" });
    }) as typeof fetch,
    "https://env.example.test"
  );

  await service.loadPublicUrl();
  expect(service.publicUrlView()).toMatchObject({
    publicUrl: "https://env.example.test",
    source: "environment",
    valid: true,
  });

  const savedView = await service.updatePublicUrl({ publicUrl: "https://settings.example.test/" });
  expect(savedView).toMatchObject({
    publicUrl: "https://settings.example.test",
    source: "settings",
    valid: true,
    updatedWebhooks: 0,
  });

  const check = await service.testPublicUrl({ publicUrl: "https://settings.example.test" });
  expect(check.ok).toBe(true);
  expect(checked).toEqual(["https://settings.example.test/api/v0/health"]);

  await service.updatePublicUrl({ publicUrl: "" });
  expect(service.publicUrlView()).toMatchObject({
    publicUrl: "https://env.example.test",
    source: "environment",
    valid: true,
  });
});

test("automation webhook public URL connectivity reports an HTTP failure", async () => {
  const db: any = {
    automationWebhookSettings: { findUnique: async () => null },
    automationWebhook: { findMany: async () => [] },
  };
  const service = new AutomationWebhooksService(
    db,
    async () => {},
    (async () => new Response("bad gateway", { status: 502 })) as unknown as typeof fetch
  );
  const result = await service.testPublicUrl({ publicUrl: "https://unavailable.example.test" });
  expect(result).toMatchObject({
    ok: false,
    status: 502,
    message: "Public webhook address responded with HTTP 502.",
  });
});
