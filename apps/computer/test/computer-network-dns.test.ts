import { expect, test } from "bun:test";
import { createSocket } from "node:dgram";
import { createConnection, type AddressInfo } from "node:net";
import {
  createDnsExchange,
  dnsFailure,
  dnsNodata,
  startComputerDns,
} from "../src/computer-network-dns";

const query = Buffer.from("007b01000001000000000000076578616d706c6503636f6d0000010001", "hex");
const aaaa = Buffer.from("007b01000001000000000000076578616d706c6503636f6d00001c0001", "hex");

test("AAAA is withheld only while the exit cannot route IPv6, and other types pass through", async () => {
  let opened = 0;
  const open = async () => {
    opened++;
    throw new Error("selected route unavailable");
  };
  const filtered = createDnsExchange({ open }, { ipv6: () => false });
  const nodata = await filtered(aaaa);
  expect(opened).toBe(0);
  expect(nodata.readUInt16BE(0)).toBe(123);
  expect(nodata[2]! & 0x80).toBe(0x80);
  expect(nodata[3]! & 0x0f).toBe(0);
  expect(nodata.readUInt16BE(6)).toBe(0);
  expect(nodata.subarray(12)).toEqual(aaaa.subarray(12));
  await expect(filtered(query)).rejects.toThrow("selected route unavailable");
  expect(opened).toBe(1);
  // A routable exit keeps its AAAA answers untouched.
  const direct = createDnsExchange({ open }, { ipv6: () => true });
  await expect(direct(aaaa)).rejects.toThrow("selected route unavailable");
  expect(opened).toBe(2);
  expect(dnsNodata(aaaa).length).toBe(aaaa.length);
});

test("computer DNS handles UDP, split and coalesced TCP, and route failure without another resolver", async () => {
  let failed = false,
    calls = 0;
  const dns = await startComputerDns(
    async (value) => {
      calls++;
      if (failed) throw new Error("selected device unavailable");
      const response = Buffer.from(value);
      response[2] = 0x81;
      response[3] = 0x80;
      return response;
    },
    0,
    ["127.0.0.1"]
  );
  const udp = createSocket("udp4");
  const tcp = createConnection({ host: "127.0.0.1", port: dns.port });
  try {
    const response = new Promise<Buffer>((resolve, reject) => {
      udp.once("message", resolve);
      udp.once("error", reject);
    });
    udp.send(query, dns.port, "127.0.0.1");
    expect((await response)[3]).toBe(0x80);
    const framed = Buffer.concat([Buffer.from([0, query.length]), query]);
    const messages: Buffer[] = [];
    const received = new Promise<void>((resolve, reject) => {
      let buffered = Buffer.alloc(0);
      tcp.on("data", (chunk) => {
        buffered = Buffer.concat([buffered, chunk]);
        while (buffered.length >= 2 && buffered.length >= buffered.readUInt16BE(0) + 2) {
          const size = buffered.readUInt16BE(0);
          messages.push(Buffer.from(buffered.subarray(2, size + 2)));
          buffered = buffered.subarray(size + 2);
          if (messages.length === 2) resolve();
        }
      });
      tcp.once("error", reject);
    });
    tcp.write(framed.subarray(0, 1));
    await Bun.sleep(5);
    tcp.write(Buffer.concat([framed.subarray(1), framed]));
    await received;
    expect(messages.map((value) => value.readUInt16BE(0))).toEqual([123, 123]);
    failed = true;
    const unavailable = new Promise<Buffer>((resolve) => udp.once("message", resolve));
    udp.send(query, dns.port, "127.0.0.1");
    expect((await unavailable)[3]! & 0xf).toBe(2);
    expect(calls).toBe(4);
    expect(dnsFailure(Buffer.from([1, 2])).subarray(0, 2)).toEqual(Buffer.from([1, 2]));
  } finally {
    udp.close();
    tcp.destroy();
    dns.close();
  }
}, 10_000);

