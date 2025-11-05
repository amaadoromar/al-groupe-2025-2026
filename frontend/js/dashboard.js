import { qs, storage, drawChart, drawChartMulti, computeRange, fmtTime, T, toast } from './common.js';
import { apiFetch } from './auth.js';

const state = { dbPatient: '' };

async function fetchSummary(pid, minutes = 60) {
  const res = await fetch(`/api/dashboard/patient/${encodeURIComponent(pid)}/summary?minutes=${minutes}`);
  if (!res.ok) throw new Error('fetch summary failed');
  return res.json();
}

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
    txt.textContent = `${a.msg} ? ${fmtTime(a.t)}`;
    li.appendChild(b); li.appendChild(txt);
    ul.appendChild(li);
  });
}

let _dashTimer = null;
async function renderDashboard() {
  const pid = state.dbPatient;
  if (!pid) return;
  try {
    const summary = await fetchSummary(pid, 60);
    const hrSeries = (summary.seriesHeartRate || []).map(p => p.value);
    const spo2Series = (summary.seriesSpO2 || []).map(p => p.value);
    drawChart(qs('#chart-hr'), hrSeries, T.hr.color, computeRange(hrSeries, T.hr.min, T.hr.max), T.hr.unit);
    drawChart(qs('#chart-spo2'), spo2Series, T.spo2.color, computeRange(spo2Series, T.spo2.min, T.spo2.max), T.spo2.unit);
    const bpSys = (summary.seriesBloodPressureSys || []).map(p => p.value);
    const bpDia = (summary.seriesBloodPressureDia || []).map(p => p.value);
    const gluc = (summary.seriesGlucose || []).map(p => p.value);
    const weight = (summary.seriesWeight || []).map(p => p.value);
    drawChartMulti(qs('#chart-bp'), [bpSys, bpDia], ['#ef4444','#f59e0b'], computeRange(bpSys.concat(bpDia), T.bp.min, T.bp.max), T.bp.unit, 4, ['Systolique','Diastolique']);
    drawChart(qs('#chart-glucose'), gluc, T.glucose.color, computeRange(gluc, T.glucose.min, T.glucose.max), T.glucose.unit);
    drawChart(qs('#chart-weight'), weight, T.weight.color, computeRange(weight, T.weight.min, T.weight.max), T.weight.unit);
    // No temperature chart (not simulated)
    const lastHr = summary.heartRate; qs('#last-hr').textContent = lastHr ? `Dernier: ${lastHr.value} bpm - ${fmtTime(lastHr.time)}` : '';
    const lastSp = summary.spo2; qs('#last-spo2').textContent = lastSp ? `Dernier: ${lastSp.value}% - ${fmtTime(lastSp.time)}` : '';
    // No temperature last value (not simulated)
    const lastBp = (summary.bpSystolic && summary.bpSystolic.value != null) || (summary.bpDiastolic && summary.bpDiastolic.value != null)
      ? `${summary.bpSystolic && summary.bpSystolic.value != null ? summary.bpSystolic.value : '-'} / ${summary.bpDiastolic && summary.bpDiastolic.value != null ? summary.bpDiastolic.value : '-'} mmHg`
      : '-';
    const elBp = qs('#last-bp'); if (elBp) elBp.textContent = lastBp;
    const elG = qs('#last-glucose'); if (elG) elG.textContent = (summary.glucose && summary.glucose.value != null) ? `${summary.glucose.value} mg/dL` : '-';
    const elW = qs('#last-weight'); if (elW) elW.textContent = (summary.weight && summary.weight.value != null) ? `${summary.weight.value} kg` : '-';
    const elS = qs('#last-steps'); if (elS) elS.textContent = (summary.steps && summary.steps.value != null) ? `${summary.steps.value}` : '-';
    const alerts = summary.recentAlerts || [];
    const ul = qs('#alerts'); ul.innerHTML = '';
    alerts.forEach(a => {
      const li = document.createElement('li');
      const b = document.createElement('span'); b.className = 'badge'; b.textContent = (a.typeAlerte || a.type || 'ALERTE');
      const txt = document.createElement('div'); txt.textContent = `${a.message} - ${a.dateCreation || ''}`;
      li.appendChild(b); li.appendChild(txt); ul.appendChild(li);
    });
  } catch (e) {
    const samples = storage.getSamples(pid);
    const hr = samples.map(s => s.hr);
    const spo2 = samples.map(s => s.spo2);
    // No temperature in fallback rendering
    drawChart(qs('#chart-hr'), hr, T.hr.color, [T.hr.min, T.hr.max]);
    drawChart(qs('#chart-spo2'), spo2, T.spo2.color, [T.spo2.min, T.spo2.max]);
    // No temperature chart in fallback
    const last = samples[samples.length - 1];
    qs('#last-hr').textContent = last ? `Dernier: ${last.hr} bpm - ${fmtTime(last.t)}` : '';
    qs('#last-spo2').textContent = last ? `Dernier: ${last.spo2}% - ${fmtTime(last.t)}` : '';
    // No temperature last value in fallback
    const lastBp = last && (last.bpSys != null || last.bpDia != null)
      ? `${last.bpSys != null ? last.bpSys : '-'} / ${last.bpDia != null ? last.bpDia : '-'} mmHg` : '-';
    const elBp = qs('#last-bp'); if (elBp) elBp.textContent = lastBp;
    const elG = qs('#last-glucose'); if (elG) elG.textContent = last && last.glucose != null ? `${last.glucose} mg/dL` : '-';
    const elW = qs('#last-weight'); if (elW) elW.textContent = last && last.weight != null ? `${last.weight} kg` : '-';
    const elS = qs('#last-steps'); if (elS) elS.textContent = last && last.steps != null ? `${last.steps}` : '-';
    renderAlerts();
  }
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
    const res = await apiFetch('/api/patients');
    if (!res.ok) throw new Error('Chargement des patientséchoué');
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
    // Fallback par défaut
    if (!state.dbPatient) { state.dbPatient = '1'; try { sel.value = state.dbPatient; } catch {} }
  }
  bindUI();
  renderDashboard();
  // Refresh charts every second to mimic real-time updates
  if (_dashTimer) clearInterval(_dashTimer);
  _dashTimer = setInterval(() => { try { renderDashboard(); } catch {} }, 1000);
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
window.addEventListener('beforeunload', () => { if (_dashTimer) clearInterval(_dashTimer); });


async function pingMonitoring(){try{const r=await fetch('/api/monitoring/health'); if(r.ok){const j=await r.json(); const el=qs('#mon-status'); if(el) el.textContent = j.status==='ok'?'(Monitoring OK)':'(Monitoring ERR)';}}catch{}} window.addEventListener('load',()=>{ pingMonitoring(); setInterval(pingMonitoring,15000);});

