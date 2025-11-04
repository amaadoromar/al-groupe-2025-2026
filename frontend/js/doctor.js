import { apiFetch } from './auth.js';
const $ = (s) => document.querySelector(s);

async function loadPatients() {
  const res = await apiFetch('/api/users?role=PATIENT');
  if (!res.ok) throw new Error('Chargement patients (utilisateurs) échoué');
  const list = await res.json();
  const sel = $('#doc-patient'); sel.innerHTML = '';
  list.forEach(u => {
    const o = document.createElement('option');
    o.value = u.id; o.textContent = `${u.prenom} ${u.nom} (${u.email})`;
    sel.appendChild(o);
  });
  if (list.length) { sel.value = String(list[0].id); }
}

async function ensurePatientId() {
  const userId = parseInt($('#doc-patient').value, 10);
  const res = await apiFetch(`/api/patients/by-user/${userId}/ensure`, { method: 'POST' });
  if (!res.ok) throw new Error('Impossible d’assurer le patient');
  const data = await res.json();
  return data.patientId;
}

async function loadForm() {
  const pid = await ensurePatientId();
  const res = await apiFetch(`/api/patients/${pid}/form`);
  if (!res.ok) { $('#patient-form').textContent = 'Erreur de chargement'; return; }
  const data = await res.json();
  $('#patient-form').value = data.form || '{}';
}

async function loadObservations() {
  const pid = await ensurePatientId();
  const res = await apiFetch(`/api/observations?patientId=${pid}`);
  if (!res.ok) return;
  const list = await res.json();
  const ul = $('#obs-list'); ul.innerHTML = '';
  list.forEach(o => {
    const li = document.createElement('li');
    const id = document.createElement('span'); id.className = 'pill'; id.textContent = o.id;
    const txt = document.createElement('div'); txt.textContent = `${o.content} — ${o.createdAt}`;
    li.appendChild(id); li.appendChild(txt);
    ul.appendChild(li);
  });
}

async function addObservation() {
  const pid = await ensurePatientId();
  const content = $('#obs-content').value.trim();
  if (!content) return;
  const type = ($('#obs-type').value || 'NOTE');
  const res = await apiFetch('/api/observations', { method: 'POST', body: JSON.stringify({ patientId: pid, content, type }) });
  if (res.status === 201) { $('#obs-content').value = ''; await loadObservations(); }
}

async function saveForm() {
  const pid = await ensurePatientId();
  const form = $('#patient-form').value;
  await apiFetch(`/api/patients/${pid}/form`, { method: 'PUT', body: JSON.stringify({ form }) });
}

async function init() {
  await loadPatients();
  $('#btn-load-form').addEventListener('click', loadForm);
  $('#btn-load-obs').addEventListener('click', loadObservations);
  $('#btn-add-obs').addEventListener('click', addObservation);
  $('#btn-save-form').addEventListener('click', saveForm);
  $('#doc-patient').addEventListener('change', () => { loadForm(); loadObservations(); });
  try { await loadForm(); await loadObservations(); } catch {}
}

window.addEventListener('DOMContentLoaded', init);
