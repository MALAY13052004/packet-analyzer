import { useRef } from "react";
import { formatBytes } from "../utils/format";

export default function UploadCard({
  selectedFile,
  onSelectFile,
  onClearFile,
  onAnalyze,
  isAnalyzing
}) {
  const fileInputRef = useRef(null);

  function handleChange(event) {
    const file = event.target.files[0];
    if (file) {
      onSelectFile(file);
    }
  }

  return (
    <section className="upload-card" id="dashboard">
      <div className="upload-icon">↑</div>

      <div className="upload-copy">
        <h2>Analyze a PCAP</h2>
        <p>Choose a .pcap capture file and run it through the DPI engine.</p>
      </div>

      <input
        ref={fileInputRef}
        type="file"
        accept=".pcap"
        hidden
        onChange={handleChange}
      />

      <button
        type="button"
        className="button secondary"
        onClick={() => fileInputRef.current?.click()}
      >
        Choose PCAP
      </button>

      <button
        type="button"
        className="button primary"
        disabled={!selectedFile || isAnalyzing}
        onClick={onAnalyze}
      >
        {isAnalyzing ? "Analyzing…" : "Analyze"}
      </button>

      {selectedFile && (
        <div className="selected-file" style={{ width: "100%" }}>
          <span className="file-badge">PCAP</span>
          <span className="file-name">{selectedFile.name}</span>
          <span className="file-size">{formatBytes(selectedFile.size)}</span>
          <button
            type="button"
            className="clear-btn"
            title="Remove file"
            onClick={() => {
              if (fileInputRef.current) {
                fileInputRef.current.value = "";
              }
              onClearFile();
            }}
          >
            ×
          </button>
        </div>
      )}
    </section>
  );
}
