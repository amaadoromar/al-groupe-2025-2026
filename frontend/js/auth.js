// Simple auth helper: stores token + user info and wraps fetch

const KEY = 'id.auth';
const KEY_BASE = 'id.baseUrl';

export function setBaseUrl(url) {
  if (url) localStorage.setItem(KEY_BASE, url.replace(/\/$/, ''));
}

export function getBaseUrl() {
  const saved = localStorage.getItem(KEY_BASE);
  return (saved && saved.trim()) ? saved : 'http://localhost:8084';
}

export function saveAuth(data) {
  localStorage.setItem(KEY, JSON.stringify(data));
}

export function clearAuth() {
  localStorage.removeItem(KEY);
}

export function getAuth() {
  const raw = localStorage.getItem(KEY);
  if (!raw) return null;
  try { return JSON.parse(raw); } catch { return null; }
}

export function getToken() {
  return getAuth()?.accessToken || null;
}

export function getUser() {
  const a = getAuth();
  if (!a) return null;
  return {
    id: a.userId,
    email: a.email,
    nom: a.nom,
    prenom: a.prenom,
    role: a.role,
    roles: a.roles || (a.role ? [a.role] : [])
  };
}

export function logout() {
  clearAuth();
  window.location.href = 'login.html';
}

export async function apiFetch(path, opts = {}) {
  const token = getToken();
  const headers = Object.assign({ 'Content-Type': 'application/json' }, opts.headers || {});
  if (token) headers['Authorization'] = `Bearer ${token}`;
  const url = /^https?:\/\//.test(path) ? path : `${getBaseUrl()}${path}`;
  const res = await fetch(url, { ...opts, headers });
  if (res.status === 401) { logout(); return Promise.reject(new Error('Unauthorized')); }
  return res;
}

export function redirectByRole(role) {
  switch (role) {
    case 'ADMIN':
      return 'admin.html';
    case 'DOCTEUR':
    case 'INFIRMIER':
    case 'PATIENT':
    case 'PROCHE':
      return 'dashboard.html';
    default:
      return 'dashboard.html';
  }
}

export function applyRoleNav() {
  const user = getUser();
  const nav = document.querySelector('.nav');
  if (!nav || !user) return;
  const role = user.role;
  nav.querySelectorAll('a').forEach(a => {
    const href = a.getAttribute('href');
    if (href === 'admin.html' && role !== 'ADMIN') a.style.display = 'none';
    if (href === 'doctor.html' && !(role === 'DOCTEUR' || role === 'INFIRMIER' || role === 'ADMIN')) a.style.display = 'none';
    if (href === 'patient-form.html') a.style.display = 'none';
  });
}

export function ensureAuth(allowedRoles) {
  const user = getUser();
  if (!user) { window.location.href = 'login.html'; return; }
  if (Array.isArray(allowedRoles) && allowedRoles.length && !allowedRoles.includes(user.role)) {
    window.location.href = redirectByRole(user.role);
    return;
  }
  applyRoleNav();
}
