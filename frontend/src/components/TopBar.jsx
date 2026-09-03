export default function TopBar({ status }) {
  const statusClass =
    status === "analyzing" ? "busy" : status === "error" ? "error" : "";

  const statusLabel =
    status === "analyzing"
      ? "Analyzing"
      : status === "error"
        ? "API unavailable"
        : status === "success"
          ? "API connected"
          : "API ready";

  return (
    <header className="topbar">
      <div>
        <h1>Packet Analysis Dashboard</h1>
        <p className="subtitle">
          Upload a PCAP file to inspect traffic, applications, flows and
          security alerts.
        </p>
      </div>

      <div className={`api-status ${statusClass}`}>
        <span className={`status-dot ${statusClass}`} />
        {statusLabel}
      </div>
    </header>
  );
}
