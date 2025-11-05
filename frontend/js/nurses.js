import { apiFetch } from './auth.js';
const $ = (s) => document.querySelector(s);

function render(list) {
  const ul = $('#nurses-list');
  const q = ($('#search').value || '').toLowerCase();
  ul.innerHTML = '';
  list.filter(u => (`${u.prenom} ${u.nom} ${u.email}`).toLowerCase().includes(q)).forEach(u => {
    const li = document.createElement('li');
    const id = document.createElement('span'); id.className = 'pill'; id.textContent = u.id;
    const name = document.createElement('div'); name.textContent = `${u.prenom} ${u.nom} (${u.email})`;
    const role = document.createElement('span'); role.className = 'badge ok'; role.textContent = u.role;
    li.appendChild(id); li.appendChild(name); li.appendChild(role);
    ul.appendChild(li);
  });
}

async function init() {
  try {
    const res = await apiFetch('/api/users?role=INFIRMIER');
    if (!res.ok) throw new Error('Erreur chargement');
    const list = await res.json();
    render(list);
    $('#search').addEventListener('input', () => render(list));
  } catch (e) {
    const ul = $('#nurses-list'); ul.innerHTML = '<li>Impossible de charger la liste</li>';
  }
}

window.addEventListener('DOMContentLoaded', init);

