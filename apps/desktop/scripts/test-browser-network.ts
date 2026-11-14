import { createServer } from "vite";
import { chromium } from "../../computer/node_modules/playwright-core";
import { mkdir } from "node:fs/promises";
import { resolve } from "node:path";
const root = resolve(import.meta.dirname, "..");
const vite = await createServer({ root, server: { host: "127.0.0.1", port: 0 } });
await vite.listen();
const browser = await chromium.launch({
  executablePath:
    process.env.CHROME_PATH ?? "/Applications/Google Chrome.app/Contents/MacOS/Google Chrome",
  headless: true,
});
const output = resolve(root, "../../output/browser-network-compact");
await mkdir(output, { recursive: true });
const context = await browser.newContext({ viewport: { width: 760, height: 820 } });
let checks = 0;
const assert = (value: unknown, message: string) => {
  if (!value) throw Error(message);
  checks++;
};
try {
  const page = await context.newPage();
  page.setDefaultTimeout(8000);
  const errors: string[] = [];
  const dialogs: string[] = [];
  page.on("pageerror", (error) => errors.push(error.message));
  page.on("dialog", (dialog) => {
    dialogs.push(dialog.message());
    void dialog.accept();
  });
  await page.addInitScript(() => {
    (window as any).networkFixture = {
      view: {
        settings: {
          deploymentId: "test-deployment",
          mode: "ranked",
          machineId: null,
          revision: 1,
          sourceOrder: ["office", "server"],
        },
        runtime: {
          state: "ready",
          appliedRevision: 1,
          activeMachineId: "office",
          activeConnections: 0,
          bytesSent: 0,
          bytesReceived: 0,
          blockedConnections: 0,
        },
        devices: [
          {
            machineId: "home",
            label: "MacBook Air",
            enabled: true,
            connected: true,
            sharingAllowed: false,
            supported: true,
          },
          {
            machineId: "office",
            label: "Mac mini",
            enabled: true,
            connected: true,
            sharingAllowed: true,
            supported: true,
          },
        ],
      },
      sharing: {
        supported: true,
        connected: true,
        sharingAllowed: false,
        providing: false,
        serverUrl: "https://outcomego.example",
        deploymentId: "test-deployment",
        activeConnections: 0,
      },
      mutations: [],
      permissionChanges: [],
    };
  });
  await page.goto(`${vite.resolvedUrls!.local[0]}test/browser/browser-network.html`);
  const sources = () =>
    page
      .locator("[data-network-source]")
      .evaluateAll((rows) => rows.map((row) => row.getAttribute("data-network-source")));
  const status = (text: string) => page.getByRole("status").filter({ hasText: text }).waitFor();
  await page.getByRole("list", { name: "Network exits" }).waitFor();
  assert((await page.getByLabel("Browser network source").count()) === 0, "always ranked");
  assert(
    JSON.stringify(await sources()) === JSON.stringify(["office", "server"]),
    "server shares priority pool"
  );
  await page.getByRole("button", { name: "Reveal exit IP", exact: true }).waitFor();
  assert(
    (await page.getByRole("button", { name: "Reveal exit IP" }).getAttribute("aria-pressed")) ===
      "false",
    "IP hidden by default"
  );
  assert(
    (await page.getByText("198.51.100.20", { exact: true }).getAttribute("aria-hidden")) === "true",
    "hidden IP excluded from accessibility tree"
  );
  await page.getByRole("button", { name: "Reveal exit IP" }).click();
  assert(
    (await page.getByText("198.51.100.20", { exact: true }).getAttribute("aria-hidden")) ===
      "false",
    "IP reveals on click"
  );
  await page.getByRole("button", { name: "Hide exit IP" }).press("Enter");
  assert(
    await page.getByRole("button", { name: "Reveal exit IP" }).isVisible(),
    "keyboard hides IP"
  );
  await page.getByRole("button", { name: "Reveal exit location" }).click();
  assert(
    (await page.getByText("Boston, United States").getAttribute("aria-hidden")) === "false",
    "location reveals independently"
  );
  assert(
    await page
      .locator('[data-network-source="office"]')
      .getByLabel("Working · In use", { exact: true })
      .isVisible(),
    "active working device uses a text badge"
  );
  assert(
    (await page.evaluate(() => (window as any).networkFixture.qualityChecks ?? 0)) === 0,
    "speed test never runs automatically"
  );
  await page.getByRole("button", { name: "Test active connection" }).click();
  await page.getByLabel("Latency 42 milliseconds", { exact: true }).waitFor();
  assert(
    await page.getByLabel("Estimated download 28.4 megabits per second").isVisible(),
    "manual exit test shows download estimate"
  );
  assert(
    await page.getByLabel("Estimated upload 9.2 megabits per second").isVisible(),
    "manual exit test shows upload estimate"
  );
  await page.getByRole("checkbox", { name: "Route browser traffic through this computer" }).check();
  await page.locator('[data-network-source="home"]').waitFor();
  assert(
    await page
      .locator('[data-network-source="home"]')
      .getByText("(current)", { exact: true })
      .isVisible(),
    "enrolled local computer is identified independently of active route"
  );
  assert(
    JSON.stringify(await sources()) === JSON.stringify(["office", "home", "server"]),
    "opt-in automatically adds device"
  );
  assert(
    (await page.evaluate(() => (window as any).networkFixture.mutations.length)) === 0,
    "no extra Apply for opt-in"
  );
  await page.getByRole("button", { name: "Use MacBook Air", exact: true }).click();
  await status("Using MacBook Air");
  assert((await sources())[0] === "home", "selecting a source moves it first");
  assert(
    (await page.evaluate(() => (window as any).networkFixture.view.runtime.activeMachineId)) ===
      "home",
    "selecting a source changes the active connection immediately"
  );
  assert(
    (await page.getByLabel("Latency 42 milliseconds", { exact: true }).count()) === 0,
    "route change clears old speed results"
  );
  await page.getByText("203.0.113.10", { exact: true }).waitFor();
  assert(
    await page.getByRole("button", { name: "Reveal exit location" }).isVisible(),
    "route switch resets location privacy"
  );
  assert(
    (await page.getByText("203.0.113.10", { exact: true }).getAttribute("aria-hidden")) === "true",
    "new route IP hidden"
  );
  await page.getByRole("checkbox").uncheck();
  await status("Using Mac mini");
  assert((await sources())[0] === "home", "stopping sharing retains rank");
  assert(
    await page
      .locator('[data-network-source="home"]')
      .getByText("Not working", { exact: true })
      .isVisible(),
    "stopped device status changes"
  );
  await page.getByRole("button", { name: "Use Server connection", exact: true }).click();
  await status("Using the server connection");
  assert(
    JSON.stringify(await sources()) === JSON.stringify(["server", "home", "office"]),
    "selecting the server moves it first"
  );
  assert(
    (await page.evaluate(() => (window as any).networkFixture.view.runtime.activeMachineId)) ===
      null,
    "selecting the server immediately activates the server route"
  );
  await page.getByRole("checkbox").check();
  await status("Using the server connection");
  assert(
    (await sources()).filter((id) => id === "home").length === 1,
    "reenabling never duplicates"
  );
  assert((await sources())[0] === "server", "reenabling respects server first");
  await page.evaluate(() => {
    const f = (window as any).networkFixture;
    f.view.settings.sourceOrder = ["server", "home", "office"];
    f.view.settings.revision++;
    (window as any).refreshNetworkFixture();
  });
  await page.getByRole("button", { name: "Use Mac mini", exact: true }).click();
  await page.getByRole("alert").filter({ hasText: "connection changed" }).waitFor();
  assert((await sources())[0] === "server", "stale draft refresh preserves concurrent edit");
  assert(
    dialogs.every((message) => !message.includes("Save this connection priority")),
    "priority changes have no confirmation dialog"
  );
  await page.evaluate(() => {
    const f = (window as any).networkFixture;
    f.failProbe = true;
    f.view.settings.revision++;
  });
  await page.getByText("Unavailable", { exact: true }).waitFor();
  assert(
    (await page.getByRole("button", { name: /Reveal exit/ }).count()) === 0,
    "failed probe never displays stale IP"
  );
  await page.evaluate(() => {
    (window as any).networkFixture.failQuality = true;
  });
  await page.getByRole("button", { name: "Test active connection" }).click();
  await page.getByText("Test unavailable", { exact: true }).waitFor();
  assert(
    (await page.getByLabel("Latency 42 milliseconds", { exact: true }).count()) === 0,
    "failed speed test clears old results"
  );
  await page.setViewportSize({ width: 360, height: 850 });
  assert(
    await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth),
    "fits narrow widths"
  );
  await page.screenshot({ path: resolve(output, "compact-component.png"), fullPage: true });
  assert(errors.length === 0, errors.join("\n"));
  console.log(JSON.stringify({ checks, output }));
} finally {
  await context.close();
  await browser.close();
  await vite.close();
}
