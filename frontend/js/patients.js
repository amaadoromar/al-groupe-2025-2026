import { apiFetch } from './auth.js';
const $ = (s) => document.querySelector(s);

function render(list) {
  const ul = $('#patients-list');
  const q = ($('#search').value || '').toLowerCase();
  ul.innerHTML = '';
  list.filter(p => (`${p.prenom} ${p.nom} ${p.email}`).toLowerCase().includes(q)).forEach(p => {
    const li = document.createElement('li');
    const id = document.createElement('span'); id.className = 'pill'; id.textContent = p.id;
    const name = document.createElement('div'); name.textContent = `${p.prenom} ${p.nom} (${p.email})`;
    const link = document.createElement('a'); link.href = `patient-view.html?patientId=${p.id}`; link.textContent = 'Consulter';
    li.appendChild(id); li.appendChild(name); li.appendChild(link);
    ul.appendChild(li);
  });
}

async function init() {
  try {
    const res = await apiFetch('/api/patients');
    if (!res.ok) throw new Error('Erreur chargement');
    const list = await res.json();
    render(list);
    $('#search').addEventListener('input', () => render(list));
  } catch (e) {
    const ul = $('#patients-list'); ul.innerHTML = '<li>Impossible de charger la liste</li>';
  }
}

window.addEventListener('DOMContentLoaded', init);
