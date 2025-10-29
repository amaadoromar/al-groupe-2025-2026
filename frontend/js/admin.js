import { qs, storage, uuid, now, toast } from './common.js';

function bindUI() {
  const form = qs('#patient-form');
  form.addEventListener('submit', (e) => {
    e.preventDefault();
    const p = {
      id: uuid(),
      prenom: qs('#prenom').value.trim(),
      nom: qs('#nom').value.trim(),
      naissance: qs('#naissance').value,
      email: qs('#email').value.trim(),
      dossier: qs('#dossier').value.trim(),
      createdAt: now()
    };
    if (!p.prenom || !p.nom || !p.naissance) return toast('Champs requis manquants');
    storage.addPatient(p);
    form.reset();
    toast('Compte patient créé');
    refreshPatients();
  });

  qs('#search').addEventListener('input', refreshPatients);
}

function refreshPatients() {
  const list = storage.getPatients();
  const ul = qs('#patients-list');
  const term = qs('#search').value?.toLowerCase() || '';
  const filtered = list.filter(p => `${p.prenom} ${p.nom} ${p.dossier}`.toLowerCase().includes(term));
  ul.innerHTML = '';
  filtered.forEach(p => {
    const li = document.createElement('li');
    const id = document.createElement('span'); id.className = 'pill'; id.textContent = (p.dossier || p.id).slice(0, 12);
    const name = document.createElement('div'); name.textContent = `${p.prenom} ${p.nom}`;
    const dob = document.createElement('div'); dob.className = 'pill'; dob.textContent = p.naissance;
    li.appendChild(id); li.appendChild(name); li.appendChild(dob);
    ul.appendChild(li);
  });
}

function init() {
  bindUI();
  refreshPatients();
}

window.addEventListener('DOMContentLoaded', init);

