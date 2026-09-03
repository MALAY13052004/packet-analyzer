import { formatNumber, formatBytes } from "../utils/format";

export default function StatsGrid({ data }) {
  const totalPackets = data?.totalPackets;
  const totalBytes = data?.totalBytes;
  const flowCount = data?.flowCount;
  const alertCount = data?.alertCount;

  return (
    <section className="stats-grid">
      <article className="stat-card">
        <span className="stat-label">Total Packets</span>
        <strong>{data ? formatNumber(totalPackets) : "—"}</strong>
        <span className="stat-hint">Captured packets</span>
      </article>

      <article className="stat-card">
        <span className="stat-label">Total Bytes</span>
        <strong>{data ? formatBytes(totalBytes) : "—"}</strong>
        <span className="stat-hint">Traffic volume</span>
      </article>

      <article className="stat-card">
        <span className="stat-label">Network Flows</span>
        <strong>{data ? formatNumber(flowCount) : "—"}</strong>
        <span className="stat-hint">Tracked conversations</span>
      </article>

      <article className="stat-card alert-stat">
        <span className="stat-label">Security Alerts</span>
        <strong>{data ? formatNumber(alertCount) : "—"}</strong>
        <span className="stat-hint">Detected by rules</span>
      </article>
    </section>
  );
}
