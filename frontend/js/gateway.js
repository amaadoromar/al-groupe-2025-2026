import { qs, storage, clamp, now, toast } from './common.js';
import { sendRealtimeNotification } from './notifications.js';
import { connectMqtt, disconnectMqtt, isMqttConnected, subscribePatientVitals } from './mqtt-client.js';
import { apiFetch } from './auth.js';

let sim = { timer: null, base: { hr: 76, spo2: 97, temp: 36.8 } };
const state = { gwPatient: '' };
let unsubscribeVitals = null;

function updateGatewayTiles(s) {
  qs('#gw-hr').textContent = s ? `${s.hr} bpm` : '— bpm';
  qs('#gw-spo2').textContent = s ? `${s.spo2} %` : '— %';
  qs('#gw-temp').textContent = s ? `${s.temp} °C` : '— °C';
}

function evaluateAlerts(pid, s) {
  const alerts = [];
  if (s.hr < 45 || s.hr > 110) alerts.push({ type: 'hr', value: s.hr, msg: `Rythme cardiaque ${s.hr} bpm` });
  if (s.spo2 < 90) alerts.push({ type: 'spo2', value: s.spo2, msg: `SpO2 ${s.spo2}%` });
  if (s.temp > 38.5) alerts.push({ type: 'temp', value: s.temp, msg: `Température ${s.temp}°C` });
  alerts.forEach(a => {
    const rec = { t: now(), ...a };
    storage.pushAlert(pid, rec);
    const payload = { type: 'WARNING', title: `Alerte ${a.type.toUpperCase()}`, message: a.msg };
    // Envoyer la notification au backend; l'UI l'affichera via WebSocket
    sendRealtimeNotification(payload).catch(() => {});
  });
}

function simulateStep(randomized) {
  if (!state.gwPatient) return;
  const t = now();
  const b = sim.base;
  if (randomized) {
    b.hr = clamp(b.hr + (Math.random() - 0.5) * 3, 60, 100);
    b.spo2 = clamp(b.spo2 + (Math.random() - 0.5) * 0.6, 93, 99);
    b.temp = clamp(b.temp + (Math.random() - 0.5) * 0.08, 36.4, 37.2);
    if (Math.random() < 0.015) b.hr = 40 + Math.random() * 15;
    if (Math.random() < 0.01) b.spo2 = 84 + Math.random() * 6;
    if (Math.random() < 0.008) b.temp = 38.7 + Math.random() * 0.8;
  }
  const sample = { t, hr: +b.hr.toFixed(1), spo2: +b.spo2.toFixed(1), temp: +b.temp.toFixed(2) };
  storage.pushSample(state.gwPatient, sample);
  updateGatewayTiles(sample);
  evaluateAlerts(state.gwPatient, sample);
}

function forceSpike() {
  sim.base.hr = 140 + Math.random() * 10;
  sim.base.spo2 = 86 + Math.random() * 3;
  sim.base.temp = 39.2 + Math.random() * 0.6;
}

function bindUI() {
  qs('#gw-start').addEventListener('click', () => {
    if (!state.gwPatient) return toast('Sélectionnez un patient');
    if (qs('#gw-mqtt').checked) return toast('Mode MQTT actif');
    clearInterval(sim.timer);
    sim.timer = setInterval(() => simulateStep(qs('#gw-random').checked), 1000);
    toast('Simulation démarrée');
  });
  qs('#gw-stop').addEventListener('click', () => {
    clearInterval(sim.timer); sim.timer = null; toast('Simulation arrêtée');
  });
  qs('#gw-spike').addEventListener('click', () => { forceSpike(); simulateStep(false); });

  qs('#gw-patient').addEventListener('change', (e) => {
    state.gwPatient = e.target.value; updateGatewayTiles(null);
    if (qs('#gw-mqtt').checked) setupMqttSubscription();
  });

  qs('#gw-mqtt').addEventListener('change', () => {
    const checked = qs('#gw-mqtt').checked;
    if (checked) {
      const url = qs('#gw-mqtt-url').value.trim();
      connectMqtt(url);
      const wait = () => {
        if (isMqttConnected()) { toast('Connecté au broker MQTT'); setupMqttSubscription(); }
        else setTimeout(wait, 300);
      };
      wait();
    } else {
      if (unsubscribeVitals) { try { unsubscribeVitals(); } catch {} unsubscribeVitals = null; }
      disconnectMqtt();
      toast('MQTT déconnecté');
    }
  });
}

async function init() {
  const sel = qs('#gw-patient');
  sel.innerHTML = '';
  try {
    const res = await apiFetch('/api/patients');
    if (!res.ok) throw new Error('patients load failed');
    const patients = await res.json();
    patients.forEach(p => {
      const o = document.createElement('option');
      o.value = String(p.id);
      o.textContent = `${p.prenom} ${p.nom} (${p.email})`;
      sel.appendChild(o);
    });
    if (patients[0]) {
      state.gwPatient = String(patients[0].id);
      sel.value = state.gwPatient;
    }
  } catch (e) {
    toast('Impossible de charger les patients', true);
  }
  bindUI();
  updateGatewayTiles(null);
}

//window.addEventListener('DOMContentLoaded', init);

function setupMqttSubscription() {
  if (!state.gwPatient) return;
  if (unsubscribeVitals) { try { unsubscribeVitals(); } catch {} unsubscribeVitals = null; }
  unsubscribeVitals = subscribePatientVitals(state.gwPatient, (sample, raw) => {
    // push to storage if we have at least hr or spo2 or temp
    if (sample.hr == null && sample.spo2 == null && sample.temp == null) return;
    storage.pushSample(state.gwPatient, sample);
    updateGatewayTiles({ hr: sample.hr ?? 0, spo2: sample.spo2 ?? 0, temp: sample.temp ?? 0 });
    // Evaluate alerts based on received sample
    evaluateAlerts(state.gwPatient, {
      hr: sample.hr ?? 76,
      spo2: sample.spo2 ?? 97,
      temp: sample.temp ?? 36.8
    });
  });
}

window.addEventListener('DOMContentLoaded', () => { init(); });
