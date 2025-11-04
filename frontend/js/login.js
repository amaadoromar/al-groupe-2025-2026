import { saveAuth, redirectByRole, apiFetch } from './auth.js';

const $ = (s) => document.querySelector(s);

async function login(email, password) {
  const res = await apiFetch('/api/auth/login', {
    method: 'POST',
    body: JSON.stringify({ email, password })
  });
  if (!res.ok) throw new Error('Identifiants invalides');
  return res.json();
}

function init() {
  const form = document.getElementById('login-form');
  const msg = document.getElementById('msg');
  form.addEventListener('submit', async (e) => {
    e.preventDefault();
    msg.textContent = '';
    const email = $('#email').value.trim();
    const password = $('#password').value;
    try {
      const data = await login(email, password);
      saveAuth(data);
      const dest = redirectByRole(data.role);
      window.location.href = dest;
    } catch (err) {
      msg.textContent = 'Erreur: ' + err.message;
    }
  });
}

window.addEventListener('DOMContentLoaded', init);
