import { apiFetch } from './auth.js';
const $ = (s) => document.querySelector(s);

async function loadPatients() {
  const res = await apiFetch('/api/patients');
  if (!res.ok) throw new Error('Chargement des patients échoué');
  const list = await res.json();
  const sel = $('#nr-patient'); sel.innerHTML = '';
  list.forEach(p => {
    const o = document.createElement('option');
    o.value = p.id; o.textContent = `${p.prenom} ${p.nom} (${p.email})`;
    sel.appendChild(o);
  });
  if (list.length) { sel.value = String(list[0].id); }
}

function getSelectedPatientId() {
  return parseInt($('#nr-patient').value, 10);
}

async function loadForm() {
  const pid = getSelectedPatientId();
  if (!pid) return;
  const res = await apiFetch(`/api/patients/form?patientId=${pid}`);
  if (!res.ok) return;
  const data = await res.json();
  try { fromModel(JSON.parse(data.form || '{}')); } catch { fromModel({}); }
}

async function loadObservations() {
  const pid = getSelectedPatientId();
  if (!pid) return;
  const res = await apiFetch(`/api/observations?patientId=${pid}`);
  if (!res.ok) return;
  const list = await res.json();
  const ul = $('#obs-list'); ul.innerHTML = '';
  if (!list.length) { ul.innerHTML = '<li>Aucune observation pour le moment</li>'; return; }
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

async function addObservation() {
  const pid = getSelectedPatientId();
  const content = $('#obs-content').value.trim();
  if (!pid || !content) return;
  const type = ($('#obs-type').value || 'NOTE');
  const res = await apiFetch('/api/observations', { method: 'POST', body: JSON.stringify({ patientId: pid, content, type }) });
  if (res.status === 201) { $('#obs-content').value = ''; await loadObservations(); }
}

async function saveForm() {
  const pid = getSelectedPatientId();
  if (!pid) return;
  const form = JSON.stringify(toModel());
  await apiFetch(`/api/patients/form?patientId=${pid}`, { method: 'PUT', body: JSON.stringify({ form }) });
}

async function init() {
  await loadPatients();
  $('#btn-load-form').addEventListener('click', loadForm);
  $('#btn-load-obs').addEventListener('click', loadObservations);
  $('#btn-add-obs').addEventListener('click', addObservation);
  $('#btn-save-form').addEventListener('click', saveForm);
  $('#obs-content').addEventListener('keydown', (e) => { if (e.key === 'Enter' && e.ctrlKey) { e.preventDefault(); addObservation(); } });
  $('#nr-patient').addEventListener('change', () => { loadForm(); loadObservations(); });
  try { await loadForm(); await loadObservations(); } catch {}
}

window.addEventListener('DOMContentLoaded', init);

function toModel(){
  return {
    fumeur: $('#nf-fumeur')?.value || 'NON',
    alcool: $('#nf-alcool')?.value || 'NON',
    activite: $('#nf-activite')?.value || 'PEU',
    tailleCm: $('#nf-taille')?.value ? parseInt($('#nf-taille').value, 10) : null,
    poidsKg: $('#nf-poids')?.value ? parseFloat($('#nf-poids').value) : null,
    douleur: $('#nf-douleur')?.value ? parseInt($('#nf-douleur').value, 10) : 0,
    symptomes: $('#nf-symptomes')?.value?.trim() || '',
    medicaments: $('#nf-medicaments')?.value?.trim() || '',
    allergies: $('#nf-allergies')?.value?.trim() || '',
    antecedents: $('#nf-antecedents')?.value?.trim() || ''
  };
}

function fromModel(m){
  if (!m) return;
  if ($('#nf-fumeur')) $('#nf-fumeur').value = m.fumeur || 'NON';
  if ($('#nf-alcool')) $('#nf-alcool').value = m.alcool || 'NON';
  if ($('#nf-activite')) $('#nf-activite').value = m.activite || 'PEU';
  if ($('#nf-taille')) $('#nf-taille').value = m.tailleCm ?? '';
  if ($('#nf-poids')) $('#nf-poids').value = m.poidsKg ?? '';
  if ($('#nf-douleur')) $('#nf-douleur').value = m.douleur ?? 0;
  if ($('#nf-symptomes')) $('#nf-symptomes').value = m.symptomes || '';
  if ($('#nf-medicaments')) $('#nf-medicaments').value = m.medicaments || '';
  if ($('#nf-allergies')) $('#nf-allergies').value = m.allergies || '';
  if ($('#nf-antecedents')) $('#nf-antecedents').value = m.antecedents || '';
}

