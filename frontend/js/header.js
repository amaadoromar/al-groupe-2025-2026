import { getUser, logout } from './auth.js';

function buildLinksForRole(role) {
  const L = [];
  if (role === 'ADMIN') {
    L.push({ href: 'admin.html', label: 'Administration' });
    L.push({ href: 'gateway.html', label: 'Passerelle' });
    L.push({ href: 'simulator.html', label: 'Simulateur' });
    L.push({ href: 'dashboard.html', label: 'Tableau de bord' });
    L.push({ href: 'reports.html', label: 'Rapports' });
  } else if (role === 'DOCTEUR') {
    L.push({ href: 'doctor.html', label: 'Médecin' });
    L.push({ href: 'gateway.html', label: 'Passerelle' });
    L.push({ href: 'simulator.html', label: 'Simulateur' });
    L.push({ href: 'nurses.html', label: 'Infirmiers' });
    L.push({ href: 'dashboard.html', label: 'Tableau de bord' });
    L.push({ href: 'reports.html', label: 'Rapports' });
  } else if (role === 'INFIRMIER') {
    L.push({ href: 'nurse.html', label: 'Infirmier' });
    L.push({ href: 'gateway.html', label: 'Passerelle' });
    L.push({ href: 'simulator.html', label: 'Simulateur' });
    L.push({ href: 'dashboard.html', label: 'Tableau de bord' });
    L.push({ href: 'reports.html', label: 'Rapports' });
  } else if (role === 'PATIENT') {
    // No links for patient (only logout shown)
  } else {
    // Unknown/unauthenticated: minimal links
  }
  return L;
}

function renderHeader() {
  const user = getUser();
  const role = user?.role || null;
  const links = buildLinksForRole(role);
  let header = document.querySelector('header.topbar');
  if (!header) {
    header = document.createElement('header');
    header.className = 'topbar';
    document.body.insertBefore(header, document.body.firstChild);
  }
  const brand = '<div class="brand">POC e‑santé</div>';
  const nav = document.createElement('nav');
  nav.className = 'nav';
  links.forEach(l => {
    const a = document.createElement('a');
    a.href = l.href;
    a.textContent = l.label;
    nav.appendChild(a);
  });
  // Logout link on the right
  const logoutLink = document.createElement('a');
  logoutLink.id = 'logout-link';
  logoutLink.href = '#';
  logoutLink.textContent = 'Se déconnecter';
  logoutLink.style.marginLeft = 'auto';
  logoutLink.addEventListener('click', (e) => { e.preventDefault(); logout(); });
  nav.appendChild(logoutLink);

  // Apply active class
  const current = location.pathname.split('/').pop();
  Array.from(nav.querySelectorAll('a')).forEach(a => {
    if (a.getAttribute('href') === current) a.classList.add('active');
  });

  header.setAttribute('data-unified','1');
  window.__UNIFIED_HEADER__ = true;
  header.innerHTML = brand;
  header.appendChild(nav);
}

window.addEventListener('DOMContentLoaded', renderHeader);
