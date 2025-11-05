# Infrastructure Components

This directory contains the infrastructure components for the eSanté platform.

## Components

### [telegraf/](./telegraf/)
**Stream Processor** - Handles real-time data streaming between MQTT and backend systems.

- Consumes patient vitals from MQTT
- Detects alert conditions (high heart rate, low battery)
- Routes alerts to notification service
- Stores normal vitals in InfluxDB

**Documentation**: [telegraf/README.md](./telegraf/README.md)

### [simulator/](./simulator/)
**Device Simulator** - Go-based simulator for medical devices and patient phones.

- Generates realistic vital signs (heart rate, SpO2, blood pressure, etc.)
- Simulates 10 patients with configurable intervals
- Publishes vitals to MQTT topics
- Generates alert conditions for testing

**Documentation**: [simulator/README.md](./simulator/README.md)

### [influxdb/](./influxdb/)
**Time-Series Database** - Stores patient vitals for analytics and reporting.

- Bucket: `patient_vitals`
- Organization: `esante`
- 5 pre-configured patient dashboards
- Retention: 30 days default

**Access**: http://localhost:8086 (admin/adminpassword)

## Data Flow

```
Simulator → MQTT → Telegraf → [Alert Detection] → MQTT (notifications)
                             ↓
                        InfluxDB (storage)
```

## Running Infrastructure

Start all components:
```bash
docker compose -f docker-compose-e2e.yml up -d
```

Check component status:
```bash
docker compose -f docker-compose-e2e.yml ps
```

View logs:
```bash
# Telegraf
docker logs esante_telegraf -f

# Simulator
docker logs esante_simulator -f

# InfluxDB
docker logs esante_influxdb -f
```

## Configuration

- **Telegraf**: [telegraf/telegraf.conf](./telegraf/telegraf.conf)
- **Simulator**: [simulator/.env](./simulator/.env)
- **InfluxDB**: [../init/influx_init.sh](../init/influx_init.sh)

## Monitoring

### Telegraf Metrics
```bash
docker logs esante_telegraf 2>&1 | grep "metrics gathered"
```

### Simulator Status
```bash
docker logs esante_simulator 2>&1 | tail -20
```

### InfluxDB Query
```bash
docker exec -it esante_influxdb influx query 'from(bucket: "patient_vitals") |> range(start: -5m) |> limit(n: 10)'
```

## Performance

| Component | Memory | CPU | Throughput |
|-----------|--------|-----|------------|
| Telegraf | ~50MB | <5% | 10,000+ msg/s |
| Simulator | ~20MB | <2% | 60 msg/min |
| InfluxDB | ~200MB | <10% | 1,000+ writes/s |

## Related Documentation

- [E2E-QUICKSTART.md](../E2E-QUICKSTART.md) - Quick start guide
- [ARCHITECTURE.md](../ARCHITECTURE.md) - System architecture
- [docker-compose-e2e.yml](../docker-compose-e2e.yml) - Docker orchestration
