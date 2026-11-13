-- Protected process fields are withheld from ordinary Shell calls.
ALTER TABLE "outcomego_process_secret"
  ADD COLUMN IF NOT EXISTS "protected" BOOLEAN NOT NULL DEFAULT false;

UPDATE "outcomego_process_secret"
SET "protected" = true
WHERE "ownerKey" = 'plugin:bitwarden'
  AND "name" IN ('BW_SERVER_URL', 'BW_EMAIL', 'BW_CLIENT_ID', 'BW_CLIENT_SECRET', 'BW_MASTER_PASSWORD');

CREATE TYPE "outcomego_bitwarden_read_approval_status" AS ENUM ('pending', 'approved', 'denied', 'expired');

CREATE TABLE "outcomego_bitwarden_read_approval" (
  "id" UUID NOT NULL,
  "botId" UUID NOT NULL,
  "channelId" UUID NOT NULL,
  "sourceRunId" UUID NOT NULL,
  "widgetMessageId" UUID,
  "itemId" TEXT,
  "title" TEXT NOT NULL,
  "hostname" TEXT NOT NULL,
  "status" "outcomego_bitwarden_read_approval_status" NOT NULL DEFAULT 'pending',
  "createdAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "expiresAt" TIMESTAMP(3) NOT NULL,
  "approvedAt" TIMESTAMP(3),
  "consumedAt" TIMESTAMP(3),
  "leaseTokenHash" TEXT,
  "leaseExpiresAt" TIMESTAMP(3),
  CONSTRAINT "outcomego_bitwarden_read_approval_pkey" PRIMARY KEY ("id"),
  CONSTRAINT "outcomego_bitwarden_read_approval_widgetMessageId_key" UNIQUE ("widgetMessageId"),
  CONSTRAINT "outcomego_bitwarden_read_approval_botId_fkey" FOREIGN KEY ("botId") REFERENCES "outcomego_bot"("id") ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT "outcomego_bitwarden_read_approval_channelId_fkey" FOREIGN KEY ("channelId") REFERENCES "outcomego_channel"("id") ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT "outcomego_bitwarden_read_approval_sourceRunId_fkey" FOREIGN KEY ("sourceRunId") REFERENCES "outcomego_run"("id") ON DELETE CASCADE ON UPDATE CASCADE
);

CREATE INDEX "outcomego_bitwarden_read_approval_lookup_idx"
  ON "outcomego_bitwarden_read_approval" ("botId", "channelId", "status", "createdAt");
CREATE INDEX "outcomego_bitwarden_read_approval_source_run_idx"
  ON "outcomego_bitwarden_read_approval" ("sourceRunId", "status");
CREATE INDEX "outcomego_bitwarden_read_approval_lease_idx"
  ON "outcomego_bitwarden_read_approval" ("leaseTokenHash");
