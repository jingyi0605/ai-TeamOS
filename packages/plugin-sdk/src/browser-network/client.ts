import WebSocket from "ws";
import { randomBytes } from "node:crypto";
import { NetworkMux, type NetworkStream } from "./mux";
import {
  NETWORK_PROTOCOL,
  LEASE_MS,
  RENEW_MS,
  MAX_STREAMS,
  MAX_WINDOW,
  WINDOW,
  validWindow,
  RECONNECT_MAX_MS,
  type NetworkRoute,
  type OpenTarget,
  type NetworkDnsDiagnostic,
} from "./protocol";
import {
  connectTargetAddress,
  dialTarget,
  NetworkError,
  resolveTargetAddresses,
  type TargetAddress,
} from "./policy";
import type { Duplex } from "node:stream";
import { Duplex as NodeDuplex } from "node:stream";
import type { Socket } from "node:net";

const looksLikeTlsClientHello = (bytes: Buffer) =>
  bytes.length >= 6 &&
  bytes[0] === 22 &&
  bytes[1] === 3 &&
  bytes[2]! >= 1 &&
  bytes[2]! <= 4 &&
  bytes[5] === 1;

const looksLikeTlsServerResponse = (bytes: Buffer) =>
  bytes.length >= 6 &&
  bytes[0] === 22 &&
  bytes[1] === 3 &&
  bytes[2]! >= 1 &&
  bytes[2]! <= 4 &&
  bytes[5] !== 0;

/**
 * Keeps the browser's initial TLS bytes and retries another resolved address
 * when the first connected edge closes before returning a TLS record.
 */
export class EarlyTlsRetrySocket extends NodeDuplex {
  private socket?: Socket;
  private index = 0;
  private outbound = Buffer.alloc(0);
  private inbound = Buffer.alloc(0);
  private tls = false;
  private validated = false;
  private retrying = false;
  private stopped = false;
  private finished = false;
  private resolveReady!: () => void;
  private rejectReady!: (error: Error) => void;
  readonly ready: Promise<void>;
  constructor(
    private readonly addresses: TargetAddress[],
    private readonly signal: AbortSignal,
    private readonly connect: (address: TargetAddress) => Promise<Socket>,
    private readonly selected: (address: TargetAddress) => void
  ) {
    super({ allowHalfOpen: true });
    this.ready = new Promise((resolve, reject) => {
      this.resolveReady = resolve;
      this.rejectReady = reject;
    });
    void this.ready.catch(() => {});
    this.on("error", () => {});
    signal.addEventListener("abort", () => this.destroy(new NetworkError("request_cancelled")), {
      once: true,
    });
    void this.connectNext();
  }
  private attach(socket: Socket) {
    this.socket = socket;
    socket.on("data", (chunk) => {
      const bytes = Buffer.from(chunk);
      if (!this.validated) {
        if (!this.tls) this.validated = true;
        else {
          this.inbound = Buffer.concat([this.inbound, bytes]).subarray(0, 6);
          if (looksLikeTlsServerResponse(this.inbound)) this.validated = true;
        }
      }
      this.push(bytes);
    });
    socket.once("end", () => {
      if (this.socket !== socket) return;
      if (!this.tryRetry()) this.push(null);
    });
    socket.once("close", () => {
      if (this.socket !== socket) return;
      if (!this.tryRetry() && !this.destroyed) this.push(null);
    });
    socket.on("error", () => {
      if (this.socket !== socket) return;
      if (!this.tryRetry()) this.destroy(new NetworkError("connect_failed"));
    });
    if (this.outbound.length) {
      socket.write(this.outbound, () => {
        if (this.finished && this.socket === socket && !socket.destroyed) socket.end();
      });
    } else if (this.finished) {
      socket.end();
    }
  }
  private tryRetry() {
    if (this.stopped || this.validated || !this.tls || this.index >= this.addresses.length)
      return false;
    if (this.retrying) return true;
    this.retrying = true;
    this.socket?.destroy();
    this.socket = undefined;
    void this.connectNext().finally(() => {
      this.retrying = false;
    });
    return true;
  }
  private async connectNext() {
    if (this.stopped || this.index >= this.addresses.length) {
      const error = new NetworkError("connect_failed");
      this.rejectReady(error);
      if (!this.destroyed) this.destroy(error);
      return;
    }
    const address = this.addresses[this.index++]!;
    try {
      const socket = await this.connect(address);
      if (this.stopped || this.destroyed) {
        socket.destroy();
        return;
      }
      this.selected(address);
      this.resolveReady();
      this.attach(socket);
    } catch {
      await this.connectNext();
    }
  }
  override _read() {}
  override _write(chunk: Buffer, _encoding: BufferEncoding, done: (error?: Error | null) => void) {
    if (this.destroyed || this.stopped) {
      done(new NetworkError("connect_failed"));
      return;
    }
    if (!this.validated && this.outbound.length <= 64 * 1024) {
      this.outbound = Buffer.concat([this.outbound, chunk]);
      if (!this.tls) this.tls = looksLikeTlsClientHello(this.outbound);
    }
    if (this.socket && !this.socket.destroyed) this.socket.write(chunk, done);
    else done();
  }
  override _final(done: (error?: Error | null) => void) {
    this.finished = true;
    if (this.socket && !this.socket.destroyed) this.socket.end();
    done();
  }
  override _destroy(error: Error | null, done: (error?: Error | null) => void) {
    this.stopped = true;
    this.socket?.destroy();
    done(error);
  }
}

