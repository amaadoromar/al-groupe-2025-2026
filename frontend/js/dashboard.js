import { qs, storage, drawChart, fmtTime, T, toast } from './common.js';
import { apiFetch } from './auth.js';

const state = { dbPatient: '' };

function renderAlerts() {
  const pid = state.dbPatient;
  const list = storage.getAlerts(pid).slice().reverse();
  const ul = qs('#alerts');
  ul.innerHTML = '';
  list.forEach(a => {
    const li = document.createElement('li');
    const b = document.createElement('span');
    b.className = 'badge ' + (a.type === 'hr' ? (a.value > T.hr.warnHi || a.value < T.hr.warnLo ? 'danger' : 'ok') : a.type === 'spo2' ? (a.value < T.spo2.warnLo ? 'danger' : 'ok') : (a.value > T.temp.warnHi ? 'warn' : 'ok'));
    b.textContent = a.type.toUpperCase();
    const txt = document.createElement('div');
    txt.textContent = `${a.msg} — ${fmtTime(a.t)}`;
    li.appendChild(b); li.appendChild(txt);
    ul.appendChild(li);
  });
}

function renderDashboard() {
  const pid = state.dbPatient;
  if (!pid) return;
  const samples = storage.getSamples(pid);
  const hr = samples.map(s => s.hr);
  const spo2 = samples.map(s => s.spo2);
  const temp = samples.map(s => s.temp);
  const cHR = qs('#chart-hr');
  const cS = qs('#chart-spo2');
  const cT = qs('#chart-temp');
  drawChart(cHR, hr, T.hr.color, [T.hr.min, T.hr.max]);
  drawChart(cS, spo2, T.spo2.color, [T.spo2.min, T.spo2.max]);
  drawChart(cT, temp, T.temp.color, [T.temp.min, T.temp.max]);
  const last = samples[samples.length - 1];
  qs('#last-hr').textContent = last ? `Dernier: ${last.hr} bpm – ${fmtTime(last.t)}` : '';
  qs('#last-spo2').textContent = last ? `Dernier: ${last.spo2}% – ${fmtTime(last.t)}` : '';
  qs('#last-temp').textContent = last ? `Dernier: ${last.temp}°C – ${fmtTime(last.t)}` : '';
  // Extended vitals (if available)
  const lastBp = last && (last.bpSys != null || last.bpDia != null)
    ? `${last.bpSys != null ? last.bpSys : '—'} / ${last.bpDia != null ? last.bpDia : '—'} mmHg`
    : '—';
  const lastGlucose = last && last.glucose != null ? `${last.glucose} mg/dL` : '—';
  const lastWeight = last && last.weight != null ? `${last.weight} kg` : '—';
  const lastSteps = last && last.steps != null ? `${last.steps}` : '—';
  const elBp = qs('#last-bp'); if (elBp) elBp.textContent = lastBp;
  const elG = qs('#last-glucose'); if (elG) elG.textContent = lastGlucose;
  const elW = qs('#last-weight'); if (elW) elW.textContent = lastWeight;
  const elS = qs('#last-steps'); if (elS) elS.textContent = lastSteps;
  renderAlerts();
}

function bindUI() {
  qs('#db-patient').addEventListener('change', (e) => { state.dbPatient = e.target.value; renderDashboard(); });
  qs('#db-clear').addEventListener('click', () => { if (state.dbPatient) { storage.clearSamples(state.dbPatient); renderDashboard(); } });
  qs('#db-notify').addEventListener('click', async () => {
    if (!('Notification' in window)) return toast('Notifications non supportées');
    const perm = await Notification.requestPermission();
    toast(perm === 'granted' ? 'Notifications activées' : 'Notifications refusées');
  });
  qs('#ack-all').addEventListener('click', () => { if (state.dbPatient) { storage.clearAlerts(state.dbPatient); renderAlerts(); } });
}

async function init() {
  const sel = qs('#db-patient');
  sel.innerHTML = '';
  try {
    const res = await apiFetch('/api/users?role=PATIENT');
    if (!res.ok) throw new Error('Chargement des patients échoué');
    const patients = await res.json();
    patients.forEach(u => {
      const o = document.createElement('option');
      o.value = String(u.id);
      o.textContent = `${u.prenom} ${u.nom} (${u.email})`;
      sel.appendChild(o);
    });
    if (patients[0]) {
      state.dbPatient = String(patients[0].id);
      sel.value = state.dbPatient;
    }
  } catch (e) {
    toast('Impossible de charger la liste des patients', true);
  }
  bindUI();
  renderDashboard();
  // Refresh charts when samples/alerts update in other tabs (e.g., gateway via MQTT)
  window.addEventListener('storage', (e) => {
    if (!state.dbPatient) return;
    if (!e.key) return;
    if (e.key === 'samples:' + state.dbPatient || e.key.startsWith('samples:') || e.key.startsWith('alerts:')) {
      renderDashboard();
    }
  });
}

window.addEventListener('DOMContentLoaded', () => { init(); });
