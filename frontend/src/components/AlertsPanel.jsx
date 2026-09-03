import { formatNumber } from "../utils/format";

export default function AlertsPanel({ data }) {
  const alerts = data?.alerts || [];
  const alertCount = Number(data?.alertCount) || 0;

  return (
    <section className="panel" id="alerts">
      <div className="panel-header">
        <div>
          <h2>Security Alerts</h2>
          <p className="panel-hint">Threat detection</p>
        </div>
        <span className="count-pill danger">
          {formatNumber(alertCount)} alert{alertCount === 1 ? "" : "s"}
        </span>
      </div>

      <div className="table-wrap">
        <table>
          <thead>
            <tr>
              <th>Severity</th>
              <th>Type</th>
              <th>Message</th>
              <th>Source</th>
              <th>Destination</th>
            </tr>
          </thead>
          <tbody>
            {alerts.length === 0 ? (
              <tr>
                <td colSpan={5} className="empty-table">
                  {data
                    ? "No security alerts detected."
                    : "No alerts to display."}
                </td>
              </tr>
            ) : (
              alerts.map((alert, index) => {
                const severity = String(
                  alert.severity || "INFO"
                ).toUpperCase();
                return (
                  <tr key={index}>
                    <td>
                      <span className={`severity ${severity}`}>
                        {severity}
                      </span>
                    </td>
                    <td>
                      <strong>{alert.type || "Unknown"}</strong>
                    </td>
                    <td>{alert.message || "—"}</td>
                    <td>{alert.sourceIp || "—"}</td>
                    <td>{alert.destinationIp || "—"}</td>
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
