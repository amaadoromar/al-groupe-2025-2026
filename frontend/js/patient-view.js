import { apiFetch } from './auth.js';
const $ = (s) => document.querySelector(s);

function getUserId() {
  const p = new URLSearchParams(location.search);
  return parseInt(p.get('userId') || '0', 10);
}

async function ensurePatientId(userId) {
  const res = await apiFetch(`/api/patients/by-user/${userId}/ensure`, { method: 'POST' });
  if (!res.ok) throw new Error('Impossible d’assurer le patient');
  const data = await res.json();
  return data.patientId;
}

async function loadForm(pid) {
  const res = await apiFetch(`/api/patients/${pid}/form`);
  if (!res.ok) return;
  const data = await res.json();
  $('#form-json').value = data.form || '{}';
}

async function saveForm(pid) {
  const form = $('#form-json').value;
  await apiFetch(`/api/patients/${pid}/form`, { method: 'PUT', body: JSON.stringify({ form }) });
}

async function loadObs(pid) {
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

async function addObs(pid) {
  const content = $('#obs-content').value.trim();
  if (!content) return;
  const type = ($('#obs-type').value || 'NOTE');
  const res = await apiFetch('/api/observations', { method: 'POST', body: JSON.stringify({ patientId: pid, content, type }) });
  if (res.status === 201) { $('#obs-content').value = ''; await loadObs(pid); }
}

async function init() {
  const userId = getUserId();
  if (!userId) { location.href = 'patients.html'; return; }
  const pid = await ensurePatientId(userId);
  await loadForm(pid);
  await loadObs(pid);
  $('#btn-save').addEventListener('click', () => saveForm(pid));
  $('#btn-add-obs').addEventListener('click', () => addObs(pid));
}

window.addEventListener('DOMContentLoaded', init);
