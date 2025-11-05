// Lightweight MQTT client wrapper for browser (mqtt.js via CDN)
// Exposes connect/disconnect/subscribePatientVitals helpers.

let client = null;
let currentSub = null;

export function isMqttAvailable() {
  return !!(window.mqtt && window.mqtt.connect);
}

export function isMqttConnected() {
  return !!(client && client.connected);
}

export function connectMqtt(url) {
  if (!isMqttAvailable()) {
    console.warn('mqtt.js not loaded. Include https://unpkg.com/mqtt/dist/mqtt.min.js');
    return;
  }
  if (client && client.connected) return client;
  try {
    client = window.mqtt.connect(url, {
      reconnectPeriod: 3000,
      clean: true,
      clientId: 'esante-web-' + Math.random().toString(16).slice(2)
    });
    return client;
  } catch (e) {
    console.warn('MQTT connect failed', e);
    client = null;
  }
}

export function disconnectMqtt() {
  try {
    if (client) client.end(true);
  } catch {}
  client = null;
  currentSub = null;
}

export function subscribePatientVitals(patientId, onSample) {
  if (!client) return () => {};
  const topic = `esante/patient/${patientId}/vitals/+`;
  if (currentSub) try { client.unsubscribe(currentSub); } catch {}
  currentSub = topic;

  const state = { hr: undefined, spo2: undefined, temp: undefined, bpSys: undefined, bpDia: undefined, glucose: undefined, weight: undefined, steps: undefined };

  const handler = (t, payload) => {
    try {
      const msg = JSON.parse(new TextDecoder().decode(payload));
      const type = (msg.measurementType || '').toUpperCase();
      if (type === 'HEART_RATE') state.hr = +msg.value;
      else if (type === 'SPO2') state.spo2 = +msg.value;
      else if (type === 'TEMPERATURE' || type === 'TEMP' || type === 'BODY_TEMP') state.temp = +msg.value;
      else if (type === 'BLOOD_PRESSURE') { state.bpSys = +msg.value; if (msg.value2 != null) state.bpDia = +msg.value2; }
      else if (type === 'GLUCOSE') state.glucose = +msg.value;
      else if (type === 'WEIGHT') state.weight = +msg.value;
      else if (type === 'STEPS') state.steps = +msg.value;
      const sample = { t: Date.now() };
      if (state.hr != null) sample.hr = +(+state.hr).toFixed(1);
      if (state.spo2 != null) sample.spo2 = +(+state.spo2).toFixed(1);
      if (state.temp != null) sample.temp = +(+state.temp).toFixed(2);
      if (state.bpSys != null) sample.bpSys = +(+state.bpSys).toFixed(0);
      if (state.bpDia != null) sample.bpDia = +(+state.bpDia).toFixed(0);
      if (state.glucose != null) sample.glucose = +(+state.glucose).toFixed(0);
      if (state.weight != null) sample.weight = +(+state.weight).toFixed(1);
      if (state.steps != null) sample.steps = +(+state.steps).toFixed(0);
      onSample(sample, msg);
    } catch (e) {
      // ignore malformed
    }
  };

  client.subscribe(topic);
  client.on('message', handler);

  return () => {
    try { client.removeListener('message', handler); } catch {}
    try { client.unsubscribe(topic); } catch {}
  };
}
