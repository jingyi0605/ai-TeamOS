import { Agent, request } from "node:https";
import { connect } from "node:tls";
import { isIP } from "node:net";
import type { Duplex } from "node:stream";
import type {
  NetworkEndpointCheck,
  NetworkEgress,
  NetworkRoute,
} from "@outcomego/plugin-sdk/browser-network/protocol";

const HOST = "ipwho.is";
const SECONDARY_HOSTS = ["example.com", "cloudflare.com"];
const FAILURE = "Could not check browser exit connection";
export function parseEgress(body: string): Pick<NetworkEgress, "ip" | "location"> {
  const value = JSON.parse(body);
  if (value.success !== true || typeof value.ip !== "string" || !isIP(value.ip))
    throw new Error(FAILURE);
  const parts = [value.city, value.country].filter(
    (v): v is string =>
      typeof v === "string" && v.length > 0 && v.length <= 100 && !/[\x00-\x1f\x7f]/.test(v)
  );
  return { ip: value.ip, location: parts.join(", ") || "Unknown" };
}

/** Uses exactly the same leased route and device-side DNS as managed browsers. */
export function createEgressProbe(client: {
  route: NetworkRoute | null;
  available(): boolean;
  open(host: string, port: number): Promise<Duplex>;
}) {
  let cached: { epoch: string; until: number; result?: NetworkEgress } | undefined;
  let pending: { epoch: string; result: Promise<NetworkEgress> } | undefined;
  return (): Promise<NetworkEgress> => {
    const route = client.route;
    if (!route || !client.available()) return Promise.reject(new Error(FAILURE));
    if (cached?.epoch === route.epoch && performance.now() < cached.until)
      return cached.result ? Promise.resolve(cached.result) : Promise.reject(new Error(FAILURE));
    if (pending)
      return pending.epoch === route.epoch ? pending.result : Promise.reject(new Error(FAILURE));
    const epoch = route.epoch;
    const result = (async () => {
      let raw: Duplex | undefined;
      let agent: Agent | undefined;
      let expired = false;
      let timer: ReturnType<typeof setTimeout> | undefined;
      try {
        const lookup = (async () => {
          raw = await client.open(HOST, 443);
          if (expired) {
            raw.destroy();
            throw new Error(FAILURE);
          }
          agent = new Agent({ keepAlive: false, maxSockets: 1 });
          agent.createConnection = () => connect({ socket: raw!, servername: HOST });
          const body = await new Promise<string>((resolve, reject) => {
            const req = request(
              {
                hostname: HOST,
                port: 443,
                path: "/?fields=success,ip,city,country",
                method: "GET",
                agent,
                headers: { accept: "application/json", "user-agent": "OutcomeGo/egress-check" },
              },
              (response) => {
                if (
                  response.statusCode !== 200 ||
                  !response.headers["content-type"]?.includes("application/json")
                ) {
                  response.destroy();
                  reject(new Error(FAILURE));
                  return;
                }
                let bytes = 0;
                const chunks: Buffer[] = [];
                response.on("data", (chunk: Buffer) => {
                  bytes += chunk.length;
                  if (bytes > 8192) {
                    response.destroy(new Error(FAILURE));
                    return;
                  }
                  chunks.push(chunk);
                });
                response.on("end", () => resolve(Buffer.concat(chunks).toString("utf8")));
                response.on("error", reject);
                response.on("aborted", () => reject(new Error(FAILURE)));
              }
            );
            req.on("error", reject);
            req.end();
          });
          if (!client.available() || client.route?.epoch !== epoch) throw new Error(FAILURE);
          const checks = await Promise.all(
            SECONDARY_HOSTS.map((host) => checkEndpoint(client, host))
          );
          if (!checks.some((check) => check.ok)) throw new Error(FAILURE);
          return {
            ...parseEgress(body),
            epoch,
            sourceId: route.mode === "server" ? "server" : route.machineId!,
            checkedAt: new Date().toISOString(),
            checks,
          };
        })();
        const value = await Promise.race([
          lookup,
          new Promise<never>((_, reject) => {
            timer = setTimeout(() => {
              expired = true;
              raw?.destroy();
              agent?.destroy();
              reject(new Error(FAILURE));
            }, 8000);
          }),
        ]);
        cached = { epoch, until: performance.now() + 120_000, result: value };
        return value;
      } catch {
        cached = { epoch, until: performance.now() + 120_000 };
        throw new Error(FAILURE);
      } finally {
        expired = true;
        clearTimeout(timer);
        agent?.destroy();
        raw?.destroy();
        pending = undefined;
      }
    })();
    pending = { epoch, result };
    return result;
  };
}

async function checkEndpoint(
  client: {
    open(host: string, port: number): Promise<Duplex>;
  },
  host: string
): Promise<NetworkEndpointCheck> {
  let raw: Duplex | undefined;
  let socket: ReturnType<typeof connect> | undefined;
  try {
    raw = await client.open(host, 443);
    socket = connect({ socket: raw, servername: host, ALPNProtocols: ["http/1.1"] });
    const status = await new Promise<number>((resolve, reject) => {
      let data = "";
      const timer = setTimeout(() => reject(new Error("timeout")), 5000);
      const finish = (error?: Error, code?: number) => {
        clearTimeout(timer);
        error ? reject(error) : resolve(code!);
      };
      socket!.once("secureConnect", () => {
        socket!.write(
          `HEAD / HTTP/1.1\r\nHost: ${host}\r\nConnection: close\r\nUser-Agent: OutcomeGo/egress-check\r\n\r\n`
        );
      });
      socket!.on("data", (chunk: Buffer) => {
        data += chunk.toString("latin1");
        const line = data.match(/^HTTP\/\d(?:\.\d)?\s+(\d{3})\b/);
        if (line) finish(undefined, Number(line[1]));
      });
      socket!.once("error", (error: Error) => finish(error));
      socket!.once("close", () => {
        if (!data.match(/^HTTP\/\d(?:\.\d)?\s+\d{3}\b/)) finish(new Error("no_http_response"));
      });
    });
    return { host, ok: status >= 200 && status < 400, status };
  } catch {
    return { host, ok: false, error: "unavailable" };
  } finally {
    socket?.destroy();
    if (!socket) raw?.destroy();
  }
}
