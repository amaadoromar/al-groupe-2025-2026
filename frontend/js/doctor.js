import { apiFetch } from './auth.js';
const $ = (s) => document.querySelector(s);

const patientEmailById = new Map();

async function loadPatients() {
  const res = await apiFetch('/api/patients');
  if (!res.ok) throw new Error('Chargement des patients échoué');
  const list = await res.json();
  const sel = $('#doc-patient'); sel.innerHTML = '';
  list.forEach(p => {
    const o = document.createElement('option');
    o.value = p.id; o.textContent = `${p.prenom} ${p.nom} (${p.email})`;
    sel.appendChild(o);
    patientEmailById.set(p.id, p.email);
  });
  if (list.length) { sel.value = String(list[0].id); }
}

function getSelectedPatientId(){ return parseInt($('#doc-patient').value, 10); }

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

function getNotifBase(){ return (localStorage.getItem('notifEmailBaseUrl') || 'http://localhost:8083').replace(/\/$/,''); }
async function loadAlertHistory(){
  const pid = getSelectedPatientId(); if (!pid) return;
  const email = patientEmailById.get(pid) || '';
  const ul = $('#alert-history'); if (ul) ul.innerHTML='';
  try{
    const author = encodeURIComponent(email || '');
    const res = await fetch(getNotifBase()+`/api/notifications/email/history?authorEmail=${author}&limit=20`);
    if (!res.ok) throw new Error('history');
    const list = await res.json();
    if (!list.length){ if (ul) ul.innerHTML = '<li>Aucune alerte</li>'; return; }
    list.forEach(it=>{
      const li=document.createElement('li');
      const t=document.createElement('div'); t.textContent=new Date(it.timestamp).toLocaleString();
      const s=document.createElement('span'); s.className='badge ok'; s.textContent='ENVOYEE';
      const msg=document.createElement('div'); msg.textContent=it.subject || 'Alerte patient';
      li.appendChild(s); li.appendChild(msg); li.appendChild(t);
      if (ul) ul.appendChild(li);
    });
  } catch {
    if (ul) ul.innerHTML = '<li>Impossible de charger les alertes</li>';
  }
}

async function init() {
  await loadPatients();
  // Bind create-patient form
  const form = document.getElementById('create-patient-form');
  if (form) {
    form.addEventListener('submit', async (e) => {
      e.preventDefault();
      const body = {
        prenom: document.getElementById('cp-prenom')?.value.trim(),
        nom: document.getElementById('cp-nom')?.value.trim(),
        email: document.getElementById('cp-email')?.value.trim(),
        password: document.getElementById('cp-pass')?.value,
        dateNaissance: document.getElementById('cp-naissance')?.value || null,
        sexe: document.getElementById('cp-sexe')?.value || null,
        tailleCm: document.getElementById('cp-taille')?.value ? parseInt(document.getElementById('cp-taille').value, 10) : null,
        poidsKg: document.getElementById('cp-poids')?.value ? parseFloat(document.getElementById('cp-poids').value) : null
      };
      try {
        const res = await apiFetch('/api/patients/bootstrap', { method: 'POST', body: JSON.stringify(body) });
        if (res.status !== 201) throw new Error('Création patient échouée');
        const data = await res.json();
        await loadPatients();
        const sel = document.getElementById('doc-patient');
        if (sel && data.patientId) { sel.value = String(data.patientId); }
        await loadForm();
        await loadObservations();
        form.reset();
      } catch (err) {
        console.error(err);
        alert('Impossible de créer le patient');
      }
    });
  }
  $('#btn-load-form').addEventListener('click', loadForm);
  $('#btn-load-obs').addEventListener('click', loadObservations);
  $('#btn-add-obs').addEventListener('click', addObservation);
  $('#btn-save-form').addEventListener('click', saveForm);
  $('#btn-load-alerts')?.addEventListener('click', loadAlertHistory);
  $('#obs-content').addEventListener('keydown', (e) => { if (e.key === 'Enter' && e.ctrlKey) { e.preventDefault(); addObservation(); } });
  $('#doc-patient').addEventListener('change', () => { loadForm(); loadObservations(); loadAlertHistory(); });
  window.addEventListener('realtime:notification', () => { loadAlertHistory(); });
  try { await loadForm(); await loadObservations(); } catch {}
  try { await loadAlertHistory(); } catch {}
}

window.addEventListener('DOMContentLoaded', init);

function toModel(){
  return {
    fumeur: $('#df-fumeur')?.value || 'NON',
    alcool: $('#df-alcool')?.value || 'NON',
    activite: $('#df-activite')?.value || 'PEU',
    tailleCm: $('#df-taille')?.value ? parseInt($('#df-taille').value, 10) : null,
    poidsKg: $('#df-poids')?.value ? parseFloat($('#df-poids').value) : null,
    douleur: $('#df-douleur')?.value ? parseInt($('#df-douleur').value, 10) : 0,
    symptomes: $('#df-symptomes')?.value?.trim() || '',
    medicaments: $('#df-medicaments')?.value?.trim() || '',
    allergies: $('#df-allergies')?.value?.trim() || '',
    antecedents: $('#df-antecedents')?.value?.trim() || ''
  };
}

function fromModel(m){
  if (!m) return;
  if ($('#df-fumeur')) $('#df-fumeur').value = m.fumeur || 'NON';
  if ($('#df-alcool')) $('#df-alcool').value = m.alcool || 'NON';
  if ($('#df-activite')) $('#df-activite').value = m.activite || 'PEU';
  if ($('#df-taille')) $('#df-taille').value = m.tailleCm ?? '';
  if ($('#df-poids')) $('#df-poids').value = m.poidsKg ?? '';
  if ($('#df-douleur')) $('#df-douleur').value = m.douleur ?? 0;
  if ($('#df-symptomes')) $('#df-symptomes').value = m.symptomes || '';
  if ($('#df-medicaments')) $('#df-medicaments').value = m.medicaments || '';
  if ($('#df-allergies')) $('#df-allergies').value = m.allergies || '';
  if ($('#df-antecedents')) $('#df-antecedents').value = m.antecedents || '';
}
