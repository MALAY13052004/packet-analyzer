import { formatNumber, normalizeApplications } from "../utils/format";

export default function ApplicationsPanel({ data }) {
  const entries = normalizeApplications(data?.applicationCounts);
  const max = Math.max(...entries.map(([, count]) => Number(count) || 0), 1);

  return (
    <article className="panel">
      <div className="panel-header">
        <div>
          <h2>Applications</h2>
          <p className="panel-hint">Deep packet inspection</p>
        </div>
        <span className="count-pill">{entries.length}</span>
      </div>

      {entries.length === 0 ? (
        <div className="application-list empty-state">
          {data
            ? "No application classifications were detected."
            : "Run an analysis to see detected applications."}
        </div>
      ) : (
        <div className="application-list">
          {entries.map(([name, count]) => {
            const pct = ((Number(count) || 0) / max) * 100;
            return (
              <div className="application-row" key={name}>
                <span className="application-name" title={name}>
                  {name}
                </span>
                <div className="application-track">
                  <span style={{ width: `${pct}%` }} />
                </div>
                <span className="application-value">
                  {formatNumber(count)}
                </span>
              </div>
            );
          })}
        </div>
      )}
    </article>
  );
}
