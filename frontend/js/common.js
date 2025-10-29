// Shared utilities, constants and storage across pages

export const T = {
  hr: { key: 'hr', label: 'BPM', color: '#22c55e', min: 30, max: 160, warnLo: 45, warnHi: 110 },
  spo2: { key: 'spo2', label: '%', color: '#38bdf8', min: 70, max: 100, warnLo: 90, warnHi: 100 },
  temp: { key: 'temp', label: '°C', color: '#f59e0b', min: 34, max: 42, warnLo: 36, warnHi: 38.5 }
};

export const qs = (s) => document.querySelector(s);
export const qsa = (s) => Array.from(document.querySelectorAll(s));
export const now = () => Date.now();
const pad2 = (n) => (n < 10 ? '0' + n : '' + n);
export const fmtTime = (t) => {
  const d = new Date(t);
  return `${pad2(d.getHours())}:${pad2(d.getMinutes())}:${pad2(d.getSeconds())}`;
};
export const uuid = () => crypto.getRandomValues(new Uint32Array(4)).join('-');
export const clamp = (v, a, b) => Math.max(a, Math.min(b, v));
export const mean = (arr) => (arr.length ? arr.reduce((s, x) => s + x, 0) / arr.length : 0);
export const min = (arr) => (arr.length ? arr.reduce((m, x) => (x < m ? x : m), arr[0]) : 0);
export const max = (arr) => (arr.length ? arr.reduce((m, x) => (x > m ? x : m), arr[0]) : 0);

export const storage = {
  getPatients() {
    return JSON.parse(localStorage.getItem('patients') || '[]');
  },
  savePatients(list) {
    localStorage.setItem('patients', JSON.stringify(list));
  },
  addPatient(p) {
    const list = this.getPatients();
    list.push(p);
    this.savePatients(list);
  },
  getSamples(pid) {
    return JSON.parse(localStorage.getItem('samples:' + pid) || '[]');
  },
  pushSample(pid, s, cap = 600) {
    const list = this.getSamples(pid);
    list.push(s);
    const pruned = list.slice(-cap);
    localStorage.setItem('samples:' + pid, JSON.stringify(pruned));
  },
  clearSamples(pid) {
    localStorage.removeItem('samples:' + pid);
  },
  getAlerts(pid) {
    return JSON.parse(localStorage.getItem('alerts:' + pid) || '[]');
  },
  pushAlert(pid, a, cap = 200) {
    const list = this.getAlerts(pid);
    list.push(a);
    const pruned = list.slice(-cap);
    localStorage.setItem('alerts:' + pid, JSON.stringify(pruned));
  },
  clearAlerts(pid) {
    localStorage.removeItem('alerts:' + pid);
  }
};

export function drawChart(canvas, series, color, yRange, yTicks = 4) {
  const ctx = canvas.getContext('2d');
  const w = canvas.width, h = canvas.height;
  ctx.clearRect(0, 0, w, h);
  ctx.fillStyle = '#0a1326';
  ctx.fillRect(0, 0, w, h);
  ctx.strokeStyle = '#1f2937';
  for (let i = 0; i <= yTicks; i++) {
    const y = (h - 28) * (i / yTicks) + 8;
    ctx.beginPath(); ctx.moveTo(40, y); ctx.lineTo(w - 8, y); ctx.stroke();
  }
  const minY = yRange[0], maxY = yRange[1];
  const N = series.length;
  if (!N) return;
  const xs = series.map((_, i) => 40 + (i / Math.max(1, N - 1)) * (w - 52));
  const ys = series.map(v => 8 + (1 - (v - minY) / (maxY - minY)) * (h - 36));
  ctx.strokeStyle = color; ctx.lineWidth = 2; ctx.beginPath();
  ctx.moveTo(xs[0], ys[0]);
  for (let i = 1; i < N; i++) ctx.lineTo(xs[i], ys[i]);
  ctx.stroke();
  ctx.fillStyle = '#6b7280'; ctx.font = '12px system-ui, sans-serif';
  ctx.fillText(minY, 8, h - 10); ctx.fillText(maxY, 8, 18);
}

export function toast(msg, crit = false) {
  const el = qs('#toast');
  el.textContent = msg; el.classList.remove('hidden');
  el.classList.toggle('crit', !!crit);
  clearTimeout(el._t);
  el._t = setTimeout(() => el.classList.add('hidden'), 4000);
  if (crit) beep();
}

export function beep() {
  try {
    const ac = new (window.AudioContext || window.webkitAudioContext)();
    const o = ac.createOscillator(); const g = ac.createGain();
    o.type = 'square'; o.frequency.value = 880; o.connect(g); g.connect(ac.destination);
    g.gain.setValueAtTime(0.0001, ac.currentTime); g.gain.exponentialRampToValueAtTime(0.2, ac.currentTime + 0.01);
    o.start(); setTimeout(() => { g.gain.exponentialRampToValueAtTime(0.0001, ac.currentTime + 0.1); o.stop(ac.currentTime + 0.12); }, 160);
  } catch {}
}

export async function notify(title, body) {
  if (!('Notification' in window)) return;
  if (Notification.permission === 'granted') new Notification(title, { body });
}

export function populatePatients(selectElts) {
  const patients = storage.getPatients();
  selectElts.forEach(sel => {
    const prev = sel.value;
    sel.innerHTML = '';
    patients.forEach(p => {
      const o = document.createElement('option');
      o.value = p.id; o.textContent = `${p.prenom} ${p.nom} – ${p.dossier || 'sans dossier'}`;
      sel.appendChild(o);
    });
    if (prev) sel.value = prev;
  });
}

