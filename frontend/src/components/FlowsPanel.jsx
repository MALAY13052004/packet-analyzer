import { formatNumber, formatBytes, normalizeFlows } from "../utils/format";

export default function FlowsPanel({ data }) {
  const flows = normalizeFlows(data?.flows);
  const flowCount = Number(data?.flowCount) || 0;

  return (
    <section className="panel" id="flows">
      <div className="panel-header">
        <div>
          <h2>Network Flows</h2>
          <p className="panel-hint">Conversations</p>
        </div>
        <span className="count-pill">
          {formatNumber(flowCount)} flow{flowCount === 1 ? "" : "s"}
        </span>
      </div>

      <div className="table-wrap">
        <table>
          <thead>
            <tr>
              <th>Protocol</th>
              <th>Source</th>
              <th>Destination</th>
              <th>Packets</th>
              <th>Bytes</th>
            </tr>
          </thead>
          <tbody>
            {flows.length === 0 ? (
              <tr>
                <td colSpan={5} className="empty-table">
                  {data ? "No flows detected." : "No flows to display."}
                </td>
              </tr>
            ) : (
              flows.map((flow, index) => {
                const sourcePort = flow.sourcePort ?? "";
                const destinationPort = flow.destinationPort ?? "";
                return (
                  <tr key={index}>
                    <td>
                      <strong>{flow.protocol || "—"}</strong>
                    </td>
                    <td>
                      {flow.sourceIp || "—"}
                      {sourcePort !== "" ? `:${sourcePort}` : ""}
                    </td>
                    <td>
                      {flow.destinationIp || "—"}
                      {destinationPort !== "" ? `:${destinationPort}` : ""}
                    </td>
                    <td>{formatNumber(flow.packetCount)}</td>
                    <td>{formatBytes(flow.byteCount)}</td>
                  </tr>
                );
              })
            )}
          </tbody>
        </table>
      </div>
    </section>
  );
}
