import { useEffect, useMemo, useRef, useState } from "react";
import { analyzePcap, checkHealth } from "./api";
import { formatBytes, formatNumber, normalizeApplications, normalizeFlows, percentage } from "./utils/format";

const nav = [
  { id: "overview", label: "Overview", icon: "⌂" },
  { id: "protocols", label: "Protocols", icon: "◈" },
  { id: "flows", label: "Network Flows", icon: "⇄" },
  { id: "alerts", label: "Security Alerts", icon: "⚠" },
];

function Icon({ children }) { return <span className="icon" aria-hidden="true">{children}</span>; }

function App() {
  const inputRef = useRef(null);
  const [file, setFile] = useState(null);
  const [result, setResult] = useState(null);
  const [status, setStatus] = useState("checking");
  const [message, setMessage] = useState("Connecting to the analysis engine…");
  const [dragging, setDragging] = useState(false);
  const [active, setActive] = useState("overview");

  useEffect(() => {
    checkHealth().then(() => setStatus("ready")).catch(() => setStatus("error"));
  }, []);

  const apps = useMemo(() => normalizeApplications(result?.applicationCounts), [result]);
  const flows = useMemo(() => normalizeFlows(result?.flows), [result]);
  const alerts = result?.alerts || [];
  const total = Math.max(Number(result?.totalPackets) || 0, 1);

  function selectFile(candidate) {
    if (!candidate) return;
    if (!candidate.name.toLowerCase().endsWith(".pcap")) {
      setMessage("Please choose a .pcap file. Other formats are not supported.");
      setStatus("error");
      return;
    }
    setFile(candidate);
    setStatus("ready");
    setMessage(`${candidate.name} is ready for analysis.`);
  }

  function onInputChange(e) { selectFile(e.target.files?.[0]); }
  function onDrop(e) {
    e.preventDefault();
    setDragging(false);
    selectFile(e.dataTransfer.files?.[0]);
  }

  async function analyze() {
    if (!file) {
      inputRef.current?.click();
      return;
    }
    setStatus("analyzing");
    setMessage("Running packet parsing, DPI, flow tracking and alert rules…");
    try {
      const data = await analyzePcap(file);
      setResult(data);
      setStatus("success");
      setMessage(`Analysis complete — ${formatNumber(data.totalPackets)} packets processed across ${formatNumber(data.flowCount)} flows.`);
      setActive("overview");
      setTimeout(() => document.getElementById("results")?.scrollIntoView({ behavior: "smooth" }), 60);
    } catch (err) {
      setStatus("error");
      setMessage(err.message || "The backend could not analyze this PCAP file.");
    }
  }

  function clearFile() {
    setFile(null);
    if (inputRef.current) inputRef.current.value = "";
    setStatus("ready");
    setMessage("Select or drop a PCAP capture to begin.");
  }

  function go(id) {
    setActive(id);
    document.getElementById(id)?.scrollIntoView({ behavior: "smooth", block: "start" });
  }

  const statusText = status === "analyzing" ? "ANALYZING" : status === "error" ? "OFFLINE" : status === "checking" ? "CONNECTING" : "ONLINE";

  return (
    <div className="app-shell">
      <div className="ambient ambient-a" /><div className="ambient ambient-b" />
      <aside className="sidebar">
        <div className="brand">
          <div className="brand-cube"><span>PA</span></div>
          <div><b>PACKET<span>LAB</span></b><small>NETWORK INTELLIGENCE</small></div>
        </div>
        <div className="side-label">WORKSPACE</div>
        <nav>{nav.map(item => <button key={item.id} className={active === item.id ? "nav active" : "nav"} onClick={() => go(item.id)}><Icon>{item.icon}</Icon>{item.label}</button>)}</nav>
        <div className="engine-card">
          <div className="engine-orb"><span /></div>
          <div><small>ANALYSIS ENGINE</small><strong>Java DPI Core</strong></div>
          <div className={`engine-state ${status}`}><i />{statusText}</div>
        </div>
        <div className="side-footer">SPRING BOOT · JAVA 21<br />PCAP TRAFFIC ANALYZER</div>
      </aside>

      <main className="main">
        <header className="topbar">
          <div><div className="eyebrow">NETWORK SECURITY / LIVE WORKSPACE</div><h1>Packet Intelligence <span>Dashboard</span></h1><p>Turn raw PCAP captures into traffic intelligence, application insights, network flows and security signals.</p></div>
          <div className={`connection ${status}`}><i /> {statusText}<span>API</span></div>
        </header>

        <section className="hero-grid" id="overview">
          <div className="hero-copy">
            <div className="hero-tag"><span /> PCAP ANALYSIS WORKSPACE</div>
            <h2>See what is happening<br /><em>inside your traffic.</em></h2>
            <p>Upload a capture and let the Java DPI engine decode packets, classify applications, track conversations and surface low-level anomalies.</p>
            <div className="hero-metrics"><div><b>01</b><span>UPLOAD</span></div><div><b>02</b><span>ANALYZE</span></div><div><b>03</b><span>INSPECT</span></div></div>
          </div>
          <div className={`drop-zone ${dragging ? "dragging" : ""}`} onDragOver={e => { e.preventDefault(); setDragging(true); }} onDragLeave={() => setDragging(false)} onDrop={onDrop} onClick={() => inputRef.current?.click()}>
            <div className="scan-lines" />
            <div className="upload-cube"><span>↥</span></div>
            <div className="drop-title">DROP PCAP HERE</div>
            <div className="drop-sub">or click to browse your computer</div>
            <div className="format-pill">PCAP FILES <span>•</span> NETWORK CAPTURES</div>
            <input ref={inputRef} type="file" accept=".pcap" hidden onChange={onInputChange} />
          </div>
        </section>

        <section className="selected-row">
          <div className="selected-file">
            <div className="file-icon">PCAP</div>
            <div className="file-meta"><strong>{file ? file.name : "No capture selected"}</strong><span>{file ? `${formatBytes(file.size)} · Ready to analyze` : "Choose a .pcap file above"}</span></div>
            {file && <button className="x-btn" onClick={e => { e.stopPropagation(); clearFile(); }}>×</button>}
          </div>
          <button className="analyze-btn" disabled={status === "analyzing"} onClick={analyze}><span>{status === "analyzing" ? "PROCESSING" : "ANALYZE CAPTURE"}</span><b>→</b></button>
        </section>

        <div className={`message ${status === "error" ? "error" : ""}`}><i />{message}</div>

        <section className="stats-grid" id="results">
          <Stat icon="⌁" label="TOTAL PACKETS" value={result ? formatNumber(result.totalPackets) : "—"} note="Captured frames" />
          <Stat icon="◌" label="TRAFFIC VOLUME" value={result ? formatBytes(result.totalBytes) : "—"} note="Total payload size" />
          <Stat icon="⇄" label="NETWORK FLOWS" value={result ? formatNumber(result.flowCount) : "—"} note="Tracked conversations" />
          <Stat icon="⚠" label="SECURITY ALERTS" value={result ? formatNumber(result.alertCount) : "—"} note="Rule-based signals" danger />
        </section>

        <section className="section-head" id="protocols"><div><span>01 / TRAFFIC COMPOSITION</span><h2>Protocol intelligence</h2></div><p>Packet-level breakdown from the capture.</p></section>
        <section className="dashboard-grid">
          <article className="glass panel protocol-card">
            <PanelHead title="Packet Types" sub="PROTOCOL BREAKDOWN" />
            <div className="protocol-layout">
              <div className="donut" style={{"--tcp": result ? `${percentage(result.tcpPackets, total)}%` : "0%", "--udp": result ? `${percentage(result.udpPackets, total)}%` : "0%"}}><div><b>{result ? formatNumber(result.totalPackets) : "—"}</b><span>PACKETS</span></div></div>
              <div className="legend-list">{[["TCP",result?.tcpPackets,"tcp"],["UDP",result?.udpPackets,"udp"],["OTHER",result?.otherPackets,"other"],["DROPPED",result?.droppedPackets,"drop"]].map(([name,val,cls]) => <div className="legend-row" key={name}><span><i className={cls} />{name}</span><b>{result ? formatNumber(val) : "—"}</b><small>{result ? `${percentage(val,total).toFixed(1)}%` : ""}</small></div>)}</div>
            </div>
          </article>
          <article className="glass panel"><PanelHead title="Application Detection" sub="DEEP PACKET INSPECTION" /><div className="app-list">{apps.length ? apps.map(([name,count]) => <div className="app-row" key={name}><div><span>{name}</span><b>{formatNumber(count)}</b></div><div className="track"><i style={{width:`${Math.max(5, (Number(count)/Math.max(...apps.map(x=>Number(x[1])||0),1))*100)}%`}} /></div></div>) : <Empty text={result ? "No application classifications detected." : "Run an analysis to populate application intelligence."} />}</div></article>
        </section>

        <section className="section-head" id="alerts"><div><span>02 / SECURITY SIGNALS</span><h2>Alert center</h2></div><p>{result ? `${formatNumber(alerts.length)} signal${alerts.length === 1 ? "" : "s"} detected` : "Waiting for analysis"}</p></section>
        <article className="glass panel table-panel">
          <PanelHead title="Security Alerts" sub="RULE ENGINE OUTPUT" badge={`${result?.alertCount || 0} ALERTS`} danger />
          <div className="table-wrap"><table><thead><tr><th>SEVERITY</th><th>TYPE</th><th>MESSAGE</th><th>SOURCE</th><th>DESTINATION</th></tr></thead><tbody>{alerts.length ? alerts.map((a,i)=><tr key={i}><td><span className={`severity ${String(a.severity||"INFO").toLowerCase()}`}>{a.severity || "INFO"}</span></td><td><b>{a.type || "UNKNOWN"}</b></td><td>{a.message || "—"}</td><td className="mono">{a.sourceIp || "—"}</td><td className="mono">{a.destinationIp || "—"}</td></tr>) : <tr><td colSpan="5"><Empty text={result ? "No security alerts detected in this capture." : "Analyze a PCAP to see security signals."} /></td></tr>}</tbody></table></div>
        </article>

        <section className="section-head" id="flows"><div><span>03 / CONNECTION MAP</span><h2>Network flows</h2></div><p>Endpoint conversations reconstructed from packets.</p></section>
        <article className="glass panel table-panel"><PanelHead title="Tracked Conversations" sub="FLOW TRACKER" badge={`${result?.flowCount || 0} FLOWS`} /><div className="table-wrap"><table><thead><tr><th>PROTOCOL</th><th>SOURCE</th><th>DESTINATION</th><th>PACKETS</th><th>BYTES</th></tr></thead><tbody>{flows.length ? flows.map((f,i)=><tr key={i}><td><span className="protocol-chip">{f.protocol || "—"}</span></td><td className="mono">{f.sourceIp || "—"}{f.sourcePort != null ? `:${f.sourcePort}` : ""}</td><td className="mono">{f.destinationIp || "—"}{f.destinationPort != null ? `:${f.destinationPort}` : ""}</td><td><b>{formatNumber(f.packetCount)}</b></td><td>{formatBytes(f.byteCount)}</td></tr>) : <tr><td colSpan="5"><Empty text={result ? "No network flows detected." : "Analyze a PCAP to reconstruct conversations."} /></td></tr>}</tbody></table></div></article>

        <footer><div><b>PACKET<span>LAB</span></b><small>NETWORK INTELLIGENCE PLATFORM</small></div><span>SPRING BOOT · JAVA DPI · REACT</span></footer>
      </main>
    </div>
  );
}

function Stat({icon,label,value,note,danger}) { return <article className={`stat ${danger ? "danger" : ""}`}><div className="stat-icon">{icon}</div><div><span>{label}</span><strong>{value}</strong><small>{note}</small></div><div className="depth" /></article>; }
function PanelHead({title,sub,badge,danger}) { return <div className="panel-head"><div><span>{sub}</span><h3>{title}</h3></div>{badge && <b className={danger ? "danger-badge" : "badge"}>{badge}</b>}</div>; }
function Empty({text}) { return <div className="empty"><span>◌</span>{text}</div>; }

export default App;
