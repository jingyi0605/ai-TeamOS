import "./browser-network.css";
import { useEffect, useRef, useState } from "react";
import type {
  BrowserNetworkView,
  NetworkSharingStatus,
  NetworkEgress,
  NetworkQuality,
} from "@outcomego/contracts/browser-network";
import { clientErrorMessage } from "@outcomego/product-core/redaction";
import { api } from "../../../client/outcomego-api";
import { Laptop, Monitor, Server } from "lucide-react";
import { PrivateSettingValue } from "./private-value";
import { SectionLabel, SettingsGroup, settingsSecondaryButton } from "./ui";

import { browserNetworkLabel } from "../../../lib/browser-network-status";
import { useWindowVisible } from "../../../hooks/use-window-active";
import { useI18n } from "../../../lib/i18n";

export function BrowserNetworkSettings() {
  const { t } = useI18n();
  const visible = useWindowVisible();
  const [view, setView] = useState<BrowserNetworkView | null>(null);
  const [egress, setEgress] = useState<NetworkEgress | null>(null);
  const [quality, setQuality] = useState<NetworkQuality | null>(null);
  const [testing, setTesting] = useState(false);
  const [qualityFailed, setQualityFailed] = useState(false);
  const [egressFailed, setEgressFailed] = useState(false);
  const [localMachineId, setLocalMachineId] = useState<string | null>(null);
  const [sharing, setSharing] = useState<NetworkSharingStatus | null>(null);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  useEffect(() => {
    if (!visible) return;
    let active = true;
    const refresh = async () => {
      const results = await Promise.allSettled([
        api.browserNetwork(),
        window.outcomego?.computer.network() ?? Promise.resolve(null),
        window.outcomego?.auth?.machineStatus?.() ?? Promise.resolve(null),
      ]);
      if (!active) return;
      if (results[0].status === "fulfilled") setView(results[0].value);
      else setError(clientErrorMessage(results[0].reason, t("Could not load computer networking")));
      if (results[1].status === "fulfilled") setSharing(results[1].value);
      setLocalMachineId(
        results[2].status === "fulfilled" && results[2].value?.configured
          ? results[2].value.machineId
          : null
      );
    };
    void refresh();
    const timer = setInterval(() => void refresh(), 5000);
    return () => {
      active = false;
      clearInterval(timer);
    };
  }, [visible]);
  const routeKey =
    view?.runtime.routeEpoch ??
    `${view?.settings.revision}:${view?.runtime.state}:${view?.runtime.activeMachineId}`;
  const routeRef = useRef(routeKey);
  routeRef.current = routeKey;
  useEffect(() => {
    setQuality(null);
    setQualityFailed(false);
  }, [routeKey]);
  const testConnection = async () => {
    if (testing) return;
    const epoch = routeKey;
    setTesting(true);
    setQualityFailed(false);
    try {
      const result = await api.browserNetworkQuality();
      if (routeRef.current === epoch && result.epoch === view?.runtime.routeEpoch)
        setQuality(result);
    } catch {
      if (routeRef.current === epoch) {
        setQuality(null);
        setQualityFailed(true);
      }
    } finally {
      setTesting(false);
    }
  };
  useEffect(() => {
    if (!visible || !view || !["ready", "server"].includes(view.runtime.state)) return;
    let active = true;
    setEgress(null);
    setEgressFailed(false);
    const check = async () => {
      try {
        const value = await api.browserNetworkEgress();
        if (active) {
          setEgress(value);
          setEgressFailed(false);
        }
      } catch {
        if (active) {
          setEgress(null);
          setEgressFailed(true);
        }
      }
    };
    void check();
    const timer = setInterval(() => void check(), 60_000);
    return () => {
      active = false;
      clearInterval(timer);
    };
  }, [routeKey, view?.runtime.state, visible]);
  const ranked = view?.settings.sourceOrder?.length ? view.settings.sourceOrder : ["server"];
  const activeId = view?.runtime.state === "server" ? "server" : view?.runtime.activeMachineId;
  const ready = view && ["ready", "server"].includes(view.runtime.state);
  const selectSource = async (sourceId: string) => {
    if (!view || busy || sourceId === activeId) return;
    const device = view.devices.find((candidate) => candidate.machineId === sourceId);
    const available =
      sourceId === "server" ||
      (!!device?.enabled && (device.working ?? (device.connected && device.sharingAllowed)));
    if (!available) return;
    setBusy(true);
    setError(null);
    try {
      setView(
        await api.updateBrowserNetwork({
          expectedRevision: view.settings.revision,
          mode: "ranked",
          machineId: null,
          sourceOrder: [sourceId, ...view.settings.sourceOrder.filter((candidate) => candidate !== sourceId)],
        })
      );
    } catch (e) {
      setError(clientErrorMessage(e, t("Could not change computer connection")));
      try {
        setView(await api.browserNetwork());
      } catch {
        /* Keep the original error visible while offline. */
      }
    } finally {
      setBusy(false);
    }
  };
  const share = async (allowed: boolean) => {
    if (
      allowed &&
      !window.confirm(
        t(
          "Allow all apps and commands on the bot computer at {server} to use this computer's internet connection while OutcomeGo is open? Public TCP connections and DNS are supported. Other UDP traffic and local and private network destinations remain blocked.",
          { server: sharing?.serverUrl ?? t("this server") }
        )
      )
    )
      return;
    setBusy(true);
    setError(null);
    try {
      const result = await window.outcomego?.computer.shareNetwork(allowed);
      if (result) setSharing(result);
      setView(await api.browserNetwork());
    } catch (e) {
      setError(clientErrorMessage(e, t("Could not change network sharing")));
    } finally {
      setBusy(false);
    }
  };
  return (
    <section className="bn-network" data-settings-anchor="browser-network">
      <SectionLabel>{t("Computer network")}</SectionLabel>
      <SettingsGroup>
        <p role="status" className="bn-status">
          {view ? t(browserNetworkLabel(view).split(" · ")[0] ?? "") : t("Loading connections…")}
        </p>
        <ol aria-label={t("Network exits")} className="bn-list">
          {ranked.map((id, index) => {
            const device = view?.devices.find((d) => d.machineId === id);
            const name =
              id === "server"
                ? t("Server connection")
                : (device?.label ?? t("Unavailable computer"));
            const available =
              id === "server" ||
              (!!device?.enabled &&
                (device.working ?? (device.connected && device.sharingAllowed)));
            return (
              <NetworkSourceRow
                key={id}
                id={id}
                name={name}
                index={index}
                active={!!ready && activeId === id}
                current={!!device && id === localMachineId}
                available={available}
                disabled={busy || !view || !available || activeId === id}
                onSelect={() => void selectSource(id)}
              />
            );
          })}
        </ol>
        <div className="bn-egress" key={routeKey}>
          <span>{t("Exit IP")}</span>
          {ready &&
          egress &&
          (!view?.runtime.routeEpoch || egress.epoch === view.runtime.routeEpoch) ? (
            <div className="bn-egress-values">
              <PrivateSettingValue key={egress.ip} value={egress.ip} label={t("exit IP")} />
              <span aria-hidden="true">·</span>
              <PrivateSettingValue
                key={egress.location}
                value={egress.location}
                label={t("Exit location")}
              />
            </div>
          ) : (
            <span className="bn-egress-pending">
              {!ready || egressFailed ? t("Unavailable") : t("Checking…")}
            </span>
          )}
        </div>
        <div className="bn-quality">
          <span className="bn-quality-label">{t("Connection test")}</span>
          <div className="bn-quality-values" aria-live="polite">
            {ready && quality?.epoch === view?.runtime.routeEpoch && quality ? (
              <>
                <span aria-label={t("Latency {ms} milliseconds", { ms: quality.latencyMs })}>
                  {quality.latencyMs} ms
                </span>
                <span
                  aria-label={t("Estimated download {mbps} megabits per second", {
                    mbps: quality.downloadMbps,
                  })}
                >
                  ↓ ~{quality.downloadMbps.toFixed(1)} Mbps
                </span>
                <span
                  aria-label={t("Estimated upload {mbps} megabits per second", {
                    mbps: quality.uploadMbps,
                  })}
                >
                  ↑ ~{quality.uploadMbps.toFixed(1)} Mbps
                </span>
              </>
            ) : qualityFailed ? (
              <span>{t("Test unavailable")}</span>
            ) : null}
          </div>
          <button
            type="button"
            className={settingsSecondaryButton}
            aria-label={t("Test active connection")}
            title={t("Quick latency and speed estimate from the bot computer; about 1.25 MB")}
            disabled={!ready || testing}
            onClick={() => void testConnection()}
          >
            {testing ? t("Testing…") : t("Test")}
          </button>
        </div>
        {sharing?.supported && (
          <label className="bn-share">
            <span>{t("Share this computer’s connection")}</span>
            <input
              type="checkbox"
              aria-label={t("Share this computer’s connection")}
              checked={sharing.sharingAllowed}
              disabled={busy || !sharing.connected}
              onChange={(e) => void share(e.target.checked)}
              className="bn-switch"
            />
          </label>
        )}
        {view && !["ready", "server"].includes(view.runtime.state) && (
          <button
            type="button"
            disabled={busy}
            onClick={() => {
              setBusy(true);
              void api
                .retryBrowserNetwork()
                .then(setView)
                .catch((e) => setError(clientErrorMessage(e, t("Could not retry connection"))))
                .finally(() => setBusy(false));
            }}
            className={`${settingsSecondaryButton} mb-2`}
          >
            {t("Retry connection")}
          </button>
        )}
        {error && (
          <p role="alert" className="bn-error">
            {error}
          </p>
        )}
      </SettingsGroup>
    </section>
  );
}

