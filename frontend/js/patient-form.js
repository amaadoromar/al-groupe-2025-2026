import { apiFetch } from './auth.js';

async function loadForm() {
  // fetch user to get patientId from /api/auth/me
  const meRes = await apiFetch('/api/auth/me');
  if (!meRes.ok) return;
  const me = await meRes.json();
  const pid = me.patientId;
  if (!pid) { document.getElementById('form-json').value = '{}'; return; }
  const res = await apiFetch(`/api/patients/${pid}/form`);
  if (!res.ok) return;
  const data = await res.json();
  document.getElementById('form-json').value = data.form || '{}';
  document.getElementById('form-json').dataset.pid = pid;
}

async function saveForm() {
  const pid = document.getElementById('form-json').dataset.pid;
  if (!pid) return;
  const val = document.getElementById('form-json').value;
  await apiFetch(`/api/patients/${pid}/form`, { method: 'PUT', body: JSON.stringify({ form: val }) });
}

window.addEventListener('DOMContentLoaded', () => {
  loadForm();
  document.getElementById('btn-save').addEventListener('click', saveForm);
});

