import { apiFetch } from './auth.js';

function $id(id){ return document.getElementById(id); }

function toModel(){
  return {
    fumeur: $id('pf-fumeur').value || 'NON',
    alcool: $id('pf-alcool').value || 'NON',
    activite: $id('pf-activite').value || 'PEU',
    tailleCm: $id('pf-taille').value ? parseInt($id('pf-taille').value, 10) : null,
    poidsKg: $id('pf-poids').value ? parseFloat($id('pf-poids').value) : null,
    douleur: $id('pf-douleur').value ? parseInt($id('pf-douleur').value, 10) : 0,
    symptomes: $id('pf-symptomes').value.trim(),
    medicaments: $id('pf-medicaments').value.trim(),
    allergies: $id('pf-allergies').value.trim(),
    antecedents: $id('pf-antecedents').value.trim()
  };
}

function fromModel(m){
  if (!m) return;
  $id('pf-fumeur').value = m.fumeur || 'NON';
  $id('pf-alcool').value = m.alcool || 'NON';
  $id('pf-activite').value = m.activite || 'PEU';
  $id('pf-taille').value = m.tailleCm ?? '';
  $id('pf-poids').value = m.poidsKg ?? '';
  $id('pf-douleur').value = m.douleur ?? 0;
  $id('pf-symptomes').value = m.symptomes || '';
  $id('pf-medicaments').value = m.medicaments || '';
  $id('pf-allergies').value = m.allergies || '';
  $id('pf-antecedents').value = m.antecedents || '';
}

async function loadForm() {
  const meRes = await apiFetch('/api/auth/me');
  if (!meRes.ok) return;
  const me = await meRes.json();
  const pid = me.patientId;
  if (!pid) { return; }
  const res = await apiFetch(`/api/patients/${pid}/form`);
  if (!res.ok) return;
  const data = await res.json();
  try { fromModel(JSON.parse(data.form || '{}')); } catch { fromModel({}); }
  document.body.dataset.pid = pid;
}

async function saveForm() {
  const pid = document.body.dataset.pid;
  if (!pid) return;
  const val = JSON.stringify(toModel());
  await apiFetch(`/api/patients/${pid}/form`, { method: 'PUT', body: JSON.stringify({ form: val }) });
}

window.addEventListener('DOMContentLoaded', () => {
  loadForm();
  $id('btn-save').addEventListener('click', saveForm);
});
