// Auth + role helpers (UTF-8)

const KEY_AUTH = 'id.auth';
const KEY_BASE = 'id.baseUrl';

export function saveAuth(data) {
  localStorage.setItem(KEY_AUTH, JSON.stringify(data));
}

export function clearAuth() {
  localStorage.removeItem(KEY_AUTH);
}

export function getAuth() {
  const raw = localStorage.getItem(KEY_AUTH);
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

export function setBaseUrl(url) {
  if (url) localStorage.setItem(KEY_BASE, url.replace(/\/$/, ''));
}

export function getBaseUrl() {
  const saved = localStorage.getItem(KEY_BASE);
  return (saved && saved.trim()) ? saved : 'http://localhost:8084';
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
  if (res.status === 401) { logout(); throw new Error('Unauthorized'); }
  return res;
}

export function redirectByRole(role) {
  switch (role) {
    case 'ADMIN': return 'admin.html';
    case 'DOCTEUR':
    case 'INFIRMIER': return 'doctor.html';
    case 'PATIENT':
    case 'PROCHE': return 'dashboard.html';
    default: return 'dashboard.html';
  }
}

function linkAllowed(role, href) {
  if (href === 'admin.html') return role === 'ADMIN';
  if (href === 'doctor.html') return role === 'DOCTEUR';
  if (href === 'gateway.html' || href === 'patients.html') {
    return role === 'DOCTEUR' || role === 'INFIRMIER';
  }
  if (href === 'nurses.html') return role === 'DOCTEUR' || role === 'ADMIN';
  if (href === 'nurse.html') return role === 'INFIRMIER' || role === 'ADMIN';
  return true;
}

function setActiveNav() {
  const nav = document.querySelector('.nav'); if (!nav) return;
  const path = location.pathname.split('/').pop();
  nav.querySelectorAll('a').forEach(a => a.classList.toggle('active', a.getAttribute('href') === path));
}

export function applyRoleNav() {
  const user = getUser();
  const nav = document.querySelector('.nav');
  if (!nav || !user) return;
  // Cleanup accidental literal "`n" artifacts in static HTML
  try { nav.innerHTML = nav.innerHTML.replace(/`n\s*/g, '\n'); } catch {}
  const role = user.role;
  nav.querySelectorAll('a').forEach(a => {
    const href = a.getAttribute('href');
    if (!linkAllowed(role, href)) a.style.display = 'none';
  });
  setActiveNav();
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