/** Reconnecting main-process/service client. Lease expiry is anchored to request time. */
export class BrowserNetworkClient {
  private ws?: WebSocket;
  private mux?: NetworkMux;
  private timer?: ReturnType<typeof setInterval>;
  private retry?: ReturnType<typeof setTimeout>;
  private pending = new Map<string, number>();
  private expires = 0;
  private stopped = true;
  private attempt = 0;
  private transitionUntil = 0;
  private readonly routeWaiters = new Set<{ resolve: () => void; reject: () => void }>();
  private releaseRouteWaiters(ready: boolean) {
    for (const waiter of this.routeWaiters) ready ? waiter.resolve() : waiter.reject();
    this.routeWaiters.clear();
  }
  private waitForRoute(signal?: AbortSignal): Promise<void> {
    if (this.available()) return Promise.resolve();
    const remaining = this.transitionUntil - performance.now();
    if (
      this.options.role !== "computer" ||
      remaining <= 0 ||
      this.ws?.readyState !== WebSocket.OPEN
    )
      return Promise.reject(new NetworkError("device_unavailable"));
    if (this.routeWaiters.size >= MAX_STREAMS) return Promise.reject(new NetworkError("capacity"));
    return new Promise((resolve, reject) => {
      const finish = (ready: boolean) => {
        clearTimeout(timer);
        signal?.removeEventListener("abort", waiter.reject);
        this.routeWaiters.delete(waiter);
        ready ? resolve() : reject(new NetworkError("device_unavailable"));
      };
      const waiter = { resolve: () => finish(true), reject: () => finish(false) };
      const timer = setTimeout(waiter.reject, remaining);
      this.routeWaiters.add(waiter);
      signal?.addEventListener("abort", waiter.reject, { once: true });
      if (signal?.aborted) waiter.reject();
    });
  }
  route: NetworkRoute | null = null;
  constructor(
    private readonly options: {
      url: string;
      headers: Record<string, string>;
      role: "computer" | "device";
      machineId?: string;
      deploymentId?: string;
      dial?: (target: OpenTarget, signal: AbortSignal) => Promise<Duplex>;
      resolver?: (route: NetworkRoute) => string;
      diagnostic?: (diagnostic: NetworkDnsDiagnostic) => void;
      changed?: () => void;
    }
  ) {}
  start() {
    if (!this.stopped) return;
    this.stopped = false;
    this.connect();
  }
  stop() {
    this.stopped = true;
    clearTimeout(this.retry);
    this.disconnect();
  }
  private disconnect() {
    this.transitionUntil = 0;
    this.releaseRouteWaiters(false);
    this.fence();
    clearInterval(this.timer);
    this.timer = undefined;
    this.route = null;
    this.expires = 0;
    this.pending.clear();
    const mux = this.mux;
    this.mux = undefined;
    mux?.close();
    const ws = this.ws;
    this.ws = undefined;
    ws?.terminate();
    this.options.changed?.();
  }
  available() {
    return (
      !!this.route?.available && performance.now() < this.expires && !!this.mux && !this.mux.closed
    );
  }
  status() {
    return {
      ready: this.available(),
      mode: this.route?.mode ?? null,
      activeConnections: (this.mux?.streams.size ?? 0) + this.direct.size,
    };
  }
  private challenge() {
    if (!this.mux || this.mux.closed) return;
    if (this.pending.size > 4) {
      this.mux.close();
      return;
    }
    const nonce = randomBytes(32).toString("base64url");
    this.pending.set(nonce, performance.now());
    this.mux.control({
      op: "lease",
      nonce,
      deploymentId: this.options.deploymentId ?? null,
      maxWindow: MAX_WINDOW,
    });
  }
  private connect() {
    if (this.stopped) return;
    const ws = new WebSocket(this.options.url, NETWORK_PROTOCOL, {
      headers: this.options.headers,
      perMessageDeflate: false,
      maxPayload: 65548,
      handshakeTimeout: 5_000,
      followRedirects: false,
    });
    this.ws = ws;
    ws.on("open", () => {
      if (this.ws !== ws || this.stopped) {
        ws.terminate();
        return;
      }
      const connectedAt = performance.now();
      const mux = new NetworkMux(
        {
          send: (bytes) => {
            ws.send(bytes);
          },
          close: () => ws.terminate(),
          get bufferedAmount() {
            return ws.bufferedAmount;
          },
        },
        {
          control: (message) => {
            if (message.op === "invalidate") {
              this.transitionUntil = performance.now() + 2000;
              this.route = null;
              this.expires = 0;
              this.pending.clear();
              this.fence();
              this.challenge();
              this.options.changed?.();
              return;
            }
            if (message.op !== "lease" || typeof message.nonce !== "string")
              throw new Error("Invalid network control");
            const started = this.pending.get(message.nonce);
            this.pending.delete(message.nonce);
            if (started === undefined || performance.now() >= started + LEASE_MS) return;
            const route = message.route as NetworkRoute;
            if (
              !route ||
              !["server", "device"].includes(route.mode) ||
              typeof route.epoch !== "string" ||
              !Number.isSafeInteger(route.revision) ||
              typeof route.available !== "boolean" ||
              (route.streamWindow !== undefined && !validWindow(route.streamWindow)) ||
              (this.options.deploymentId && route.deploymentId !== this.options.deploymentId)
            )
              throw new Error("Invalid route");
            if (
              this.route &&
              this.route.epoch === route.epoch &&
              (this.route.streamWindow ?? WINDOW) !== (route.streamWindow ?? WINDOW)
            )
              throw new Error("Window changed without fencing");
            if (this.route && this.route.epoch !== route.epoch) this.fence();
            mux.configureWindow(route.streamWindow ?? WINDOW);
            this.route = route;
            // Keep backoff across flapping connections, even if they briefly
            // grant a lease. Reset only after a full healthy lease interval.
            if (performance.now() - connectedAt >= LEASE_MS) this.attempt = 0;
            this.expires = started + LEASE_MS;
            mux.control({ op: "applied", epoch: route.epoch, revision: route.revision });
            if (this.available()) {
              this.transitionUntil = 0;
              this.releaseRouteWaiters(true);
            }
            this.options.changed?.();
          },
          dial:
            this.options.role === "device"
              ? async (target, signal) => {
                  this.assertRoute(target);
                  if (
                    this.route?.mode !== "device" ||
                    this.route.machineId !== this.options.machineId
                  )
                    throw new NetworkError("not_authorized");
                  const socket = await (
                    this.options.dial ?? ((t, s) => dialTarget(t, s, { publicOnly: true }))
                  )(target, signal);
                  try {
                    this.assertRoute(target);
                  } catch (e) {
                    socket.destroy();
                    throw e;
                  }
                  return socket;
                }
              : undefined,
        }
      );
      this.mux = mux;
      this.challenge();
      let lastRenew = performance.now();
      this.timer = setInterval(() => {
        if (this.expires && performance.now() >= this.expires) {
          this.transitionUntil = 0;
          this.releaseRouteWaiters(false);
          this.route = null;
          this.expires = 0;
          this.fence();
          this.options.changed?.();
        }
        if (performance.now() - lastRenew >= RENEW_MS) {
          lastRenew = performance.now();
          this.challenge();
        }
      }, 250);
      this.timer.unref?.();
    });
    ws.on("message", (bytes, binary) => {
      if (this.ws !== ws) return;
      if (!binary) {
        ws.terminate();
        return;
      }
      this.mux?.receive(Buffer.isBuffer(bytes) ? bytes : Buffer.from(bytes as ArrayBuffer));
    });
    ws.on("error", () => {});
    ws.on("close", () => {
      if (this.ws !== ws) return;
      this.disconnect();
      if (!this.stopped) {
        this.retry = setTimeout(
          () => this.connect(),
          Math.min(RECONNECT_MAX_MS, 1000 * 2 ** Math.min(this.attempt++, 10)) *
            (0.8 + Math.random() * 0.4)
        );
        this.retry.unref?.();
      }
    });
  }
  private assertRoute(target: OpenTarget) {
    if (!this.available() || target.epoch !== this.route?.epoch)
      throw new NetworkError("device_unavailable");
  }
  async open(host: string, port: number, signal?: AbortSignal): Promise<Duplex> {
    signal?.throwIfAborted();
    await this.waitForRoute(signal);
    signal?.throwIfAborted();
    const route = this.route;
    if (!route || !this.available()) throw new NetworkError("device_unavailable");
    const target = { host, port, epoch: route.epoch };
    if (route.mode === "server") {
      const controller = new AbortController();
      const abort = () => controller.abort();
      signal?.addEventListener("abort", abort, { once: true });
      this.directDials.add(controller);
      let socket: Duplex;
      try {
        const diagnostic: NetworkDnsDiagnostic = {
          host,
          port,
          resolver: this.options.resolver?.(route) ?? "system",
          addresses: [] as string[],
          selectedAddress: null as string | null,
          routeEpoch: route.epoch,
          status: "failed",
          checkedAt: new Date().toISOString(),
        };
        const addresses = await resolveTargetAddresses(target, controller.signal, {
          resolver: diagnostic.resolver,
          onResolved: (event) => {
            diagnostic.addresses = event.addresses.map((value) => value.address);
          },
        });
        if (port === 443 && addresses.length > 1) {
          const retrySocket = new EarlyTlsRetrySocket(
            addresses.slice(0, 8),
            controller.signal,
            (address) => connectTargetAddress(target, address, controller.signal),
            (address) => {
              diagnostic.selectedAddress = address.address;
              diagnostic.status = "connected";
              diagnostic.checkedAt = new Date().toISOString();
              this.options.diagnostic?.(diagnostic);
            }
          );
          socket = retrySocket;
          await retrySocket.ready;
        } else {
          const address = addresses[0]!;
          socket = await connectTargetAddress(target, address, controller.signal);
          diagnostic.selectedAddress = address.address;
          diagnostic.status = "connected";
          diagnostic.checkedAt = new Date().toISOString();
          this.options.diagnostic?.(diagnostic);
        }
      } finally {
        this.directDials.delete(controller);
        signal?.removeEventListener("abort", abort);
      }
      try {
        signal?.throwIfAborted();
        this.assertRoute(target);
      } catch (error) {
        socket.destroy();
        throw error;
      }
      // Server-mode sockets must be fenced on the same epoch changes as remote streams.
      this.direct.add(socket);
      socket.once("close", () => this.direct.delete(socket));
      return socket;
    }
    const stream: NetworkStream = this.mux!.open(target);
    const abort = () => stream.destroy(new NetworkError("request_cancelled"));
    signal?.addEventListener("abort", abort, { once: true });
    if (signal?.aborted) abort();
    try {
      await stream.ready;
      signal?.throwIfAborted();
      this.assertRoute(target);
      return stream;
    } catch (error) {
      stream.destroy();
      throw error;
    } finally {
      signal?.removeEventListener("abort", abort);
    }
  }
  private readonly direct = new Set<Duplex>();
  private readonly directDials = new Set<AbortController>();
  fence() {
    for (const dial of this.directDials) dial.abort();
    this.directDials.clear();
    for (const socket of this.direct) socket.destroy();
    this.direct.clear();
    this.mux?.reset();
  }
}
