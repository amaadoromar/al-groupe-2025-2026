# Telegraf Stream Processor

Telegraf handles real-time stream processing for the eSanté platform.

## Overview

Telegraf acts as the data streaming layer between MQTT (vitals data) and our backend systems:
- Consumes patient vitals from MQTT topics
- Processes data for alert detection
- Routes alerts to notification system via MQTT
- Stores normal vitals in InfluxDB for analytics

## Configuration

See [`telegraf.conf`](./telegraf.conf) for the complete configuration.

### Key Components

1. **MQTT Input**: Subscribes to `esante/patient/+/vitals/#`
2. **Regex Processor**: Extracts patientId and measurementType from topic
3. **Starlark Processor**: Alert detection logic (HR > 150, battery < 30%)
4. **Dual Outputs**:
   - MQTT: Alerts to `esante/notifications/patient/{id}`
   - InfluxDB: Normal vitals to `patient_vitals` bucket

## Documentation

- **[TELEGRAF-GUIDE.md](./TELEGRAF-GUIDE.md)** - Complete configuration guide, troubleshooting, and monitoring

## Quick Start

Telegraf runs automatically with the E2E stack:

```bash
docker compose -f docker-compose-e2e.yml up -d
```

## Monitoring

Check Telegraf logs:
```bash
docker logs esante_telegraf -f
```

View metrics:
```bash
# Total messages processed
docker logs esante_telegraf 2>&1 | grep "metrics gathered"

# Alert detection
docker logs esante_telegraf 2>&1 | grep "alert"
```

## Performance

- **Throughput**: 10,000+ messages/second
- **Latency**: <50ms average processing time
- **Memory**: ~50MB footprint
- **CPU**: <5% utilization (10 patients)

## Architecture

```
MQTT (vitals) → Telegraf → [Alert Detection] → MQTT (notifications)
                         ↓
                    InfluxDB (storage)
```

For detailed architecture diagrams and data flows, see the main [ARCHITECTURE.md](../../docs/ARCHITECTURE.md).
