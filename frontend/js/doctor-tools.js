import { qs, toast } from './common.js';
import { getUser, apiFetch } from './auth.js';
import { sendMailHtml } from './notifications.js';

async function fetchPatientEmail(pid) {
  // Try to parse from the select option label as '(email)'
  const sel = qs('#rp-patient');
  const opt = sel?.options[sel.selectedIndex];
  if (opt) {
    const m = /\(([^)]+)\)$/.exec(opt.textContent || '');
    if (m) return m[1];
  }
  // Fallback: call API if available
  try {
    const res = await apiFetch(`/api/patients/${encodeURIComponent(pid)}`);
    if (res.ok) { const p = await res.json(); return p?.email || null; }
  } catch {}
  return null;
}

async function fetchLastReportBase64(pid) {
  try {
    const res = await fetch(`/api/reports/patient/${encodeURIComponent(pid)}`);
    if (!res.ok) return null;
    const items = await res.json();
    if (!Array.isArray(items) || !items.length) return null;
    // assume first item is the latest
    const id = items[0].id;
    const b64Res = await fetch(`/api/reports/${encodeURIComponent(id)}/base64`);
    if (!b64Res.ok) return null;
    return await b64Res.text();
  } catch { return null; }
}

function setupDoctorTools() {
  const user = getUser();
  const box = qs('#doctor-tools');
  if (!box) return;
  box.style.display = (user && user.role === 'DOCTEUR') ? '' : 'none';
  const btn = qs('#rp-send');
  if (!btn) return;
  btn.addEventListener('click', async () => {
    try {
      const u = getUser(); if (!u || u.role !== 'DOCTEUR') return toast('Réservé aux médecins', true);
      const sendEmail = qs('#rp-send-email')?.checked; if (!sendEmail) return toast('Envoi désactivé');
      const pid = qs('#rp-patient')?.value; if (!pid) return toast('Sélectionner un patient');
      const email = await fetchPatientEmail(pid); if (!email) return toast('Email patient introuvable', true);
      const subject = (qs('#rp-subject')?.value || 'Rapport médical').trim();
      const notes = (qs('#rp-notes')?.value || '').trim();
      let html = `<p>Bonjour,</p>`;
      if (notes) html += `<p>${notes.replace(/\n/g,'<br/>')}</p>`;
      html += `<p>Cordialement,<br/>${u.prenom || ''} ${u.nom || ''}</p>`;
      const b64 = await fetchLastReportBase64(pid);
      if (!b64) return toast('Aucun PDF disponible. Générez un rapport d\'abord.', true);
      await sendMailHtml({ to: email, subject, html, attachmentName: 'rapport.pdf', attachmentBase64: b64, attachmentContentType: 'application/pdf' });
      toast('Courriel envoyé au patient');
    } catch { toast('Échec de l\'envoi', true); }
  });
}

window.addEventListener('DOMContentLoaded', setupDoctorTools);

