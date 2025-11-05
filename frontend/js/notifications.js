// Notification service integration (WebSocket + REST)
// Requires SockJS and StompJS to be loaded globally via CDN script tags.

import { toast, beep } from './common.js';

// Default WebSocket base (SockJS/STOMP)
// Updated to port 12345 per configuration
const DEFAULT_BASE = 'http://localhost:12345';

export function getNotifBaseUrl() {
  return (
    window.NOTIF_BASE_URL ||
    localStorage.getItem('notifWsBaseUrl') ||
    localStorage.getItem('notifBaseUrl') ||
    DEFAULT_BASE
  );
}

let client = null;
let connected = false;
let retryTimer = null;

export function connectNotifications(opts = {}) {
  const baseUrl = (opts.baseUrl || getNotifBaseUrl()).replace(/\/$/, '');
  if (connected || client) return; // avoid double connections

  if (!window.SockJS) {
    console.warn('SockJS not found. Include sockjs-client CDN before notifications.js');
    return;
  }

  const hasModern = !!(window.StompJs && window.StompJs.Client);
  const hasLegacy = !!window.Stomp;
  if (!hasModern && !hasLegacy) {
    console.warn('STOMP lib not found. Include @stomp/stompjs UMD or stompjs before notifications.js');
    return;
  }

  const sockFactory = () => new window.SockJS(baseUrl + '/ws');

  if (hasModern) {
    const { Stomp } = window.StompJs;
    client = new window.StompJs.Client({
      webSocketFactory: sockFactory,
      reconnectDelay: 3000,
      debug: () => {}
    });
    client.onConnect = () => {
      connected = true;
      client.subscribe('/topic/notifications', (msg) => {
        try {
          const n = JSON.parse(msg.body || '{}');
          displayNotification(n);
        } catch (e) { console.error('Invalid notification payload', e); }
      });
    };
    client.onStompError = () => { connected = false; };
    client.activate();
  } else {
    const sock = sockFactory();
    client = window.Stomp.over(sock);
    client.debug = null; // silence logs
    const onConnect = () => {
      connected = true;
      client.subscribe('/topic/notifications', (msg) => {
        try {
          const n = JSON.parse(msg.body || '{}');
          displayNotification(n);
        } catch (e) { console.error('Invalid notification payload', e); }
      });
    };
    const onError = (err) => {
      connected = false;
      client = null;
      console.warn('Notification socket error, retrying in 3s', err);
      clearTimeout(retryTimer);
      retryTimer = setTimeout(() => connectNotifications({ baseUrl }), 3000);
    };
    client.connect({}, onConnect, onError);
  }
}

export function displayNotification(n) {
  if (!n) return;
  const type = (n.type || 'INFO').toUpperCase();
  const title = n.title || 'Notification';
  const message = n.message || '';
  const text = `${title}: ${message}`;
  const critical = type === 'ERROR' || type === 'WARNING';
  toast(text, critical);
  if (critical) beep();
  try { window.dispatchEvent(new CustomEvent('realtime:notification', { detail: n })); } catch {}
  if ('Notification' in window && Notification.permission === 'granted') {
    try { new Notification(title, { body: message }); } catch {}
  }
}

export async function sendRealtimeNotification(payload, opts = {}) {
  const baseUrl = (opts.baseUrl || getNotifBaseUrl()).replace(/\/$/, '');
  const res = await fetch(baseUrl + '/api/notifications/realtime/send', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload)
  });
  if (!res.ok) throw new Error('Failed to send notification: ' + res.status);
  return res.text();
}

export async function sendTestNotification(opts = {}) {
  const baseUrl = (opts.baseUrl || getNotifBaseUrl()).replace(/\/$/, '');
  const res = await fetch(baseUrl + '/api/notifications/realtime/test');
  if (!res.ok) throw new Error('Failed to trigger test notification: ' + res.status);
  return res.text();
}

export async function sendMailHtml({ to, subject, html, attachmentName, attachmentBase64, attachmentContentType }, opts = {}) {
  const baseUrl = (opts.baseUrl || getNotifBaseUrl()).replace(/\/$/, '');
  const body = { to, subject, message: html, attachmentName, attachmentBase64, attachmentContentType };
  const res = await fetch(baseUrl + '/api/notifications/email/send-html', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body)
  });
  if (!res.ok) throw new Error('Failed to send email: ' + res.status);
  return res.json().catch(() => ({}));
}

// Send an email via notification-service
export async function sendEmailNotification(payload, opts = {}) {
  const baseUrl = (opts.baseUrl || getNotifBaseUrl()).replace(/\/$/, '');
  const { to, subject, message, patientId, authorEmail } = payload || {};
  const res = await fetch(baseUrl + '/api/notifications/email/send', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ to, subject, message, patientId, authorEmail })
  });
  if (!res.ok) throw new Error('Failed to send email: ' + res.status);
  return res.json().catch(() => ({}));
}

// Auto-connect on load
window.addEventListener('DOMContentLoaded', () => {
  try { connectNotifications(); } catch {}
});
