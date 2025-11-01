#!/bin/bash
set -e

echo " Attente du démarrage d'InfluxDB..."
sleep 10

echo "" Insertion des données de test dans InfluxDB..."

influx write \
  --org eSanteIdb \
  --bucket mesure_data \
  --precision s \
  'fc,patient=1,capteur=montre value=78 1730455200
spO2,patient=1,capteur=montre value=96 1730455260
tension,patient=1,capteur=tensiometre systolique=132,diastolique=85 1730455320
glycemie,patient=2,capteur=glucometre value=145 1730455380
poids,patient=1,capteur=balance value=72.8 1730455440
poids,patient=2,capteur=balance value=85.4 1730455500'

echo " Données InfluxDB initialisées avec succès !"
