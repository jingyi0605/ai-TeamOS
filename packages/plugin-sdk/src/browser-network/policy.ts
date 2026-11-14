import { lookup } from "node:dns/promises";
import { createConnection, isIP, type Socket } from "node:net";
import { domainToASCII } from "node:url";
import ipaddr from "ipaddr.js";
import type { OpenTarget } from "./protocol";

export class NetworkError extends Error {
  constructor(readonly code: string) {
    super(code);
  }
}
export function publicAddress(address: string): boolean {
  try {
    const ip = ipaddr.process(address);
    if (ip.range() !== "unicast") return false;
    // Reject translation/transition ranges instead of accepting embedded private IPs.
    if (ip.kind() === "ipv6") {
      const v6 = ip as ipaddr.IPv6;
      if (
        ["64:ff9b::/96", "64:ff9b:1::/48", "2001::/23", "2002::/16", "3fff::/20"].some((cidr) =>
          v6.match(ipaddr.parseCIDR(cidr) as [ipaddr.IPv6, number])
        )
      )
        return false;
    }
    return true;
  } catch {
    return false;
  }
}
export function canonicalHost(host: string): string {
  if (host.includes("%")) throw new NetworkError("destination_blocked");
  if (isIP(host)) return host;
  const name = domainToASCII(host).toLowerCase().replace(/\.$/, "");
  if (
    !name ||
    name.length > 253 ||
    name === "localhost" ||
    /\.(?:localhost|local)$/.test(name) ||
    !name.split(".").every((part) => /^[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?$/.test(part)) ||
    /^[0-9.]+$/.test(name) ||
    /^(?:0x[\da-f]+|0[0-7]+)$/i.test(name)
  )
    throw new NetworkError("destination_blocked");
  return name;
}
export async function dialTarget(
  target: OpenTarget,
  signal: AbortSignal,
  options: {
    publicOnly?: boolean;
    ports?: readonly number[];
    lookup?: typeof lookup;
    dial?: typeof createConnection;
    resolver?: string;
    onResolved?: (event: {
      host: string;
      port: number;
      resolver: string;
      addresses: Array<{ address: string; family: number }>;
      routeEpoch: string;
    }) => void;
    onConnected?: (event: {
      host: string;
      port: number;
      address: string;
      family: number;
      routeEpoch: string;
    }) => void;
  } = {}
): Promise<Socket> {
  const timeout = AbortSignal.timeout(15_000);
  const life = AbortSignal.any([signal, timeout]);
  life.throwIfAborted();
  if (options.publicOnly && options.ports && !options.ports.includes(target.port))
    throw new NetworkError("port_blocked");
  const addresses = await resolveTargetAddresses(target, life, options);
  for (const address of addresses.slice(0, 8)) {
    life.throwIfAborted();
    try {
      const socket = await connectTargetAddress(target, address, life, options);
      options.onConnected?.({
        host: target.host,
        port: target.port,
        address: address.address,
        family: address.family,
        routeEpoch: target.epoch,
      });
      return socket;
    } catch {
      if (life.aborted) throw new NetworkError("connect_timeout");
    }
  }
  throw new NetworkError("connect_failed");
}

export type TargetAddress = { address: string; family: number };

export async function resolveTargetAddresses(
  target: OpenTarget,
  signal: AbortSignal,
  options: {
    publicOnly?: boolean;
    ports?: readonly number[];
    lookup?: typeof lookup;
    resolver?: string;
    onResolved?: (event: {
      host: string;
      port: number;
      resolver: string;
      addresses: TargetAddress[];
      routeEpoch: string;
    }) => void;
  } = {}
): Promise<TargetAddress[]> {
  if (options.publicOnly && options.ports && !options.ports.includes(target.port))
    throw new NetworkError("port_blocked");
  const life = AbortSignal.any([signal, AbortSignal.timeout(15_000)]);
  life.throwIfAborted();
  const host = options.publicOnly ? canonicalHost(target.host) : target.host;
  let addresses: TargetAddress[];
  if (isIP(host)) addresses = [{ address: host, family: isIP(host) }];
  else {
    const dnsSignal = AbortSignal.any([life, AbortSignal.timeout(5_000)]);
    addresses = await new Promise((resolve, reject) => {
      const abort = () => reject(new NetworkError("dns_failed"));
      dnsSignal.addEventListener("abort", abort, { once: true });
      (options.lookup ?? lookup)(host, { all: true })
        .then(resolve, () => reject(new NetworkError("dns_failed")))
        .finally(() => dnsSignal.removeEventListener("abort", abort));
      if (dnsSignal.aborted) abort();
    });
  }
  life.throwIfAborted();
  if (!addresses.length) throw new NetworkError("dns_failed");
  if (options.publicOnly && addresses.some((a) => !publicAddress(a.address)))
    throw new NetworkError("destination_blocked");
  options.onResolved?.({
    host: target.host,
    port: target.port,
    resolver: options.resolver ?? "system",
    addresses: addresses.slice(0, 8),
    routeEpoch: target.epoch,
  });
  return addresses;
}

export async function connectTargetAddress(
  target: OpenTarget,
  address: TargetAddress,
  signal: AbortSignal,
  options: { dial?: typeof createConnection } = {}
): Promise<Socket> {
  const attempt = AbortSignal.any([signal, AbortSignal.timeout(3_000)]);
  return new Promise<Socket>((resolve, reject) => {
    const socket = (options.dial ?? createConnection)({
      host: address.address,
      family: address.family,
      port: target.port,
      allowHalfOpen: true,
      noDelay: true,
    });
    const abort = () => socket.destroy(new NetworkError("connect_timeout"));
    const cleanup = () => attempt.removeEventListener("abort", abort);
    socket.once("error", (error) => {
      cleanup();
      reject(error);
    });
    socket.once("connect", () => {
      cleanup();
      if (signal.aborted) {
        socket.destroy();
        reject(new NetworkError("not_authorized"));
      } else resolve(socket);
    });
    attempt.addEventListener("abort", abort, { once: true });
    if (attempt.aborted) abort();
  });
}
