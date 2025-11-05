#!/usr/bin/env sh
set -eu

HOST_URL=${INFLUX_HOST_URL:-http://localhost:8086}
echo "Waiting for InfluxDB to be ready at ${HOST_URL}..."

# Run once: guard file for idempotency
SEED_FLAG="/docker-entrypoint-initdb.d/.influx_seed_done"
if [ -f "$SEED_FLAG" ]; then
  echo "Seed already applied. Skipping."
  exit 0
fi

# Wait until the InfluxDB API answers
until influx ping --host-url "$HOST_URL" >/dev/null 2>&1; do
  echo "InfluxDB not ready yet..."
  sleep 5
done

echo "Seeding test data into InfluxDB..."

# Settings align with docker-compose DOCKER_INFLUXDB_INIT_* envs
# TOKEN is required (provided by the official image during setup)
TOKEN="${DOCKER_INFLUXDB_INIT_ADMIN_TOKEN:?DOCKER_INFLUXDB_INIT_ADMIN_TOKEN is required}"
ORG="${DOCKER_INFLUXDB_INIT_ORG:-eSanteIdb}"
BUCKET="${DOCKER_INFLUXDB_INIT_BUCKET:-mesure_data}"

# Ensure bucket exists (idempotent)
if ! influx bucket list --host-url "$HOST_URL" --org "$ORG" --token "$TOKEN" | grep -q "\b$BUCKET\b"; then
  echo "Bucket '$BUCKET' missing. Creating..."
  influx bucket create --host-url "$HOST_URL" --org "$ORG" --token "$TOKEN" --name "$BUCKET" >/dev/null
fi

now_ts() { date +%s; }
NOW=$(now_ts)
HOUR=3600
DAY=$((24 * HOUR))

rand_u16() {
  # Portable random 0..65535
  od -An -N2 -tu2 /dev/urandom 2>/dev/null | tr -d ' '
}

val_range() {
  base=$1; span=$2
  r=$(rand_u16)
  echo $(( base + r % span ))
}

write_point() {
  line="$1"
  ts="$2"
  influx write --host-url "$HOST_URL" --org "$ORG" --bucket "$BUCKET" --precision s --token "$TOKEN" "$line $ts" >/dev/null
}

echo "Patient 1 (Alice)"
for i in $(seq 0 168); do
  ts=$((NOW - i * HOUR))
  hr=$(val_range 65 25)
  write_point "fc,patient=1,capteur=montre value=$hr" "$ts"
done

for i in $(seq 0 84); do
  ts=$((NOW - i * HOUR * 2))
  spo2=$(val_range 94 5)
  write_point "spO2,patient=1,capteur=montre value=$spo2" "$ts"
done

for i in $(seq 0 14); do
  ts=$((NOW - i * DAY / 2))
  systolic=$(val_range 125 30)
  diastolic=$(val_range 80 15)
  write_point "tension,patient=1,capteur=tensiometre systolique=$systolic,diastolique=$diastolic" "$ts"
done

for i in $(seq 0 7); do
  ts=$((NOW - i * DAY))
  w10=$(val_range 685 10)
  weight=$(printf '%d.%d' $((w10 / 10)) $((w10 % 10)))
  write_point "poids,patient=1,capteur=balance value=$weight" "$ts"
done

echo "Patient 2 (Bob)"
for i in $(seq 0 168); do
  ts=$((NOW - i * HOUR))
  hr=$(val_range 70 20)
  write_point "fc,patient=2,capteur=montre value=$hr" "$ts"
done

for i in $(seq 0 28); do
  ts=$((NOW - i * DAY / 4))
  glucose=$(val_range 100 120)
  write_point "glycemie,patient=2,capteur=glucometre value=$glucose" "$ts"
done

for i in $(seq 0 7); do
  ts=$((NOW - i * DAY))
  w10=$(val_range 823 10)
  weight=$(printf '%d.%d' $((w10 / 10)) $((w10 % 10)))
  write_point "poids,patient=2,capteur=balance value=$weight" "$ts"
done

for patient_id in 3 4 5; do
  echo "Patient $patient_id"
  for i in $(seq 0 100); do
    ts=$((NOW - i * HOUR))
    hr=$(val_range 60 30)
    write_point "fc,patient=$patient_id,capteur=montre value=$hr" "$ts"
  done
done

touch "$SEED_FLAG"
echo "InfluxDB seed completed."