test("malformed DNS never opens a transport, and failed admission never falls back", async () => {
  let calls = 0;
  const exchange = createDnsExchange({
    open: async (host, port) => {
      calls++;
      expect(host).toBe("doh.pub");
      expect(port).toBe(443);
      throw new Error("selected route unavailable");
    },
  });
  for (const malformed of [
    Buffer.alloc(0),
    Buffer.alloc(4097),
    Buffer.alloc(12),
    Buffer.from("000080000001000000000000", "hex"),
  ])
    await expect(exchange(malformed)).rejects.toThrow("Invalid DNS query");
  expect(calls).toBe(0);
  await expect(exchange(query)).rejects.toThrow("selected route unavailable");
  expect(calls).toBe(1);
});

test("server-mode DNS can use the Docker resolver without opening the exit tunnel", async () => {
  const upstream = createSocket("udp4");
  await new Promise<void>((resolve) => upstream.bind(0, "127.0.0.1", resolve));
  const port = (upstream.address() as AddressInfo).port;
  const response = new Promise<void>((resolve, reject) => {
    upstream.once("message", (packet, peer) => {
      const header = Buffer.from(packet.subarray(0, 12));
      header[2] = 0x81;
      header[3] = 0x80;
      header.writeUInt16BE(1, 6);
      upstream.send(
        Buffer.concat([
          header,
          packet.subarray(12),
          Buffer.from([0xc0, 0x0c, 0, 1, 0, 1, 0, 0, 0, 60, 0, 4, 11, 77, 0, 80]),
        ]),
        peer.port,
        peer.address
      );
      resolve();
    });
    upstream.once("error", reject);
  });
  try {
    const exchange = createDnsExchange(
      {
        open: async () => {
          throw new Error("DoH must not be opened");
        },
      },
      {
        resolver: () => "docker",
        dockerResolver: "127.0.0.1",
        dockerResolverPort: port,
      }
    );
    const result = await exchange(query);
    await response;
    expect(result.readUInt16BE(0)).toBe(123);
    expect(result.readUInt16BE(6)).toBe(1);
    expect(result.subarray(-4)).toEqual(Buffer.from([11, 77, 0, 80]));
    exchange.close();
  } finally {
    upstream.close();
  }
});

test("DNS deadlines include opening the tunnel and dispose of a late socket", async () => {
  let finish!: (value: any) => void;
  let destroyed = false;
  const exchange = createDnsExchange({
    open: () =>
      new Promise((resolve) => {
        finish = resolve;
      }),
  });
  await expect(exchange(query)).rejects.toThrow("DNS timeout");
  finish({
    destroy() {
      destroyed = true;
    },
  });
  await Bun.sleep(5);
  expect(destroyed).toBe(true);
}, 7000);

test("Node DNS reuses bounded TLS, fences epochs, cancels queued/late dials and never replays", async () => {
  const { mkdtemp, rm } = await import("node:fs/promises");
  const { tmpdir } = await import("node:os");
  const { join } = await import("node:path");
  const directory = await mkdtemp(join(tmpdir(), "outcomego-dns-pool-"));
  try {
    const cert = join(directory, "cert.pem"),
      key = join(directory, "key.pem");
    const ssl = Bun.spawn(
      [
        "openssl",
        "req",
        "-x509",
        "-newkey",
        "rsa:2048",
        "-nodes",
        "-keyout",
        key,
        "-out",
        cert,
        "-days",
        "1",
        "-subj",
        "/CN=doh.pub",
        "-addext",
        "subjectAltName=DNS:doh.pub",
      ],
      { stdout: "ignore", stderr: "ignore" }
    );
    expect(await ssl.exited).toBe(0);
    const bundle = join(directory, "fixture.mjs");
    const build = Bun.spawn(
      [
        process.execPath,
        "build",
        join(import.meta.dirname, "computer-network-dns-node.fixture.ts"),
        "--target",
        "node",
        "--outfile",
        bundle,
      ],
      { stdout: "ignore", stderr: "inherit" }
    );
    expect(await build.exited).toBe(0);
    const run = Bun.spawn(["node", bundle, cert, key], {
      env: { ...process.env, NODE_EXTRA_CA_CERTS: cert },
      stdout: "pipe",
      stderr: "pipe",
    });
    const [stdout, stderr, code] = await Promise.all([
      new Response(run.stdout).text(),
      new Response(run.stderr).text(),
      run.exited,
    ]);
    if (code !== 0) throw new Error(stderr || stdout);
    expect(JSON.parse(stdout).passed).toBe(true);
  } finally {
    await rm(directory, { recursive: true, force: true });
  }
}, 10000);
