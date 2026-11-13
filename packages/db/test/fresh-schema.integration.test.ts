import { expect, test } from "bun:test";
import { join } from "node:path";
import { Client } from "pg";

const url = process.env.OUTCOMEGO_TEST_DATABASE_URL;

const deploy = async (databaseUrl: string): Promise<void> => {
  const child = Bun.spawn(["bun", "scripts/deploy-schema.ts"], {
    cwd: join(import.meta.dirname, ".."),
    env: { ...process.env, DATABASE_URL: databaseUrl },
    stdin: "ignore",
    stdout: "pipe",
    stderr: "pipe",
  });
  const [status, stdout, stderr] = await Promise.all([
    child.exited,
    new Response(child.stdout).text(),
    new Response(child.stderr).text(),
  ]);
  if (status !== 0) throw new Error(`deploy-schema exited ${status}\n${stdout}\n${stderr}`);
};

test.skipIf(!url)("a fresh database receives the complete current schema and can be redeployed", async () => {
  const name = `outcomego_fresh_${crypto.randomUUID().slice(0, 8)}`;
  const admin = new Client({ connectionString: url! });
  await admin.connect();
  await admin.query(`CREATE DATABASE "${name}"`);
  const target = new URL(url!);
  target.pathname = `/${name}`;
  const db = new Client({ connectionString: target.toString() });

  try {
    await db.connect();
    await db.end();
    await deploy(target.toString());
    await db.connect();

    const columns = await db.query<{ table_name: string; column_name: string }>(`
      SELECT table_name, column_name FROM information_schema.columns
      WHERE table_schema = current_schema()
        AND table_name IN ('outcomego_web_search_settings', 'outcomego_web_fetch_settings')
        AND column_name IN ('providers', 'strategy', 'cursor', 'provider')
    `);
    expect(columns.rows.map((row) => `${row.table_name}.${row.column_name}`).sort()).toEqual([
      "outcomego_web_fetch_settings.cursor",
      "outcomego_web_fetch_settings.providers",
      "outcomego_web_fetch_settings.strategy",
      "outcomego_web_search_settings.cursor",
      "outcomego_web_search_settings.providers",
      "outcomego_web_search_settings.strategy",
    ]);

    const oldProviderBackfillFiles = await Array.fromAsync(
      new Bun.Glob("prisma/migrations/*/migration.sql").scan({ cwd: join(import.meta.dirname, "..") })
    );
    for (const file of oldProviderBackfillFiles) {
      expect(await Bun.file(join(import.meta.dirname, "..", file)).text()).not.toMatch(/SET\s+"providers"\s*=\s*jsonb_build_array\("provider"\)/i);
    }

    const marker = await db.query<{ comment: string }>(`
      SELECT obj_description(n.oid, 'pg_namespace') AS comment
      FROM pg_namespace n
      WHERE n.nspname = current_schema()
    `);
    expect(marker.rows[0]?.comment).toMatch(/^OutcomeGo schema ready: outcomego-v1:[0-9a-f]{64}$/);

    const channel = await db.query<{ routine: string }>(`
      SELECT pg_get_functiondef('public.outcomego_notify_event()'::regprocedure) AS routine
    `);
    expect(channel.rows[0]?.routine).toContain("aiteamos_events");

    const tablesBefore = await db.query<{ count: number }>(`
      SELECT count(*)::int AS count FROM information_schema.tables
      WHERE table_schema = current_schema()
    `);
    await deploy(target.toString());
    const tablesAfter = await db.query<{ count: number }>(`
      SELECT count(*)::int AS count FROM information_schema.tables
      WHERE table_schema = current_schema()
    `);
    expect(tablesAfter.rows[0]?.count).toBe(tablesBefore.rows[0]?.count);

    await db.query("COMMENT ON SCHEMA public IS 'OutcomeGo schema ready: outcomego-v1:outdated'");
    const upgraded = Bun.spawn(["bun", "scripts/deploy-schema.ts"], {
      cwd: join(import.meta.dirname, ".."),
      env: { ...process.env, DATABASE_URL: target.toString() },
      stdin: "ignore",
      stdout: "pipe",
      stderr: "pipe",
    });
    const [upgradedStatus, upgradedStdout, upgradedStderr] = await Promise.all([
      upgraded.exited,
      new Response(upgraded.stdout).text(),
      new Response(upgraded.stderr).text(),
    ]);
    expect(upgradedStatus, `${upgradedStdout}\n${upgradedStderr}`).toBe(0);
    const upgradedMarker = await db.query<{ comment: string }>(`
      SELECT obj_description(n.oid, 'pg_namespace') AS comment
      FROM pg_namespace n
      WHERE n.nspname = current_schema()
    `);
    expect(upgradedMarker.rows[0]?.comment).toMatch(/^OutcomeGo schema ready: outcomego-v1:[0-9a-f]{64}$/);
  } finally {
    await db.end().catch(() => {});
    await admin.query(`DROP DATABASE IF EXISTS "${name}" WITH (FORCE)`);
    await admin.end();
  }
}, 120_000);

