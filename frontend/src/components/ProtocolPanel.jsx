import { formatNumber, percentage } from "../utils/format";

const ROWS = [
  { key: "tcpPackets", label: "TCP", cls: "tcp" },
  { key: "udpPackets", label: "UDP", cls: "udp" },
  { key: "otherPackets", label: "Other", cls: "other" },
  { key: "droppedPackets", label: "Dropped", cls: "dropped" }
];

export default function ProtocolPanel({ data }) {
  const total = Math.max(Number(data?.totalPackets) || 0, 1);

  return (
    <article className="panel">
      <div className="panel-header">
        <div>
          <h2>Packet Types</h2>
          <p className="panel-hint">Protocol breakdown</p>
        </div>
      </div>

      <div className="protocol-list">
        {ROWS.map((row) => {
          const value = data?.[row.key];
          return (
            <div key={row.key}>
              <div className="protocol-row">
                <span>
                  <i className={`legend ${row.cls}`} />
                  {row.label}
                </span>
                <strong>{data ? formatNumber(value) : "—"}</strong>
              </div>
              <div className="bar">
                <span
                  className={`bar-fill ${row.cls}`}
                  style={{ width: `${data ? percentage(value, total) : 0}%` }}
                />
              </div>
            </div>
          );
        })}
      </div>
    </article>
  );
}
