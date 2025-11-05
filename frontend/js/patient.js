import { toast } from './common.js';
import { getUser, apiFetch } from './auth.js';
import { sendEmailNotification } from './notifications.js';

function loadHistory(key){
  try { return JSON.parse(localStorage.getItem(`pt.hist.${key}`) || '[]'); } catch { return []; }
}
function saveHistory(key, list){
  localStorage.setItem(`pt.hist.${key}`, JSON.stringify(list.slice(-20)));
}
function renderHistory(key){
  const list = loadHistory(key);
  const ul = document.getElementById('pt-history'); if (!ul) return;
  ul.innerHTML = '';
  if (!list.length) { ul.innerHTML = '<li>Aucune alerte</li>'; return; }
  list.slice().reverse().forEach(it => {
    const li = document.createElement('li');
    const t = document.createElement('div'); t.textContent = new Date(it.t).toLocaleString();
    const s = document.createElement('span'); s.className = 'badge ' + (it.status==='ENVOYEE'?'ok':'warn'); s.textContent = it.status;
    const msg = document.createElement('div'); msg.textContent = it.subject || 'Alerte danger';
    li.appendChild(s); li.appendChild(msg); li.appendChild(t);
    ul.appendChild(li);
  });
}

async function triggerDanger() {
  try {
    const user = getUser() || {};
    let authorEmail = user.email || '';
    if (!authorEmail) {
      try { const me = await apiFetch('/api/auth/me'); if (me.ok) { const m = await me.json(); authorEmail = m.sub || ''; } } catch {}
    }
    const when = new Date().toLocaleString();
    const subject = `Alerte DANGER — ${when}`;
    const message = [
      'Une alerte DANGER a été déclenchée par un patient.',
      `Date/heure: ${when}`,
      `Patient: ${user.prenom || ''} ${user.nom || ''} (${authorEmail || ''})`,
      '',
      'Veuillez prendre contact dès que possible.'
    ].join('\n');

    const emailBase = localStorage.getItem('notifEmailBaseUrl') || 'http://localhost:8083';
    // Send exactly as before: only to/subject/message
    await sendEmailNotification({ to: 'elaji.walid02@gmail.com', subject, message, authorEmail }, { baseUrl: emailBase });

    const statusEl = document.getElementById('pt-status');
    if (statusEl) statusEl.textContent = 'Votre alerte a été prise en compte.';

    // Try server history by authorEmail; fallback to local storage
    const histKey = (authorEmail || 'anon').toLowerCase();
    try {
      const base = localStorage.getItem('notifEmailBaseUrl') || 'http://localhost:8083';
      const author = encodeURIComponent(authorEmail || '');
      const res = await fetch(base.replace(/\/$/, '') + `/api/notifications/email/history?authorEmail=${author}&limit=20`);
      if (res.ok) {
        const list = await res.json();
        const ul = document.getElementById('pt-history');
        if (ul) {
          ul.innerHTML = '';
          if (!list.length) { ul.innerHTML = '<li>Aucune alerte</li>'; }
          list.forEach(it => {
            const li = document.createElement('li');
            const t = document.createElement('div'); t.textContent = new Date(it.timestamp).toLocaleString();
            const s = document.createElement('span'); s.className = 'badge ok'; s.textContent = 'ENVOYEE';
            const msg = document.createElement('div'); msg.textContent = it.subject || 'Alerte danger';
            li.appendChild(s); li.appendChild(msg); li.appendChild(t);
            ul.appendChild(li);
          });
        }
      } else { throw new Error('history'); }
    } catch {
      const hist = loadHistory(histKey);
      hist.push({ t: Date.now(), status: 'ENVOYEE', subject });
      saveHistory(histKey, hist);
      renderHistory(histKey);
    }

    toast('Alerte envoyée au médecin');
  } catch (e) {
    toast("Échec de l'envoi de l’alerte", true);
  }
}

window.addEventListener('DOMContentLoaded', async () => {
  const btn = document.getElementById('pt-danger');
  if (btn) btn.addEventListener('click', triggerDanger);
  try {
    let authorEmail = (getUser()||{}).email || '';
    if (!authorEmail) { const me = await apiFetch('/api/auth/me'); if (me.ok) { const m = await me.json(); authorEmail = m.sub || ''; } }
    const key = (authorEmail || 'anon').toLowerCase();
    try {
      const base = localStorage.getItem('notifEmailBaseUrl') || 'http://localhost:8083';
      const author = encodeURIComponent(authorEmail || '');
      const res = await fetch(base.replace(/\/$/, '') + `/api/notifications/email/history?authorEmail=${author}&limit=20`);
      if (res.ok) {
        const list = await res.json();
        const ul = document.getElementById('pt-history');
        if (ul) {
          ul.innerHTML = '';
          if (!list.length) { ul.innerHTML = '<li>Aucune alerte</li>'; }
          list.forEach(it => {
            const li = document.createElement('li');
            const t = document.createElement('div'); t.textContent = new Date(it.timestamp).toLocaleString();
            const s = document.createElement('span'); s.className = 'badge ok'; s.textContent = 'ENVOYEE';
            const msg = document.createElement('div'); msg.textContent = it.subject || 'Alerte danger';
            li.appendChild(s); li.appendChild(msg); li.appendChild(t);
            ul.appendChild(li);
          });
        }
        return;
      }
    } catch {}
    renderHistory(key);
  } catch {}
});
