# Node-RED Configuration Guide

## Initial Setup

After first start, Node-RED will automatically:
1. Install required nodes from `package.json`
2. Load pre-configured flows from `flows.json`
3. Connect to MQTT broker and InfluxDB

## Accessing Node-RED

**URL**: http://localhost:1880/admin

No authentication is configured (development mode).

## Pre-Configured Flows

The `flows.json` includes:

### Flow: "eSante Data Streaming"
- **MQTT Input Node**: Subscribes to `esante/patient/+/vitals/#`
- **JSON Parser**: Parses incoming messages
- **Alert Checker** (Function): Checks HR > 150 and battery < 30%
- **Notification Formatter** (Function): Creates alert payload
- **MQTT Output**: Publishes to `esante/notifications/patient/{id}`
- **InfluxDB Formatter** (Function): Converts to line protocol
- **InfluxDB Output**: Writes to `patient_vitals` bucket
- **Debug Nodes**: Monitor notifications and InfluxDB writes

## Configuration Nodes

### MQTT Broker Config
- **Name**: Mosquitto Broker
- **Server**: mosquitto
- **Port**: 1883
- **Client ID**: node-red-streaming
- **Protocol**: MQTT 3.1.1
- **Clean Session**: Yes

### InfluxDB Config
- **Name**: InfluxDB
- **URL**: http://influxdb:8086
- **Version**: 2.0
- **Organization**: esante
- **Bucket**: patient_vitals
- **Token**: From INFLUXDB_TOKEN env variable

## Verifying Configuration

### 1. Check Flow Deployment
```bash
docker compose -f docker-compose-e2e.yml logs node-red | grep -i "flows deployed"
```

Should see:
```
[info] Flows file     : flows.json
[info] Started flows
```

### 2. Check MQTT Connection
In Node-RED UI:
- MQTT input node should show "connected" status (green dot)
- If disconnected (red), check mosquitto service

### 3. Check InfluxDB Connection
In Node-RED UI:
- Deploy a test flow with debug output
- Check debug panel for errors

### 4. Test End-to-End
```bash
# Subscribe to notifications
docker exec esante_mqtt_broker mosquitto_sub -t "esante/notifications/#" -v

# Should see alerts when HR > 150 or battery < 30%
```

## Customizing Flows

### Modifying Alert Conditions

Edit the "Check Alert Conditions" function node:

```javascript
// Current conditions
if (battery < 30) {
    alertTriggered = true;
    alertReason.push(`Low battery: ${battery}%`);
}

if (heartRate && heartRate > 150) {
    alertTriggered = true;
    alertReason.push(`High heart rate: ${heartRate} BPM`);
}

// Add new conditions, e.g., SpO2
const spo2 = vital.measurementType === 'SPO2' ? vital.value : null;
if (spo2 && spo2 < 90) {
    alertTriggered = true;
    alertReason.push(`Low oxygen: ${spo2}%`);
}
```

Click **Deploy** to apply changes.

## Adding New Nodes

### Method 1: UI (Palette Manager)
1. Open http://localhost:1880/admin
2. Menu (☰) → Manage palette
3. Install tab → Search for nodes
4. Click Install

### Method 2: Docker Exec
```bash
docker exec esante_node_red npm install --prefix /data node-red-contrib-package-name
docker compose -f docker-compose-e2e.yml restart node-red
```

### Method 3: Update package.json
```json
{
  "dependencies": {
    "node-red": "^3.1.0",
    "node-red-contrib-influxdb": "^0.6.1",
    "node-red-contrib-your-package": "^1.0.0"
  }
}
```

Then rebuild:
```bash
docker compose -f docker-compose-e2e.yml up --build -d node-red
```

## Exporting Flows

### Via UI
1. Select all nodes (Ctrl+A)
2. Menu → Export → Clipboard
3. Copy JSON

### Via File
Flows are automatically saved to `infrastructure/node-red/flows.json`

## Importing Flows

### Via UI
1. Menu → Import → Clipboard
2. Paste JSON
3. Click Import

