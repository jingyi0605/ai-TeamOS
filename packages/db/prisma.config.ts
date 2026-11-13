import { join } from "node:path";
import { config } from "dotenv";
import { defineConfig } from "prisma/config";

config({ path: join(import.meta.dirname, "..", "..", ".env"), quiet: true });

export default defineConfig({
  schema: "prisma/schema.prisma",
  datasource: {
    url: process.env.DATABASE_URL ?? "postgresql://outcomego:outcomego@127.0.0.1:5432/outcomego",
  },
});
