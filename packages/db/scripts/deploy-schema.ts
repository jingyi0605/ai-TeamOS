import { createHash } from "node:crypto";
import { readFile } from "node:fs/promises";
import { join } from "node:path";
import { Client } from "pg";
import { applyRawSchema } from "./apply-raw-schema";

const connectionString =
  process.env.DATABASE_URL ?? "postgresql://outcomego:outcomego@127.0.0.1:5432/outcomego";
const SCHEMA_VERSION = "outcomego-v1";
const schemaFingerprint = createHash("sha256")
  .update(await readFile(join(import.meta.dirname, "..", "prisma", "schema.prisma")))
  .update("\0")
  .update(await readFile(join(import.meta.dirname, "..", "prisma", "sql", "raw-schema.sql")))
  .digest("hex");
const INITIALIZING_MARKER = `OutcomeGo schema initializing: ${SCHEMA_VERSION}:${schemaFingerprint}`;
const READY_MARKER = `OutcomeGo schema ready: ${SCHEMA_VERSION}:${schemaFingerprint}`;

const assertFreshOrCurrentDatabase = async (): Promise<"initialize" | "ready"> => {
  const client = new Client({ connectionString });
  await client.connect();
  try {
    const existing = await client.query<{ has_objects: boolean }>(`
      SELECT (
        EXISTS (
          SELECT 1 FROM pg_class
          WHERE relnamespace = current_schema()::regnamespace
            AND relkind IN ('r', 'p', 'v', 'm', 'S', 'f')
        ) OR EXISTS (
          SELECT 1 FROM pg_type
          WHERE typnamespace = current_schema()::regnamespace
            AND typtype IN ('e', 'd')
        ) OR EXISTS (
          SELECT 1 FROM pg_proc
          WHERE pronamespace = current_schema()::regnamespace
        )
      ) AS has_objects
    `);
    const schemaResult = await client.query<{ comment: string | null }>(`
      SELECT obj_description(n.oid, 'pg_namespace') AS comment
      FROM pg_namespace n
      WHERE n.nspname = current_schema()
    `);
    const schemaComment = schemaResult.rows[0]?.comment ?? null;

    if (
      !existing.rows[0]?.has_objects &&
      (schemaComment === null || schemaComment === "standard public schema")
    ) {
      return "initialize";
    }
    if (schemaComment === READY_MARKER && existing.rows[0]?.has_objects) return "ready";
    if (schemaComment === INITIALIZING_MARKER) return "initialize";
    if (existing.rows[0]?.has_objects) {
      throw new Error(
        "Database does not match the current OutcomeGo schema. In-place database upgrades are not supported; create a fresh database and import a validated outcomego-v1 backup."
      );
    }
    throw new Error("Database schema marker does not match the current OutcomeGo schema. Create a fresh database for this version.");
  } finally {
    await client.end();
  }
};

const markSchema = async (marker: string): Promise<void> => {
  const client = new Client({ connectionString });
  await client.connect();
  try {
    const result = await client.query<{ schema: string }>("SELECT current_schema() AS schema");
    const schema = `"${result.rows[0]!.schema.replaceAll('"', '""')}"`;
    await client.query(`COMMENT ON SCHEMA ${schema} IS '${marker}'`);
  } finally {
    await client.end();
  }
};

if ((await assertFreshOrCurrentDatabase()) === "ready") process.exit(0);

// A matching initializing marker lets an interrupted first install retry without admitting older schemas.
await markSchema(INITIALIZING_MARKER);

const prisma = Bun.spawn(
  [
    join(import.meta.dirname, "..", "node_modules", ".bin", "prisma"),
    "db",
    "push",
    "--config",
    "prisma.config.ts",
  ],
  {
    cwd: join(import.meta.dirname, ".."),
    env: process.env,
    stdin: "inherit",
    stdout: "inherit",
    stderr: "inherit",
  }
);

const status = await prisma.exited;
if (status !== 0) process.exit(status);

await applyRawSchema();
await markSchema(READY_MARKER);
