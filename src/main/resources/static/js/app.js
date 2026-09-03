const $ = (id) => document.getElementById(id);
let selectedFile = null;

function fmtNum(v) { return new Intl.NumberFormat().format(Number(v) || 0); }
function fmtBytes(v) {
  let n = Number(v) || 0;
  if (n < 1024) return `${n} B`;
  const units = ['KB', 'MB', 'GB', 'TB'];
  let i = -1;
  do { n /= 1024; i++; } while (n >= 1024 && i < units.length - 1);
  return `${n.toFixed(n >= 100 ? 0 : n >= 10 ? 1 : 2)} ${units[i]}`;
}
function esc(v) { return String(v ?? '').replace(/[&<>"']/g, c => ({ '&':'&amp;', '<':'&lt;', '>':'&gt;', '"':'&quot;', "'":'&#039;' }[c])); }
function setStatus(type, text) {
  const el = $('apiStatus');
  el.className = `connection ${type || ''}`;
  el.querySelector('span').textContent = text;
  $('engineState').textContent = `● ${text}`;
}
function message(text, error = false) {
  $('message').className = `message${error ? ' error' : ''}`;
  $('message').querySelector('span').textContent = text;
}
function choose(file) {
  if (!file) return;
  if (!file.name.toLowerCase().endsWith('.pcap')) {
    message('Please choose a .pcap file. PCAPNG is not supported by the current reader.', true);
    return;
  }
  selectedFile = file;
  $('fileName').textContent = file.name;
  $('fileSize').textContent = `${fmtBytes(file.size)} · Ready to analyze`;
  $('clearBtn').hidden = false;
  $('analyzeBtn').disabled = false;
  message(`${file.name} is ready for manual analysis.`);
}

$('dropZone').addEventListener('click', () => $('fileInput').click());
$('fileInput').addEventListener('change', e => choose(e.target.files[0]));
$('dropZone').addEventListener('dragover', e => { e.preventDefault(); $('dropZone').classList.add('dragging'); });
$('dropZone').addEventListener('dragleave', () => $('dropZone').classList.remove('dragging'));
$('dropZone').addEventListener('drop', e => { e.preventDefault(); $('dropZone').classList.remove('dragging'); choose(e.dataTransfer.files[0]); });
$('clearBtn').addEventListener('click', () => {
  selectedFile = null;
  $('fileInput').value = '';
  $('clearBtn').hidden = true;
  $('analyzeBtn').disabled = true;
  $('fileName').textContent = 'No capture selected';
  $('fileSize').textContent = 'Choose a .pcap file above';
  message('Automatic live capture continues in the background.');
});
$('analyzeBtn').addEventListener('click', analyze);

async function analyze() {
  if (!selectedFile) return;
  setStatus('busy', 'ANALYZING');
  message('Running packet parsing, DPI, flow tracking and alert rules…');
  $('analyzeBtn').disabled = true;
  $('analyzeBtn').querySelector('span').textContent = 'PROCESSING';
  try {
    const fd = new FormData();
    fd.append('file', selectedFile);
    const r = await fetch('/api/analyze-pcap', { method: 'POST', body: fd });
    const text = await r.text();
    let data;
    try { data = JSON.parse(text); } catch { throw new Error(text || 'Invalid server response.'); }
    if (!r.ok) throw new Error(data.message || data.error || 'PCAP analysis failed.');
    render(data);
    message(`Manual analysis complete — ${fmtNum(data.totalPackets)} packets across ${fmtNum(data.flowCount)} flows.`);
    $('results').scrollIntoView({ behavior: 'smooth', block: 'start' });
  } catch (e) {
    console.error(e);
    message(e.message || 'PCAP analysis failed.', true);
  } finally {
    $('analyzeBtn').disabled = !selectedFile;
    $('analyzeBtn').querySelector('span').textContent = 'ANALYZE CAPTURE';
    setStatus('', 'LIVE');
  }
}

function pct(v, total) { return Math.min(100, (Number(v) || 0) / (Number(total) || 1) * 100); }
function render(d) {
  $('totalPackets').textContent = fmtNum(d.totalPackets);
  $('totalBytes').textContent = fmtBytes(d.totalBytes);
  $('flowCount').textContent = fmtNum(d.flowCount);
  $('alertCount').textContent = fmtNum(d.alertCount);
  [['tcpPackets','tcpPct',d.tcpPackets],['udpPackets','udpPct',d.udpPackets],['otherPackets','otherPct',d.otherPackets],['droppedPackets','dropPct',d.droppedPackets]].forEach(([a,b,v]) => { $(a).textContent = fmtNum(v); $(b).textContent = `${pct(v,d.totalPackets).toFixed(1)}%`; });
  $('donutTotal').textContent = fmtNum(d.totalPackets);
  $('donut').style.setProperty('--tcp', `${pct(d.tcpPackets,d.totalPackets)}%`);
  $('donut').style.setProperty('--udp', `${pct(d.udpPackets,d.totalPackets)}%`);
  renderApps(d.applicationCounts || {});
  renderAlerts(d.alerts || []);
  renderFlows(d.flows || {});
  $('alertPill').textContent = `${fmtNum(d.alertCount)} ALERT${Number(d.alertCount) === 1 ? '' : 'S'}`;
  $('flowPill').textContent = `${fmtNum(d.flowCount)} FLOW${Number(d.flowCount) === 1 ? '' : 'S'}`;
  $('alertSummary').textContent = `${fmtNum((d.alerts || []).length)} signal${(d.alerts || []).length === 1 ? '' : 's'} detected`;
}
function renderApps(apps) {
  const entries = Object.entries(apps).sort((a,b) => Number(b[1]) - Number(a[1]));
  $('appCount').textContent = entries.length;
  if (!entries.length) { $('applications').innerHTML = '<div class="empty"><span>◌</span>No application classifications detected.</div>'; return; }
  const max = Math.max(...entries.map(x => Number(x[1]) || 0), 1);
  $('applications').innerHTML = entries.map(([n,c]) => `<div class="app-row"><div><span>${esc(n)}</span><b>${fmtNum(c)}</b></div><div class="track"><i style="width:${Math.max(5, Number(c)/max*100)}%"></i></div></div>`).join('');
}
function renderAlerts(arr) {
  $('alertsBody').innerHTML = arr.length ? arr.map(a => { const s = String(a.severity || 'INFO').toLowerCase(); return `<tr><td><span class="severity ${esc(s)}">${esc(a.severity || 'INFO')}</span></td><td><b>${esc(a.type || 'UNKNOWN')}</b></td><td>${esc(a.message || '—')}</td><td class="mono">${esc(a.sourceIp || '—')}</td><td class="mono">${esc(a.destinationIp || '—')}</td></tr>`; }).join('') : '<tr><td colspan="5"><div class="empty">No security alerts detected in this capture.</div></td></tr>';
}
function renderFlows(flows) {
  let arr = Array.isArray(flows) ? flows : Object.values(flows || {});
  arr.sort((a,b) => Number(b.packetCount || 0) - Number(a.packetCount || 0));
  $('flowsBody').innerHTML = arr.length ? arr.map(f => `<tr><td><span class="protocol-chip">${esc(f.protocol || '—')}</span></td><td class="mono">${esc(f.sourceIp || '—')}${f.sourcePort != null ? ':' + esc(f.sourcePort) : ''}</td><td class="mono">${esc(f.destinationIp || '—')}${f.destinationPort != null ? ':' + esc(f.destinationPort) : ''}</td><td><b>${fmtNum(f.packetCount)}</b></td><td>${fmtBytes(f.byteCount)}</td></tr>`).join('') : '<tr><td colspan="5"><div class="empty">No network flows detected.</div></td></tr>';
}

function renderLiveResponse(payload) {
  const live = payload?.data;
  const state = payload?.status || 'OFFLINE';
  const label = state === 'LIVE' ? `LIVE · ${payload.interface || 'CAPTURE'}` : state;
  setStatus(state === 'LIVE' ? '' : state === 'ERROR' ? 'error' : 'busy', label);
  const hero = document.querySelector('.hero-tag');
  if (hero) hero.innerHTML = `<i></i> LIVE CAPTURE · ${esc(payload.interface || 'AUTO')}`;
  if (live && Number(live.totalPackets) >= 0) render(live);
  if (state === 'ERROR') message(payload.message || 'Automatic live capture needs attention.', true);
  else if (live && Number(live.totalPackets) > 0) message(`Automatic capture active — ${fmtNum(live.totalPackets)} packets in the latest window.`);
  else message(payload.message || 'Waiting for live packets…');
}

async function pollLive() {
  try {
    const r = await fetch('/api/live', { cache: 'no-store' });
    if (!r.ok) throw new Error('Live capture endpoint unavailable');
    renderLiveResponse(await r.json());
  } catch (e) {
    setStatus('error', 'OFFLINE');
    message('Live capture status is unavailable. Manual PCAP analysis can still be used.', true);
  }
}

$('apiStatus').querySelector('span').textContent = 'STARTING';
$('engineState').textContent = '● STARTING';
pollLive();
setInterval(pollLive, 2500);

document.querySelectorAll('.nav').forEach(btn => btn.addEventListener('click', () => {
  document.querySelectorAll('.nav').forEach(x => x.classList.remove('active'));
  btn.classList.add('active');
  document.getElementById(btn.dataset.target)?.scrollIntoView({ behavior: 'smooth' });
}));
