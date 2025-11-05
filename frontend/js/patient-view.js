import { apiFetch } from './auth.js';
const $ = (s) => document.querySelector(s);

function getUserId() {
  const p = new URLSearchParams(location.search);
  return parseInt(p.get('userId') || '0', 10);
}

function getPatientIdFromQuery() {
  const p = new URLSearchParams(location.search);
  return parseInt(p.get('patientId') || '0', 10);
}

async function ensurePatientId(userId) {
  const res = await apiFetch(`/api/patients/by-user/${userId}/ensure`, { method: 'POST' });
  if (!res.ok) throw new Error('Impossible d’assurer le patient');
  const data = await res.json();
  return data.patientId;
}

function toModel(){
  return {
    fumeur: $('#vf-fumeur')?.value || 'NON',
    alcool: $('#vf-alcool')?.value || 'NON',
    activite: $('#vf-activite')?.value || 'PEU',
    tailleCm: $('#vf-taille')?.value ? parseInt($('#vf-taille').value, 10) : null,
    poidsKg: $('#vf-poids')?.value ? parseFloat($('#vf-poids').value) : null,
    douleur: $('#vf-douleur')?.value ? parseInt($('#vf-douleur').value, 10) : 0,
    symptomes: $('#vf-symptomes')?.value?.trim() || '',
    medicaments: $('#vf-medicaments')?.value?.trim() || '',
    allergies: $('#vf-allergies')?.value?.trim() || '',
    antecedents: $('#vf-antecedents')?.value?.trim() || ''
  };
}

function fromModel(m){
  if (!m) return;
  if ($('#vf-fumeur')) $('#vf-fumeur').value = m.fumeur || 'NON';
  if ($('#vf-alcool')) $('#vf-alcool').value = m.alcool || 'NON';
  if ($('#vf-activite')) $('#vf-activite').value = m.activite || 'PEU';
  if ($('#vf-taille')) $('#vf-taille').value = m.tailleCm ?? '';
  if ($('#vf-poids')) $('#vf-poids').value = m.poidsKg ?? '';
  if ($('#vf-douleur')) $('#vf-douleur').value = m.douleur ?? 0;
  if ($('#vf-symptomes')) $('#vf-symptomes').value = m.symptomes || '';
  if ($('#vf-medicaments')) $('#vf-medicaments').value = m.medicaments || '';
  if ($('#vf-allergies')) $('#vf-allergies').value = m.allergies || '';
  if ($('#vf-antecedents')) $('#vf-antecedents').value = m.antecedents || '';
}

async function loadForm(pid) {
  const res = await apiFetch(`/api/patients/form?patientId=${pid}`);
  if (!res.ok) return;
  const data = await res.json();
  try { fromModel(JSON.parse(data.form || '{}')); } catch { fromModel({}); }
}

async function saveForm(pid) {
  const form = JSON.stringify(toModel());
  await apiFetch(`/api/patients/form?patientId=${pid}`, { method: 'PUT', body: JSON.stringify({ form }) });
}

async function loadObs(pid) {
  const res = await apiFetch(`/api/observations?patientId=${pid}`);
  if (!res.ok) return;
  const list = await res.json();
  const ul = $('#obs-list'); ul.innerHTML = '';
  list.forEach(o => {
    const li = document.createElement('li');
    const id = document.createElement('span'); id.className = 'pill'; id.textContent = o.id;
    const type = document.createElement('span'); type.className = 'badge ' + (o.type === 'QUESTION' ? 'warn' : 'ok'); type.textContent = o.type || 'NOTE';
    const txt = document.createElement('div'); txt.textContent = `${o.content}`;
    const when = document.createElement('div'); when.className = 'latest'; when.textContent = new Date(o.createdAt).toLocaleString();
    li.appendChild(id); li.appendChild(type); li.appendChild(txt); li.appendChild(when);
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
  let pid = getPatientIdFromQuery();
  if (!pid) {
    const userId = getUserId();
    if (!userId) { location.href = 'patients.html'; return; }
    try {
      pid = await ensurePatientId(userId);
    } catch (e) {
      alert("Impossible de charger ce patient. Assurez-vous qu'il existe dans la base.");
      location.href = 'patients.html';
      return;
    }
  }
  await loadForm(pid);
  await loadObs(pid);
  $('#btn-save').addEventListener('click', () => saveForm(pid));
  $('#btn-add-obs').addEventListener('click', () => addObs(pid));
  $('#obs-content').addEventListener('keydown', (e) => { if (e.key === 'Enter' && e.ctrlKey) { e.preventDefault(); addObs(pid); } });
}

window.addEventListener('DOMContentLoaded', init);