function NetworkSourceRow({
  id,
  name,
  index,
  active,
  current,
  available,
  disabled,
  onSelect,
}: {
  id: string;
  name: string;
  index: number;
  active: boolean;
  current: boolean;
  available: boolean;
  disabled: boolean;
  onSelect: () => void;
}) {
  const { t } = useI18n();
  const Icon = id === "server" ? Server : /mini|desktop|studio/i.test(name) ? Monitor : Laptop;
  return (
    <li data-network-source={id} className="bn-source">
      <button
        type="button"
        onClick={onSelect}
        disabled={disabled}
        aria-current={active ? "true" : undefined}
        aria-label={active ? t("{name} is in use", { name }) : t("Use {name}", { name })}
        className="bn-source-button"
      >
        <span className="bn-rank">{index + 1}</span>
        <Icon size={16} strokeWidth={1.7} className="bn-icon" />
        <span className="bn-device-label">
          <span className="bn-name">{name}</span>
          {current && <span className="bn-current">{t("(current)")}</span>}
        </span>
        <span
          className={`bn-device-status${available ? " bn-working" : ""}${active ? " bn-in-use" : ""}`}
          aria-label={active ? t("Working · In use") : undefined}
        >
          <span className="bn-status-dot" aria-hidden="true" />
          {active ? t("In use") : available ? t("Working") : t("Not working")}
        </span>
      </button>
    </li>
  );
}
export default BrowserNetworkSettings;
