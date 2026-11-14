import { useEffect, useMemo, useState } from "react";
import type { BrowserNetworkView } from "@outcomego/contracts/browser-network";
import { api } from "../../client/outcomego-api";
import { clientErrorMessage } from "@outcomego/product-core/redaction";
import { browserNetworkLabel } from "../../lib/browser-network-status";
import { useWindowVisible } from "../../hooks/use-window-active";
import { Check, ChevronDown } from "lucide-react";
import { useI18n } from "../../lib/i18n";

export function BrowserNetworkIndicator() {
  const { t } = useI18n();
  const visible = useWindowVisible();
  const [view, setView] = useState<BrowserNetworkView | null>(null);
  const [open, setOpen] = useState(false);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  useEffect(() => {
    if (!visible) return;
    let active = true;
    const load = () => {
      void api
        .browserNetwork()
        .then((value) => {
          if (active) setView(value);
        })
        .catch(() => {
          if (active)
            setView((current) =>
              current ? { ...current, runtime: { ...current.runtime, state: "unavailable" } } : null
            );
        });
    };
    load();
    const timer = setInterval(load, 5000);
    return () => {
      active = false;
      clearInterval(timer);
    };
  }, [visible]);
  const activeSource =
    view?.runtime.state === "server" ? "server" : view?.runtime.activeMachineId ?? null;
  const sources = useMemo(
    () =>
      view?.settings.sourceOrder.map((id) => {
        const device = view.devices.find((candidate) => candidate.machineId === id);
        const name = id === "server" ? t("Server connection") : device?.label ?? t("Unavailable computer");
        const available =
          id === "server" ||
          Boolean(
            device?.enabled &&
              (device.working ?? (device.connected && device.sharingAllowed))
          );
        return { id, name, available };
      }) ?? [],
    [t, view]
  );

  const selectSource = async (sourceId: string) => {
    if (!view || busy || sourceId === activeSource) return;
    const source = sources.find((candidate) => candidate.id === sourceId);
    if (!source?.available) return;
    setBusy(true);
    setError(null);
    try {
      const sourceOrder = [
        sourceId,
        ...view.settings.sourceOrder.filter((candidate) => candidate !== sourceId),
      ];
      const next = await api.updateBrowserNetwork({
        expectedRevision: view.settings.revision,
        mode: "ranked",
        machineId: null,
        sourceOrder,
      });
      setView(next);
      setOpen(false);
    } catch (cause) {
      setError(clientErrorMessage(cause, t("Could not change computer connection")));
      void api
        .browserNetwork()
        .then((next) => setView(next))
        .catch(() => undefined);
    } finally {
      setBusy(false);
    }
  };

  if (!view) return null;
  const activeLabel = browserNetworkLabel(view).split(" · ")[0] ?? "";
  return (
    <div className="electron-no-drag relative shrink-0">
      <button
        type="button"
        className="flex h-8 max-w-[180px] items-center gap-1 rounded-md border border-border bg-background px-2.5 text-left text-[12px] text-foreground-secondary outline-none transition-colors hover:bg-subtle hover:text-foreground focus-visible:ring-2 focus-visible:ring-ring/35"
        aria-expanded={open}
        aria-haspopup="listbox"
        onClick={() => {
          setError(null);
          setOpen((current) => !current);
        }}
      >
        <span className="min-w-0 truncate">{activeLabel}</span>
        <ChevronDown
          size={13}
          aria-hidden="true"
          className={open ? "rotate-180" : undefined}
        />
      </button>
      {open && (
        <div
          className="absolute right-0 top-full z-50 mt-1 min-w-56 max-w-[min(320px,calc(100vw-24px))] rounded-md border border-border bg-background p-1 text-xs text-foreground shadow-lg"
          role="listbox"
          aria-label={t("Select network exit")}
        >
          <div className="px-2 py-1 text-[10px] uppercase tracking-wide text-foreground-secondary">
            {t("Network exit")}
          </div>
          {sources.map((source) => {
            const active = source.id === activeSource;
            return (
              <button
                key={source.id}
                type="button"
                role="option"
                aria-selected={active}
                disabled={busy || !source.available || active}
                className="flex w-full items-center justify-between gap-3 rounded px-2 py-1.5 text-left outline-none hover:bg-subtle focus-visible:ring-2 focus-visible:ring-ring/35 disabled:cursor-default disabled:opacity-50"
                onClick={() => void selectSource(source.id)}
              >
                <span className="truncate">{source.name}</span>
                <span className="flex shrink-0 items-center gap-1 text-[10px] text-foreground-secondary">
                  {active
                    ? t("In use")
                    : source.available
                      ? t("Working")
                      : t("Not working")}
                  {active && <Check size={12} aria-hidden="true" />}
                </span>
              </button>
            );
          })}
          {error && <p className="px-2 py-1 text-[11px] text-destructive">{error}</p>}
        </div>
      )}
    </div>
  );
}
