import { sendMailHtml } from './notifications.js';
import { qs, storage, now, min, max, mean, fmtTime, toast } from './common.js';
import { apiFetch, getUser } from './auth.js';

const state = { rpPatient: '', patients: [], lastReportId: null, lastReportBase64: null };

async function generateServerReport(pid, minutes) {
  const url = `/api/reports/generate/custom?patientId=${encodeURIComponent(pid)}&minutes=${encodeURIComponent(minutes)}`;
  const res = await fetch(url, { method: 'POST' });
  if (!res.ok) throw new Error('report generation failed');
  return res.json();
}

async function loadReportBase64(id) {
  const url = `/api/reports/${encodeURIComponent(id)}/base64`;
  const res = await fetch(url);
  if (!res.ok) return null;
  return res.text();
}

function renderLocalReport(pid, minutes) {
  const sel = qs('#rp-patient');
  const label = sel.options[sel.selectedIndex]?.textContent || '';
  const cutoff = now() - minutes * 60 * 1000;
  const data = storage.getSamples(pid).filter(s => s.t >= cutoff);
  const hr = data.map(x => x.hr), spo2 = data.map(x => x.spo2), temp = data.map(x => x.temp);
  const s = (arr) => ({ min: min(arr), max: max(arr), avg: +mean(arr).toFixed(2) });
  const stats = { hr: s(hr), spo2: s(spo2), temp: s(temp) };
  const alerts = storage.getAlerts(pid).filter(a => a.t >= cutoff);
  const risk = alerts.length ? 'Alerte(s) détectée(s)' : 'Rien à signaler';
  const html = `
    <div class="report">
      <h3>Rapport de santé — ${label}</h3>
      <div>Période: ${minutes} min, Généré: ${new Date().toLocaleString()}</div>
      <hr />
      <h4>Résumé</h4>
      <ul>
        <li>Rythme cardiaque (bpm) — min: ${stats.hr.min || '—'}, max: ${stats.hr.max || '—'}, moyenne: ${isFinite(stats.hr.avg)?stats.hr.avg:'—'}</li>
        <li>SpO₂ (%) — min: ${stats.spo2.min || '—'}, max: ${stats.spo2.max || '—'}, moyenne: ${isFinite(stats.spo2.avg)?stats.spo2.avg:'—'}</li>
        <li>Température (°C) — min: ${stats.temp.min || '—'}, max: ${stats.temp.max || '—'}, moyenne: ${isFinite(stats.temp.avg)?stats.temp.avg:'—'}</li>
      </ul>
      <h4>Événements</h4>
      <div>${risk}</div>
      <ul>
        ${alerts.slice(-20).map(a => `<li>${fmtTime(a.t)} — ${a.msg}</li>`).join('')}
      </ul>
      <div style="margin-top:12px"><button onclick="window.print()">Imprimer / Exporter en PDF</button></div>
    </div>`;
  qs('#report-preview').innerHTML = html; state.lastReportBase64 = b64 || null;
}

async function generateReport(pid, minutes) {
  try {
    const report = await generateServerReport(pid, minutes);
    const b64 = await loadReportBase64(report.id);
    const summary = report.summary || '';
    const exportUrl = `/api/reports/${report.id}/export`;
    const html = `
      <div class="report">
        <h3>Rapport g�n�r� (serveur)</h3>
        <div>P�riode: ${minutes} min</div>
        ${summary ? `<pre class="summary">${summary.replace(/</g,'&lt;')}</pre>` : ''}
        <div style="margin:10px 0"><em>Le PDF a �t� g�n�r�.</em></div>
          <a href="${exportUrl}" target="_blank">Ouvrir le PDF</a>
        </div>
        ${b64 ? `<object data="data:application/pdf;base64,${b64}" type="application/pdf" width="100%" height="600px"></object>` : ''}
      </div>`;
    qs('#report-preview').innerHTML = html; state.lastReportBase64 = b64 || null;
  } catch (e) {
    // Fallback to local, in-browser summary if server not reachable
    renderLocalReport(pid, minutes);
  }
}

async function loadHistory(pid) {
  const box = qs('#report-history');
  box.innerHTML = '<div>Chargement…</div>';
  try {
    const res = await fetch(`/api/reports/patient/${encodeURIComponent(pid)}`);
    if (!res.ok) throw new Error('history failed');
    const items = await res.json();
    if (!Array.isArray(items) || !items.length) {
      box.innerHTML = '<div>Aucun rapport trouvé.</div>';
      return;
    }
    const rows = items.map(r => {
      const dt = r.reportDate || r.createdAt || '';
      const type = r.reportType || 'N/A';
      const st = r.status || '';
      const id = r.id;
      const link = `/api/reports/${id}/export`;
      return `<tr><td>${dt}</td><td>${type}</td><td>${st}</td><td><a href="${link}" target="_blank">Ouvrir le PDF</a></td></tr>`;
    }).join('');
    box.innerHTML = `
      <table class="table">
        <thead><tr><th>Date</th><th>Type</th><th>Statut</th><th>PDF</th></tr></thead>
        <tbody>${rows}</tbody>
      </table>`;
  } catch (e) {
    box.innerHTML = '<div>Impossible de charger l\'historique.</div>';
  }
}

function bindUI() {
  qs('#rp-patient').addEventListener('change', (e) => { state.rpPatient = e.target.value; if (state.rpPatient) loadHistory(state.rpPatient); });
  qs('#rp-generate').addEventListener('click', () => {
    if (!state.rpPatient) return;
    const minutes = +qs('#rp-window').value;
    generateReport(state.rpPatient, minutes);
  });
}

async function init() {
  const sel = qs('#rp-patient');
  sel.innerHTML = '';
  try {
    const res = await apiFetch('/api/patients');
    if (!res.ok) throw new Error('load failed');
    const patients = await res.json();
    patients.forEach(p => {
      const o = document.createElement('option');
      o.value = String(p.id);
      o.textContent = `${p.prenom} ${p.nom} (${p.email})`;
      sel.appendChild(o);
    });
    if (patients[0]) { state.rpPatient = String(patients[0].id); sel.value = state.rpPatient; }
  } catch {}
  if (state.rpPatient) { loadHistory(state.rpPatient); }
  bindUI();
}

window.addEventListener('DOMContentLoaded', () => { init(); });






