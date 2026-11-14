import { randomUUID } from "node:crypto";

export const BITWARDEN_SECRET_TTL_MS = 15 * 60_000;

export type BitwardenSecretField = "username" | "password" | "totp";

type Entry = {
  value: string;
  expiresAt: number;
  timer: ReturnType<typeof setTimeout>;
};

const ENVIRONMENT_PREFIX = "OUTCOMEGO_BITWARDEN_";
const REFERENCE = /^\$(OUTCOMEGO_BITWARDEN_\w+)$|^\$\{(OUTCOMEGO_BITWARDEN_\w+)\}$/;

export const isBitwardenSecretReference = (value: unknown): value is string =>
  typeof value === "string" && REFERENCE.test(value.trim());

/**
 * Keeps an approved Bitwarden value in the Computer process only long enough
 * for a browser action to consume it. The variable name may enter the model
 * context; the value never does.
 */
export class BitwardenSecretStore {
  private readonly scopes = new Map<string, Map<string, Entry>>();

  constructor(
    private readonly environment: NodeJS.ProcessEnv = process.env,
    private readonly ttlMs = BITWARDEN_SECRET_TTL_MS,
    private readonly onDelete?: (scope: string, value: string) => void,
  ) {}

  put(scope: string, field: BitwardenSecretField, value: string): string {
    this.clearField(scope, field);
    const variable = `${ENVIRONMENT_PREFIX}${field.toUpperCase()}_${randomUUID().replaceAll("-", "").slice(0, 16)}`;
    const timer = setTimeout(() => {
      this.delete(scope, variable);
    }, this.ttlMs);
    timer.unref?.();
    this.environment[variable] = value;
    let entries = this.scopes.get(scope);
    if (!entries) {
      entries = new Map();
      this.scopes.set(scope, entries);
    }
    entries.set(variable, { value, expiresAt: Date.now() + this.ttlMs, timer });
    return variable;
  }

  resolve(scope: string, reference: unknown): { variable: string; value: string } | null {
    if (typeof reference !== "string") return null;
    const match = REFERENCE.exec(reference.trim());
    const variable = match?.[1] ?? match?.[2];
    if (!variable) return null;
    const entry = this.scopes.get(scope)?.get(variable);
    if (!entry) return null;
    if (entry.expiresAt <= Date.now()) {
      this.delete(scope, variable);
      return null;
    }
    return { variable, value: entry.value };
  }

  values(scope: string): string[] {
    const entries = this.scopes.get(scope);
    if (!entries) return [];
    const values: string[] = [];
    for (const variable of entries.keys()) {
      const resolved = this.resolve(scope, `$${variable}`);
      if (resolved) values.push(resolved.value);
    }
    return values;
  }

  has(scope: string): boolean {
    return this.scopes.has(scope);
  }

  clearScope(scope: string): string[] {
    const entries = this.scopes.get(scope);
    if (!entries) return [];
    const values = [...entries.values()].map((entry) => entry.value);
    for (const variable of entries.keys()) this.delete(scope, variable);
    return values;
  }

  clearAll(): void {
    for (const scope of [...this.scopes.keys()]) this.clearScope(scope);
  }

  private clearField(scope: string, field: BitwardenSecretField): void {
    const prefix = `${ENVIRONMENT_PREFIX}${field.toUpperCase()}_`;
    const entries = this.scopes.get(scope);
    if (!entries) return;
    for (const variable of [...entries.keys()])
      if (variable.startsWith(prefix)) this.delete(scope, variable);
  }

  private delete(scope: string, variable: string): void {
    const entries = this.scopes.get(scope);
    const entry = entries?.get(variable);
    if (!entry) return;
    clearTimeout(entry.timer);
    if (this.environment[variable] === entry.value) delete this.environment[variable];
    entries!.delete(variable);
    if (!entries!.size) this.scopes.delete(scope);
    this.onDelete?.(scope, entry.value);
  }
}
