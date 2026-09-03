// Relative path so the Vite dev proxy (see vite.config.js) or, in
// production, the same-origin Spring Boot server, handles routing.
const API_BASE = "/api";

export async function checkHealth() {
  const response = await fetch(`${API_BASE}/health`);
  if (!response.ok) {
    throw new Error("Health check failed");
  }
  return response.text();
}

export async function analyzePcap(file) {
  const formData = new FormData();
  formData.append("file", file);

  const response = await fetch(`${API_BASE}/analyze-pcap`, {
    method: "POST",
    body: formData
  });

  const text = await response.text();
  let data;

  try {
    data = JSON.parse(text);
  } catch {
    throw new Error(text || "The server returned an invalid response.");
  }

  if (!response.ok) {
    throw new Error(
      data.message || data.error || text || "PCAP analysis failed."
    );
  }

  return data;
}
