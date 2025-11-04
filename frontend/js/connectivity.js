import { setBaseUrl, getBaseUrl } from './auth.js';
const $ = (s) => document.querySelector(s);
const out = $('#out');
let token = null;

function log(obj) {
  const text = typeof obj === 'string' ? obj : JSON.stringify(obj, null, 2);
  out.textContent = (out.textContent ? out.textContent + '\n' : '') + text;
}

async function hit(path, opts = {}) {
  const val = $('#baseUrl').value.trim();
  const base = (val || window.location.origin).replace(/\/$/, '');
  try { setBaseUrl(base); } catch {}
  const url = base + path;
  const headers = Object.assign({ 'Content-Type': 'application/json' }, opts.headers || {});
  if (token && !headers['Authorization']) headers['Authorization'] = `Bearer ${token}`;
  const res = await fetch(url, { ...opts, headers });
  const text = await res.text();
  let body;
  try { body = JSON.parse(text); } catch { body = text; }
  return { status: res.status, body };
}

$('#btn-health').addEventListener('click', async () => {
  out.textContent = '';
  try {
    const { status, body } = await hit('/actuator/health');
    log({ path: '/actuator/health', status, body });
  } catch (e) { log(String(e)); }
});

$('#btn-login').addEventListener('click', async () => {
  out.textContent = '';
  const email = $('#email').value; const password = $('#password').value;
  try {
    const { status, body } = await hit('/api/auth/login', {
      method: 'POST',
      body: JSON.stringify({ email, password })
    });
    log({ path: '/api/auth/login', status, body });
    if (body && body.accessToken) {
      token = body.accessToken;
      log('Token enregistré en mémoire.');
    }
  } catch (e) { log(String(e)); }
});

$('#btn-me').addEventListener('click', async () => {
  out.textContent = '';
  try {
    const { status, body } = await hit('/api/auth/me');
    log({ path: '/api/auth/me', status, body });
  } catch (e) { log(String(e)); }
});

$('#btn-clear').addEventListener('click', () => { out.textContent = ''; });

window.addEventListener('DOMContentLoaded', () => {
  try {
    const saved = getBaseUrl();
    if (saved) $('#baseUrl').value = saved;
  } catch {}
});
