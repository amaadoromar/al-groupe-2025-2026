POC e‑santé – Frontend (sans back)

Ce dépôt contient un prototype front (HTML/CSS/JS sans dépendances) qui démontre la chaîne fonctionnelle demandée:

- Inscription d’un nouveau patient par un administrateur
- Simulation de capteurs (smartphone passerelle) pour 2–3 mesures: rythme cardiaque, SpO₂, température
- Tableau de bord pour visualiser les mesures en temps réel avec graphiques
- Système d’alertes (seuils critiques) avec notifications navigateur et son
- Génération d’un rapport imprimable/exportable en PDF (via impression navigateur)

Utilisation:
1) Ouvrir `frontend/index.html` dans un navigateur moderne (Chrome/Edge/Firefox).
2) Onglet Administration: créer un patient (au minimum prénom, nom, date de naissance).
3) Onglet Passerelle: sélectionner le patient, démarrer la simulation. Le bouton «Forcer alerte» introduit des valeurs critiques.
4) Onglet Tableau de bord: sélectionner le patient pour visualiser les courbes et les alertes. Bouton «Activer notifications» pour recevoir des notifications système.
5) Onglet Rapports: choisir patient et période, puis «Générer le rapport». Utiliser «Imprimer / Exporter en PDF» pour produire le PDF.

Notes techniques:
- Aucune dépendance externe; stockage local via `localStorage`.
- Les graphiques sont dessinés en `<canvas>` côté client.
- Seuils d’alerte: HR < 45 ou > 110, SpO₂ < 90, Température > 38.5°C (modifiables dans `frontend/app.js`).

