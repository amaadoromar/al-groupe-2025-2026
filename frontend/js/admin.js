import { toast } from './common.js';
import { ensureAuth, getUser, apiFetch, logout } from './auth.js';

const $ = (s) => document.querySelector(s);

async function fetchUsers(role) {
  const url = role ? `/api/users?role=${encodeURIComponent(role)}` : '/api/users';
  const res = await apiFetch(url);
  if (!res.ok) throw new Error('Erreur chargement utilisateurs');
  return res.json();
}

async function populateSelects() {
  const [patients, proches] = await Promise.all([
    fetchUsers('PATIENT'),
    fetchUsers('PROCHE')
  ]);
  const pSel = $('#p-user'); pSel.innerHTML = '';
  patients.forEach(u => {
    const o = document.createElement('option'); o.value = u.id; o.textContent = `${u.prenom} ${u.nom} (${u.email})`;
    pSel.appendChild(o);
  });
  const prSel = $('#pr-user'); prSel.innerHTML = '';
  proches.forEach(u => {
    const o = document.createElement('option'); o.value = u.id; o.textContent = `${u.prenom} ${u.nom} (${u.email})`;
    prSel.appendChild(o);
  });
}

function bindUserForm() {
  $('#user-form').addEventListener('submit', async (e) => {
    e.preventDefault();
    const body = {
      nom: $('#u-nom').value.trim(),
      prenom: $('#u-prenom').value.trim(),
      email: $('#u-email').value.trim(),
      password: $('#u-pass').value,
      role: $('#u-role').value
    };
    try {
      const res = await apiFetch('/api/users', { method: 'POST', body: JSON.stringify(body) });
      if (!res.ok) throw new Error('Création utilisateur échouée');
      toast('Utilisateur créé');
      await populateSelects();
      await refreshUserList();
      e.target.reset();
    } catch (err) { toast(err.message, true); }
  });
}

function bindPatientForm() {
  $('#patient-form').addEventListener('submit', async (e) => {
    e.preventDefault();
    const body = {
      utilisateurId: parseInt($('#p-user').value, 10),
      dateNaissance: $('#p-naissance').value || null,
      sexe: $('#p-sexe').value || null,
      tailleCm: $('#p-taille').value ? parseInt($('#p-taille').value, 10) : null,
      poidsKg: $('#p-poids').value ? parseFloat($('#p-poids').value) : null,
      pathologiePrincipale: $('#p-patho').value || null
    };
    try {
      const res = await apiFetch('/api/patients', { method: 'POST', body: JSON.stringify(body) });
      if (res.status !== 201) throw new Error('Création patient échouée');
      toast('Patient créé');
      e.target.reset();
    } catch (err) { toast(err.message, true); }
  });
}

function bindProcheForm() {
  $('#proche-form').addEventListener('submit', async (e) => {
    e.preventDefault();
    const body = {
      utilisateurId: parseInt($('#pr-user').value, 10),
      patientId: $('#pr-patient-id').value ? parseInt($('#pr-patient-id').value, 10) : null,
      lien: $('#pr-lien').value || null
    };
    try {
      const res = await apiFetch('/api/proches', { method: 'POST', body: JSON.stringify(body) });
      if (res.status !== 201) throw new Error('Création proche échouée');
      toast('Proche créé');
      e.target.reset();
    } catch (err) { toast(err.message, true); }
  });
}

async function refreshUserList() {
  const role = $('#filter-role').value || '';
  const users = await fetchUsers(role || undefined);
  const ul = $('#users-list'); ul.innerHTML = '';
  users.forEach(u => {
    const li = document.createElement('li');
    const id = document.createElement('span'); id.className = 'pill'; id.textContent = u.id;
    const name = document.createElement('div'); name.textContent = `${u.prenom} ${u.nom} — ${u.role}`;
    const email = document.createElement('div'); email.className = 'pill'; email.textContent = u.email;
    li.appendChild(id); li.appendChild(name); li.appendChild(email);
    ul.appendChild(li);
  });
}

function renderWho() {
  const me = getUser();
  const who = $('#who');
  who.innerHTML = `Connecté: <strong>${me.prenom} ${me.nom}</strong> (${me.email}) — Rôle: <strong>${me.role}</strong>
    <button id="btn-logout" class="link" style="float:right">Se déconnecter</button>`;
  $('#btn-logout').addEventListener('click', (e) => { e.preventDefault(); logout(); });
}

async function init() {
  ensureAuth(['ADMIN']);
  renderWho();
  bindUserForm();
  bindPatientForm();
  bindProcheForm();
  $('#filter-role').addEventListener('change', refreshUserList);
  await populateSelects();
  await refreshUserList();
}

window.addEventListener('DOMContentLoaded', init);

