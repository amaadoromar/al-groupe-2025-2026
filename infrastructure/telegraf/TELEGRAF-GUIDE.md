# Telegraf Stream Processing Guide

## Overview

Telegraf is a lightweight, plugin-driven server agent for collecting, processing, and routing time-series data in the eSante system.

## Architecture

```
MQTT Input → JSON Parser → Starlark Processor → Routing
                                                   ├─ Alerts → MQTT Output
                                                   └─ Normal → InfluxDB Output
```

## Configuration

The complete configuration is in `infrastructure/telegraf/telegraf.conf`.

### Key Sections

#### 1. MQTT Input Consumer
```toml
[[inputs.mqtt_consumer]]
  servers = ["tcp://mosquitto:1883"]
  topics = ["esante/patient/+/vitals/#"]
  qos = 1
  data_format = "json"
```

Subscribes to all patient vitals topics with QoS 1 (guaranteed delivery).

#### 2. Topic Parsing
```toml
[[processors.regex]]
  [[processors.regex.tags]]
    key = "mqtt_topic"
    pattern = "^esante/patient/([^/]+)/vitals/(.+)$"
    replacement = "${1}"
    result_key = "patientId"
```

Extracts `patientId` and `measurementType` from MQTT topic path.

#### 3. Alert Detection (Starlark)
```python
def apply(metric):
    battery = metric.fields.get("battery", 100.0)
    heart_rate = metric.fields.get("heartRate", 0.0)
    value = metric.fields.get("value", 0.0)
    
    is_alert = False
    alert_reason = ""
    
    if battery < 30:
        is_alert = True
        alert_reason = "Low battery: " + str(int(battery)) + "%"
    
    if heart_rate > 150 or (vital_type == "heartRate" and value > 150):
        is_alert = True
        alert_reason = "High heart rate: " + str(int(value)) + " BPM"
    
    if is_alert:
        metric.tags["alert"] = "true"
        metric.fields["alertReason"] = alert_reason
    else:
        metric.tags["alert"] = "false"
    
    return metric
```

Tags metrics with `alert=true` or `alert=false` based on conditions.

#### 4. Dual Output Routing

**Alerts to MQTT:**
```toml
[[outputs.mqtt]]
  servers = ["tcp://mosquitto:1883"]
  topic = "esante/notifications/patient/{{ .Tag \"patientId\" }}"
  qos = 1
  tagpass = ["alert:true"]  # Only send alerts
```

**Normal vitals to InfluxDB:**
```toml
[[outputs.influxdb_v2]]
  urls = ["http://influxdb:8086"]
  token = "esante-admin-token-change-in-production"
  organization = "esante"
  bucket = "patient_vitals"
  tagdrop = ["alert:true"]  # Exclude alerts
```

## Monitoring

### View Telegraf Logs
```bash
# Real-time logs
docker logs -f esante_telegraf

# Last 100 lines
docker logs --tail 100 esante_telegraf
```

### Check Telegraf Status
```bash
# Verify container is running
docker ps | grep telegraf

# Check resource usage
docker stats esante_telegraf
```

### Monitor MQTT Notifications
```bash
# Subscribe to all notifications
docker exec esante_mqtt_broker mosquitto_sub -t "esante/notifications/#" -v

# Subscribe to specific patient
docker exec esante_mqtt_broker mosquitto_sub -t "esante/notifications/patient/patient-1" -v
```

### Query InfluxDB Data
```bash
# Access InfluxDB UI
open http://localhost:8086

# Or use CLI
docker exec esante_influxdb influx query \
  --org esante \
  --token esante-admin-token-change-in-production \
  'from(bucket:"patient_vitals") |> range(start: -1h) |> limit(n:10)'
```

## Alert Conditions

| Condition         | Threshold | Action                          |
|-------------------|-----------|---------------------------------|
| Heart Rate High   | > 150 BPM | Publish to MQTT notifications   |
| Battery Low       | < 30%     | Publish to MQTT notifications   |
| Normal Vitals     | Otherwise | Write to InfluxDB               |

## Troubleshooting

### No Data in InfluxDB

1. Check Telegraf is receiving MQTT messages:
```bash
docker logs esante_telegraf | grep "mqtt_consumer"
```

2. Verify InfluxDB connection:
```bash
docker logs esante_telegraf | grep "influxdb"
```

3. Check InfluxDB health:
```bash
docker exec esante_influxdb influx ping
```

### No Notifications on MQTT

1. Verify alerts are being generated:
```bash
docker logs esante_telegraf | grep "alert.*true"
```

2. Check MQTT broker is running:
```bash
docker ps | grep mosquitto
```

3. Manually subscribe and test:
```bash
docker exec esante_mqtt_broker mosquitto_sub -t "esante/notifications/#" -v
```

### Telegraf Container Crashes

1. Check configuration syntax:
```bash
docker run --rm -v $(pwd)/infrastructure/telegraf/telegraf.conf:/etc/telegraf/telegraf.conf:ro \
  telegraf:1.28 --test
```

2. View crash logs:
```bash
docker logs esante_telegraf
```

3. Restart with verbose logging:
```bash
docker compose -f docker-compose-e2e.yml restart telegraf
docker logs -f esante_telegraf
```

## Configuration Changes

### Modify Alert Thresholds

Edit `infrastructure/telegraf/telegraf.conf`:

```python
# Change heart rate threshold from 150 to 140
if heart_rate > 140 or (vital_type == "heartRate" and value > 140):

# Change battery threshold from 30% to 20%
if battery < 20:
```

Then restart Telegraf:
```bash
docker compose -f docker-compose-e2e.yml restart telegraf
```

### Add New Output

Edit `telegraf.conf` and add a new output section:

```toml
[[outputs.file]]
  files = ["/tmp/vitals.json"]
  data_format = "json"
```

### Change Flush Interval

For higher throughput, reduce flush interval in `[agent]` section:

```toml
[agent]
  interval = "1s"      # Collect every 1 second
  flush_interval = "1s" # Write every 1 second
```

## Performance Tuning

### High Message Volume

Increase buffer sizes:
```toml
[agent]
  metric_batch_size = 5000   # Default: 1000
  metric_buffer_limit = 50000 # Default: 10000
```

### Low Latency Requirements

Reduce intervals:
```toml
[agent]
  interval = "100ms"
  flush_interval = "100ms"
```

### Memory Optimization

Limit buffer and use streaming:
```toml
[agent]
  metric_buffer_limit = 1000
  flush_interval = "1s"
```

## Additional Resources

- [Telegraf Documentation](https://docs.influxdata.com/telegraf/)
- [MQTT Consumer Plugin](https://github.com/influxdata/telegraf/tree/master/plugins/inputs/mqtt_consumer)
- [Starlark Processor](https://github.com/influxdata/telegraf/tree/master/plugins/processors/starlark)
- [InfluxDB Output](https://github.com/influxdata/telegraf/tree/master/plugins/outputs/influxdb_v2)