### Via File
Replace `infrastructure/node-red/flows.json` and restart:
```bash
docker compose -f docker-compose-e2e.yml restart node-red
```

## Debugging

### Enable Debug Logging
Edit `infrastructure/node-red/settings.js`:
```javascript
logging: {
    console: {
        level: "debug",  // Changed from "info"
        metrics: true,
        audit: true
    }
}
```

Restart Node-RED:
```bash
docker compose -f docker-compose-e2e.yml restart node-red
```

### View Debug Messages
In Node-RED UI:
1. Click bug icon (🐛) on right sidebar
2. Shows output from debug nodes

Or via CLI:
```bash
docker compose -f docker-compose-e2e.yml logs -f node-red
```

### Common Issues

#### "Cannot find module 'node-red-contrib-influxdb'"
**Solution**: Wait for npm install to complete on first start
```bash
docker compose -f docker-compose-e2e.yml logs node-red | grep "influxdb"
```

#### "MQTT connection failed"
**Solution**: Check mosquitto is running
```bash
docker compose -f docker-compose-e2e.yml ps mosquitto
```

#### "InfluxDB write error: unauthorized"
**Solution**: Verify token in environment
```bash
docker compose -f docker-compose-e2e.yml exec node-red env | grep INFLUX
```

#### "Permission denied" on startup
**Solution**: Fix directory ownership
```bash
sudo chown -R 1000:1000 infrastructure/node-red/
```

## Performance Tuning

### Increase Node-RED Memory
In `docker-compose-e2e.yml`, add to node-red service:
```yaml
environment:
  NODE_OPTIONS: "--max-old-space-size=4096"
```

### Batch InfluxDB Writes
Modify the InfluxDB formatter function to collect multiple points:
```javascript
// Store in context
context.set('buffer', context.get('buffer') || []);
let buffer = context.get('buffer');
buffer.push(msg.payload);

// Write batch every 10 messages
if (buffer.length >= 10) {
    msg.payload = buffer;
    context.set('buffer', []);
    return msg;
}
return null; // Don't send yet
```

## Security (Production)

### Enable Admin Authentication
Edit `infrastructure/node-red/settings.js`:
```javascript
adminAuth: {
    type: "credentials",
    users: [{
        username: "admin",
        password: "$2b$08$hashedpassword",
        permissions: "*"
    }]
}
```

Generate password hash:
```bash
docker exec esante_node_red node -e "console.log(require('bcryptjs').hashSync('your-password', 8))"
```

### Enable MQTT Authentication
Update MQTT broker config node:
- Enable authentication
- Set username/password
- Update mosquitto.conf accordingly

### Secure InfluxDB Token
Use Docker secrets instead of environment variables:
```yaml
secrets:
  - influxdb_token

services:
  node-red:
    secrets:
      - influxdb_token
```

## Monitoring Node-RED

### Health Check
```bash
curl http://localhost:1880/
```

### Check Flow Stats
Access metrics (if enabled):
```bash
curl http://localhost:1880/metrics
```

### Monitor Resource Usage
```bash
docker stats esante_node_red
```

## Backup and Restore

### Backup
```bash
# Backup flows and settings
tar -czf node-red-backup.tar.gz infrastructure/node-red/

# Backup runtime data
docker run --rm -v esante_node_red_data:/data -v $(pwd):/backup alpine tar czf /backup/node-red-data.tar.gz /data
```

### Restore
```bash
# Restore flows and settings
tar -xzf node-red-backup.tar.gz

# Restore runtime data
docker run --rm -v esante_node_red_data:/data -v $(pwd):/backup alpine tar xzf /backup/node-red-data.tar.gz -C /
```

## Additional Resources

- [Node-RED Documentation](https://nodered.org/docs/)
- [Node-RED Cookbook](https://cookbook.nodered.org/)
- [InfluxDB Node Documentation](https://flows.nodered.org/node/node-red-contrib-influxdb)
- [MQTT Node Documentation](https://flows.nodered.org/node/node-red-contrib-mqtt-broker)
