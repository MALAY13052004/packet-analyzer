export function formatNumber(value) {
  return new Intl.NumberFormat().format(Number(value) || 0);
}

export function formatBytes(bytes) {
  let number = Number(bytes) || 0;

  if (number < 1024) {
    return `${number} B`;
  }

  const units = ["KB", "MB", "GB", "TB"];
  let index = -1;

  do {
    number /= 1024;
    index++;
  } while (number >= 1024 && index < units.length - 1);

  const decimals = number >= 100 ? 0 : number >= 10 ? 1 : 2;

  return `${number.toFixed(decimals)} ${units[index]}`;
}

export function percentage(value, total) {
  const numberValue = Number(value) || 0;
  const numberTotal = Number(total) || 1;
  return Math.min(100, (numberValue / numberTotal) * 100);
}

// The API returns `flows` as either a JSON object keyed by flow id or
// an array — normalize to an array sorted by packet count.
export function normalizeFlows(flows) {
  let entries = [];

  if (Array.isArray(flows)) {
    entries = flows;
  } else if (flows && typeof flows === "object") {
    entries = Object.values(flows);
  }

  return [...entries].sort(
    (a, b) => Number(b.packetCount || 0) - Number(a.packetCount || 0)
  );
}

export function normalizeApplications(apps) {
  return Object.entries(apps || {}).sort(
    (a, b) => Number(b[1]) - Number(a[1])
  );
}