test.skipIf(!url)("an unmarked nonempty database is rejected without changing its tables", async () => {
  const name = `outcomego_unmarked_${crypto.randomUUID().slice(0, 8)}`;
  const admin = new Client({ connectionString: url! });
  await admin.connect();
  await admin.query(`CREATE DATABASE "${name}"`);
  const target = new URL(url!);
  target.pathname = `/${name}`;
  const db = new Client({ connectionString: target.toString() });

  try {
    await db.connect();
    await db.query("CREATE TABLE legacy_data (id integer PRIMARY KEY)");

    const child = Bun.spawn(["bun", "scripts/deploy-schema.ts"], {
      cwd: join(import.meta.dirname, ".."),
      env: { ...process.env, DATABASE_URL: target.toString() },
      stdin: "ignore",
      stdout: "pipe",
      stderr: "pipe",
    });
    const [status, stdout, stderr] = await Promise.all([
      child.exited,
      new Response(child.stdout).text(),
      new Response(child.stderr).text(),
    ]);
    expect(status).not.toBe(0);
    expect(`${stdout}\n${stderr}`).toContain("Database is not a recognized OutcomeGo schema");

    const tables = await db.query<{ table_name: string }>(`
      SELECT table_name FROM information_schema.tables
      WHERE table_schema = current_schema() AND table_type = 'BASE TABLE'
    `);
    expect(tables.rows.map((row) => row.table_name)).toEqual(["legacy_data"]);
  } finally {
    await db.end().catch(() => {});
    await admin.query(`DROP DATABASE IF EXISTS "${name}" WITH (FORCE)`);
    await admin.end();
  }
}, 120_000);

test.skipIf(!url)("a user-created empty schema without objects can be initialized", async () => {
  const name = `outcomego_empty_schema_${crypto.randomUUID().slice(0, 8)}`;
  const admin = new Client({ connectionString: url! });
  await admin.connect();
  await admin.query(`CREATE DATABASE "${name}"`);
  const target = new URL(url!);
  target.pathname = `/${name}`;
  const db = new Client({ connectionString: target.toString() });

  try {
    await db.connect();
    await db.end();

    const child = Bun.spawn(["bun", "scripts/deploy-schema.ts"], {
      cwd: join(import.meta.dirname, ".."),
      env: {
        ...process.env,
        DATABASE_URL: target.toString(),
      },
      stdin: "ignore",
      stdout: "pipe",
      stderr: "pipe",
    });
    const [status, stdout, stderr] = await Promise.all([
      child.exited,
      new Response(child.stdout).text(),
      new Response(child.stderr).text(),
    ]);
    expect(status, `${stdout}\n${stderr}`).toBe(0);
  } finally {
    await db.end().catch(() => {});
    await admin.query(`DROP DATABASE IF EXISTS "${name}" WITH (FORCE)`);
    await admin.end();
  }
}, 120_000);
