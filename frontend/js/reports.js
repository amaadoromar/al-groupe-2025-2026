import { qs, storage, now, min, max, mean, fmtTime } from './common.js';
import { apiFetch } from './auth.js';

const state = { rpPatient: '' };

function generateReport(pid, minutes) {
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
  qs('#report-preview').innerHTML = html;
}

function bindUI() {
  qs('#rp-patient').addEventListener('change', (e) => { state.rpPatient = e.target.value; });
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
  bindUI();
}

window.addEventListener('DOMContentLoaded', () => { init(); });
