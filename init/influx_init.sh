#!/bin/bash
set -e

echo "⏳ Attente du démarrage d'InfluxDB..."

# Attend que l'API InfluxDB réponde
until influx ping &> /dev/null; do
  echo "⏳ InfluxDB n'est pas encore prêt..."
  sleep 5
done

echo "📊 Insertion de BEAUCOUP de données de test dans InfluxDB..."

# Variables globales
TOKEN="my-super-secret-auth-token"
ORG="eSanteIdb"
BUCKET="mesure_data"

# Fonction pour générer des timestamps récents
NOW=$(date +%s)
HOUR=3600
DAY=$((24 * HOUR))

# ========================================
# PATIENT 1 - Alice (Hypertension)
# ========================================
echo "📝 Génération données Patient 1 (Alice)..."

# Fréquence cardiaque (7 derniers jours, toutes les heures)
for i in $(seq 0 168); do
  timestamp=$((NOW - i * HOUR))
  hr=$((65 + RANDOM % 25))  # 65-90 bpm
  influx write --org "$ORG" --bucket "$BUCKET" --precision s --token "$TOKEN" \
    "fc,patient=1,capteur=montre value=$hr $timestamp"
done

# SpO2 (7 derniers jours, toutes les 2 heures)
for i in $(seq 0 84); do
  timestamp=$((NOW - i * HOUR * 2))
  spo2=$((94 + RANDOM % 5))  # 94-99%
  influx write --org "$ORG" --bucket "$BUCKET" --precision s --token "$TOKEN" \
    "spO2,patient=1,capteur=montre value=$spo2 $timestamp"
done

# Tension artérielle (7 derniers jours, 2x/jour)
for i in $(seq 0 14); do
  timestamp=$((NOW - i * DAY / 2))
  systolic=$((125 + RANDOM % 30))
  diastolic=$((80 + RANDOM % 15))
  influx write --org "$ORG" --bucket "$BUCKET" --precision s --token "$TOKEN" \
    "tension,patient=1,capteur=tensiometre systolique=$systolic,diastolique=$diastolic $timestamp"
done

# Poids (7 derniers jours)
for i in $(seq 0 7); do
  timestamp=$((NOW - i * DAY))
  weight_tenths=$((685 + RANDOM % 10))
  weight="$(printf '%d.%d' $((weight_tenths / 10)) $((weight_tenths % 10)))"
  influx write --org "$ORG" --bucket "$BUCKET" --precision s --token "$TOKEN" \
    "poids,patient=1,capteur=balance value=$weight $timestamp"
done

# ========================================
# PATIENT 2 - Bob (Diabète)
# ========================================
echo "📝 Génération données Patient 2 (Bob)..."

for i in $(seq 0 168); do
  timestamp=$((NOW - i * HOUR))
  hr=$((70 + RANDOM % 20))
  influx write --org "$ORG" --bucket "$BUCKET" --precision s --token "$TOKEN" \
    "fc,patient=2,capteur=montre value=$hr $timestamp"
done

for i in $(seq 0 28); do
  timestamp=$((NOW - i * DAY / 4))
  glucose=$((100 + RANDOM % 120))
  influx write --org "$ORG" --bucket "$BUCKET" --precision s --token "$TOKEN" \
    "glycemie,patient=2,capteur=glucometre value=$glucose $timestamp"
done

for i in $(seq 0 7); do
  timestamp=$((NOW - i * DAY))
  weight_tenths=$((823 + RANDOM % 10))
  weight="$(printf '%d.%d' $((weight_tenths / 10)) $((weight_tenths % 10)))"
  influx write --org "$ORG" --bucket "$BUCKET" --precision s --token "$TOKEN" \
    "poids,patient=2,capteur=balance value=$weight $timestamp"
done

# ========================================
# PATIENTS 3–5
# ========================================
for patient_id in 3 4 5; do
  echo "📝 Génération données Patient $patient_id..."
  for i in $(seq 0 100); do
    timestamp=$((NOW - i * HOUR))
    hr=$((60 + RANDOM % 30))
    influx write --org "$ORG" --bucket "$BUCKET" --precision s --token "$TOKEN" \
      "fc,patient=$patient_id,capteur=montre value=$hr $timestamp"
  done
done

echo "✅ Données InfluxDB initialisées avec succès !"
