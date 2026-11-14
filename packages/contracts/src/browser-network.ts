export type {
  BrowserNetworkView,
  NetworkDevice,
  NetworkDnsDiagnostic,
  NetworkSettings,
  NetworkMode,
  NetworkSettingsUpdate,
  NetworkEgress,
  NetworkQuality,
} from "@outcomego/plugin-sdk/browser-network/protocol";
export interface NetworkSharingStatus {
  supported: boolean;
  connected: boolean;
  sharingAllowed: boolean;
  providing: boolean;
  serverUrl: string | null;
  deploymentId: string | null;
  activeConnections: number;
}
