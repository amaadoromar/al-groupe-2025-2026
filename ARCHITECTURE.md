# eSante Data Streaming Architecture

## System Overview

```
┌─────────────────────────────────────────────────────────────────────┐
│                    eSante E2E Data Streaming                         │
└─────────────────────────────────────────────────────────────────────┘

┌──────────────┐     MQTT Topics:                    ┌──────────────┐
│   Simulator  │     esante/patient/+/vitals/#       │   MQTT UI    │
│   (Go 1.21)  │────────────────────────────────────▶│  (Browser)   │
│              │                                      │              │
│ 10 Patients  │            ┌──────────┐             │ Real-time    │
│ 6 Vitals     │            │ Eclipse  │             │ Monitoring   │
│ Battery Data │───────────▶│Mosquitto │◀────────────│              │
└──────────────┘            │  MQTT    │             └──────────────┘
                            │ Broker   │
                            └────┬─────┘
                                 │
                                 │ Subscribe:
                                 │ esante/patient/+/vitals/#
                                 │
                            ┌────▼─────┐
                            │ Telegraf │
                            │ Streaming│
                            │  Layer   │
                            └────┬─────┘
                                 │
                    ┌────────────┴────────────┐
                    │                         │
              Alert Check:              Normal Vitals
              HR > 150 BPM                   │
              Battery < 30%                  │
                    │                         │
                    ▼                         ▼
          ┌─────────────────┐      ┌─────────────────┐
          │ MQTT Notify     │      │   InfluxDB 2.7  │
          │ esante/         │      │   Time-Series   │
          │ notifications/  │      │    Database     │
          │ patient/{id}    │      │                 │
          │                 │      │ Bucket:         │
          │ QoS 1           │      │ patient_vitals  │
          └─────────────────┘      └─────────────────┘
                    │                         │
                    ▼                         ▼
          ┌─────────────────┐      ┌─────────────────┐
          │ Backend Service │      │   Dashboards    │
          │ (Monitoring)    │      │   Analytics     │
          │ Acknowledges    │      │   Reporting     │
          └─────────────────┘      └─────────────────┘
```

## Data Flow Details

### 1. Vital Generation
```
Simulator → MQTT
Topic: esante/patient/{patientId}/vitals/{type}
Interval: 10 seconds (configurable)
QoS: 1 (guaranteed delivery)

Payload:
{
  "patientId": "patient-5",
  "deviceType": "watch",
  "measurementType": "HEART_RATE",
  "value": 165,
  "unit": "BPM",
  "timestamp": "2025-11-05T10:30:00Z",
  "metadata": {
    "battery": 25,
    "deviceId": "device-watch-5",
    "firmware": "v1.2.3",
    "quality": "poor"
  }
}
```

### 2. Telegraf Processing
```
┌─────────────┐
│ MQTT Input  │
│ Consumer    │
└──────┬──────┘
       │
       ▼
┌─────────────┐
│ Parse JSON  │
│ Extract Tags│
└──────┬──────┘
       │
       ▼
┌──────────────────────┐
│ Starlark Processor:  │
│ Check Conditions:    │
│ - HR > 150?          │
│ - Battery < 30?      │
│ Tag: alert=true/false│
└──────┬───────────────┘
       │
       ├─── alert=true ──▶ MQTT Output ──▶ Notifications
       │
       └─── alert=false ─▶ InfluxDB Output ──▶ Storage
```

### 3. Alert Notification
```
Topic: esante/notifications/patient/{patientId}
QoS: 1
Retain: false (ephemeral)

Payload:
{
  "patientId": "patient-5",
  "timestamp": "2025-11-05T10:30:00Z",
  "vitalType": "HEART_RATE",
  "value": 165,
  "unit": "BPM",
  "alertReason": "High heart rate: 165 BPM, Low battery: 25%",
  "deviceId": "device-watch-5",
  "battery": 25
}
```

### 4. InfluxDB Storage
```
Measurement: patient_vitals

Tags:
- patientId
- deviceType
- measurementType
- deviceId

Fields:
- value (float)
- value2 (float, optional for BP)
- battery (int)
- quality (string)

Timestamp: Nanosecond precision
```

## Component Ports

| Service    | Port | Protocol | Purpose                    |
|------------|------|----------|----------------------------|
| Mosquitto  | 1883 | MQTT     | Device connections         |
| Mosquitto  | 9001 | WS       | WebSocket for browsers     |
| MQTT UI    | 8080 | HTTP     | Web monitoring interface   |
| Telegraf   | N/A  | N/A      | Stream processor (no UI)   |
| InfluxDB   | 8086 | HTTP     | Database API               |

## Network Diagram

```
Docker Network: esante_network

┌────────────────────────────────────────────────────────┐
│                                                        │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐           │
│  │Simulator │  │ MQTT UI  │  │ Telegraf │           │
│  └────┬─────┘  └────┬─────┘  └────┬─────┘           │
│       │             │              │                  │
│       └─────────────┼──────────────┘                  │
│                     │                                 │
│              ┌──────▼──────┐                         │
│              │  Mosquitto  │                         │
│              └──────┬──────┘                         │
│                     │                                 │
│              ┌──────▼──────┐                         │
│              │  InfluxDB   │                         │
│              └─────────────┘                         │
│                                                        │
└────────────────────────────────────────────────────────┘
         │              │                        │
         │              │                        │
    Ports Exposed to Host:
         1883         8080                    8086
```

## Alert Trigger Conditions

### Heart Rate Alert
- Condition: `value > 150 BPM`
- Source: Simulator generates HR 151-180 during alert cycles
- Frequency: Based on ALERT_INTERVAL_SECONDS (default: 60s)

### Battery Alert
- Condition: `metadata.battery < 30%`
- Source: Simulator generates 10-35% battery (~30% of alert cycles)
- Frequency: Based on ALERT_INTERVAL_SECONDS (default: 60s)

### Combined Alert
Both conditions can trigger simultaneously, resulting in:
```
alertReason: "High heart rate: 165 BPM, Low battery: 25%"
```

## Vital Types and Ranges

| Type           | Device     | Normal Range  | Alert Range   | Unit    |
|----------------|------------|---------------|---------------|---------|
| HEART_RATE     | watch      | 60-100        | <45, >110     | BPM     |
| SPO2           | oximeter   | 95-100        | <90           | %       |
| BLOOD_PRESSURE | cuff       | 110-130/70-85 | ≥160/100      | mmHg    |
| GLUCOSE        | glucometer | 80-140        | <70, >250     | mg/dL   |
| WEIGHT         | scale      | 60-90         | <50, >120     | kg      |
| STEPS          | watch      | 2000-8000     | -             | steps   |

## Performance Characteristics

- **Throughput**: 10 patients × 6 vitals / 10 seconds = 6 messages/second
- **Latency**: MQTT + Telegraf processing < 50ms (lightweight binary)
- **Scalability**: Can handle 1000+ patients with current setup
- **Reliability**: QoS 1 ensures message delivery
- **Data Retention**: InfluxDB indefinite (until manually deleted)
- **Alert Persistence**: In-memory only (lost on restart)

## Volume Management

| Volume            | Purpose                  | Size Estimate   |
|-------------------|--------------------------|-----------------|
| mosquitto_data    | MQTT persistence         | < 100 MB        |
| mosquitto_logs    | Broker logs              | < 50 MB         |
| influxdb_data     | Time-series storage      | ~1 GB/day       |
| influxdb_config   | Database configuration   | < 10 MB         |
| node_red_data     | Flow runtime data        | < 50 MB         |
