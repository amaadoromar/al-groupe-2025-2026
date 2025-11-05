let client = null;
let connected = false;
let rateCounter = 0;
let rateWindow = [];
let currentPatient = '';
const lastByType = new Map();

function $(s) { return document.querySelector(s); }

function connect(url) {
  try {
    client = window.mqtt.connect(url, {
      reconnectPeriod: 3000,
      clean: true,
      clientId: 'sim-ui-' + Math.random().toString(16).slice(2)
    });
  } catch (e) {
    $('#mq-state').textContent = 'Erreur connexion';
    return;
  }

  client.on('connect', () => {
    connected = true;
    $('#mq-state').textContent = 'Connecté';
    client.subscribe('esante/patient/+/vitals/#');
    client.subscribe('esante/notifications/patient/+');
  });
  client.on('reconnect', () => $('#mq-state').textContent = 'Reconnexion...');
  client.on('close', () => { connected = false; $('#mq-state').textContent = 'Déconnecté'; });
  client.on('message', onMessage);
}

function disconnect() {
  try { if (client) client.end(true); } catch {}
  client = null; connected = false;
}

function onMessage(topic, payload) {
  rateCounter++;
  if (topic.startsWith('esante/notifications/patient/')) {
    try {
      const msg = JSON.parse(new TextDecoder().decode(payload));
      const li = document.createElement('li');
      li.textContent = `[${new Date().toLocaleTimeString()}] ${msg.alertReason || msg.message || 'ALERT'}`;
      const ul = $('#alerts');
      ul.insertBefore(li, ul.firstChild);
      while (ul.children.length > 10) ul.removeChild(ul.lastChild);
    } catch {}
    return;
  }

  if (!currentPatient) return;
  const parts = topic.split('/');
  // esante/patient/{id}/vitals/{type}
  const pid = parts[2];
  const mtype = parts[4] || '';
  if (pid !== currentPatient) return;
  try {
    const msg = JSON.parse(new TextDecoder().decode(payload));
    lastByType.set(mtype, { value: msg.value, value2: msg.value2, ts: msg.timestamp, unit: msg.unit });
    renderSamples();
  } catch {}
}

function renderSamples() {
  const tbody = $('#samples');
  tbody.innerHTML = '';
  const order = ['HEART_RATE','SPO2','BLOOD_PRESSURE','GLUCOSE','WEIGHT','STEPS'];
  order.forEach(t => {
    const s = lastByType.get(t);
    if (!s) return;
    const tr = document.createElement('tr');
    const name = t === 'HEART_RATE' ? 'Rythme cardiaque' : t === 'SPO2' ? 'SpO2' : t === 'BLOOD_PRESSURE' ? 'Tension' : t === 'GLUCOSE' ? 'Glycémie' : t === 'WEIGHT' ? 'Poids' : 'Pas';
    const val = t === 'BLOOD_PRESSURE' ? `${Math.round(s.value||0)} / ${s.value2 != null ? Math.round(s.value2) : '-' } mmHg` : `${s.value} ${s.unit||''}`;
    tr.innerHTML = `<td>${name}</td><td>${val}</td><td>${new Date(s.ts).toLocaleTimeString()}</td>`;
    tbody.appendChild(tr);
  });
  const hr = lastByType.get('HEART_RATE');
  const spo2 = lastByType.get('SPO2');
  const bp = lastByType.get('BLOOD_PRESSURE');
  $('#last-line').textContent = `HR ${hr?hr.value:'-'} | SpO2 ${spo2?spo2.value:'-'} | BP ${bp?Math.round(bp.value||0):'-'} / ${bp&&bp.value2!=null?Math.round(bp.value2):'-'}`;
}

function tickRate() {
  rateWindow.push(rateCounter);
  if (rateWindow.length > 5) rateWindow.shift();
  const avg5 = rateWindow.reduce((a,b)=>a+b,0) / (rateWindow.length||1);
  $('#rate').textContent = `${rateCounter} msg/s`;
  $('#rate5').textContent = `${avg5.toFixed(1)} msg/s (5s)`;
  rateCounter = 0;
}

function bindUI() {
  $('#mq-toggle').addEventListener('change', () => {
    if ($('#mq-toggle').checked) connect($('#mq-url').value.trim());
    else disconnect();
  });
  $('#bind').addEventListener('click', () => {
    const v = ($('#patient-id').value || '').trim();
    currentPatient = v || '';
    lastByType.clear();
    renderSamples();
  });
}

function init() {
  bindUI();
  if ($('#mq-toggle').checked) connect($('#mq-url').value.trim());
  setInterval(tickRate, 1000);
}

window.addEventListener('DOMContentLoaded', init);

