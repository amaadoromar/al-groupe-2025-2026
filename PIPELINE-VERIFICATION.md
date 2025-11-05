# Quick Pipeline Verification Guide

## Access Points

1. **Node-RED UI**: http://localhost:1880/admin
2. **MQTT Web UI**: http://localhost:8080  
3. **InfluxDB UI**: http://localhost:8086

## Verify Pipeline is Working

### Step 1: Check Services Status
```bash
docker ps --filter "name=esante"
```

All services should show "Up" status.

### Step 2: Verify Simulator is Publishing
```bash
# Watch vitals being published (Ctrl+C to stop)
docker exec esante_mqtt_broker mosquitto_sub -t "esante/patient/+/vitals/#" -v
```

You should see messages like:
```
esante/patient/1/vitals/HEART_RATE {"patientId":"patient-1", ...}
```

### Step 3: Open Node-RED UI

1. Go to **http://localhost:1880/admin**
2. You should see the "eSante Data Streaming" flow
3. Click the **Deploy** button (top right) if you see it
4. Click the **bug icon** (🐛) on the right sidebar to open the Debug panel

### Step 4: Fix InfluxDB Node (One-Time Setup)

The InfluxDB node needs the measurement name set in the node configuration:

1. **Double-click** the "InfluxDB - Vitals" node (blue node on the right)
2. In the configuration panel:
   - **Measurement**: Leave empty (it comes from msg.payload.measurement)
   - **Server**: Should show "InfluxDB" - click pencil icon to edit
3. Click **Done**
4. Click **Deploy** (top right)

### Step 5: Configure InfluxDB Connection

1. In Node-RED, double-click the "InfluxDB - Vitals" node
2. Click the pencil icon next to "Server"
3. Enter:
   - **URL**: `http://influxdb:8086`
   - **Version**: `2.0`
   - **Organization**: `esante`
   - **Token**: `esante-admin-token-change-in-production`
   - **Bucket**: `patient_vitals`
4. Click **Update**, then **Done**, then **Deploy**

### Step 6: Watch Debug Output

In the Debug panel (right sidebar with bug icon), you should now see:

**Normal vitals** (every 10 seconds):
```json
{
  "measurement": "patient_vitals",
  "tags": {
    "patientId": "patient-1",
    "deviceType": "watch",
    "measurementType": "HEART_RATE"
  },
  "fields": {
    "value": 75.2,
    "battery": 82
  }
}
```

**Alerts** (when HR > 150 or battery < 30%):
```json
{
  "patientId": "patient-5",
  "vitalType": "HEART_RATE",
  "value": 165,
  "alertReason": "High heart rate: 165 BPM",
  "battery": 25
}
```

### Step 7: Monitor Notifications

Open a terminal and subscribe to alerts:
```bash
docker exec esante_mqtt_broker mosquitto_sub -t "esante/notifications/#" -v
```

Wait for the next alert cycle (every 60 seconds). You'll see notifications published to:
```
esante/notifications/patient/1
esante/notifications/patient/5
...
```

### Step 8: Check InfluxDB Data

1. Open **http://localhost:8086**
2. Login with:
   - **Username**: admin
   - **Password**: adminpassword
3. Go to **Data Explorer**
4. Select:
   - **Bucket**: patient_vitals
   - **Measurement**: patient_vitals
   - **Fields**: value, battery
5. Click **Submit** - you should see time-series graphs

## Troubleshooting

### No data in Debug panel
```bash
# Check Node-RED is connected to MQTT
docker logs esante_node_red | grep -i mqtt

# Verify MQTT broker is healthy
docker exec esante_mqtt_broker mosquitto_sub -t '$SYS/#' -C 1
```

### InfluxDB errors
```bash
# Check InfluxDB is healthy
docker logs esante_influxdb | tail -20

# Test InfluxDB API
curl http://localhost:8086/health
```

### No notifications
```bash
# Check when next alert cycle occurs
docker logs esante_simulator | grep "Alert cycle"

# Alert cycles happen every 60 seconds by default
# Look for cycle numbers divisible by 6 (60s intervals)
```

### Node-RED won't deploy
```bash
# Restart Node-RED
docker compose -f docker-compose-e2e.yml restart node-red

# Check logs
docker logs esante_node_red --tail 50
```

## Testing Alert Conditions

Alerts trigger when:
- **Heart Rate > 150 BPM**: Simulator generates 151-180 during alert cycles
- **Battery < 30%**: Simulator generates 10-35% on ~30% of alert cycles

To force more frequent alerts, edit `.env`:
```bash
ALERT_INTERVAL_SECONDS=20  # Alerts every 20 seconds instead of 60
```

Then restart:
```bash
docker compose -f docker-compose-e2e.yml restart simulator
```

## Quick Commands

```bash
# View all logs
docker compose -f docker-compose-e2e.yml logs -f

# View specific service
docker compose -f docker-compose-e2e.yml logs -f node-red

# Restart everything
docker compose -f docker-compose-e2e.yml restart

# Stop everything
docker compose -f docker-compose-e2e.yml down

# Clean restart
docker compose -f docker-compose-e2e.yml down -v
docker compose -f docker-compose-e2e.yml up -d
```

## Success Criteria

✅ Node-RED UI accessible at http://localhost:1880/admin  
✅ Debug panel shows incoming vitals every 10 seconds  
✅ Alerts appear in debug panel when HR > 150 or battery < 30%  
✅ Notifications published to MQTT `esante/notifications/patient/{id}` topics  
✅ InfluxDB UI shows patient_vitals data in graphs  
✅ MQTT UI at http://localhost:8080 shows real-time messages  

## Next Steps

Once verified:
1. Customize alert conditions in Node-RED function nodes
2. Add more vital types to alert logic (SpO2, blood pressure, etc.)
3. Integrate backend service to consume notifications
4. Set up Grafana dashboards for InfluxDB data visualization
