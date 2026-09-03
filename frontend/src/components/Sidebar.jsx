const SECTIONS = [
  { id: "dashboard", label: "Dashboard" },
  { id: "flows", label: "Network Flows" },
  { id: "alerts", label: "Security Alerts" }
];

export default function Sidebar({ activeSection, onNavigate, status }) {
  const dotClass =
    status === "analyzing" ? "busy" : status === "error" ? "error" : "";

  const statusLabel =
    status === "analyzing"
      ? "Analyzing"
      : status === "error"
        ? "API unavailable"
        : "API ready";

  return (
    <aside className="sidebar">
      <div className="brand">
        <div className="brand-mark">PA</div>
        <div>
          <strong>Packet Analyzer</strong>
          <span>Deep Packet Inspection</span>
        </div>
      </div>

      <nav>
        {SECTIONS.map((section) => (
          <button
            key={section.id}
            type="button"
            className={`nav-item${activeSection === section.id ? " active" : ""}`}
            onClick={() => onNavigate(section.id)}
          >
            {section.label}
          </button>
        ))}
      </nav>

      <div className="sidebar-footer">
        <span className={`status-dot ${dotClass}`} />
        {statusLabel}
      </div>
    </aside>
  );
}
