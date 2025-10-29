Structure par page
------------------

- Pages: `admin.html`, `gateway.html`, `dashboard.html`, `reports.html`
- CSS: `css/base.css` (commun) + `css/admin.css`, `css/gateway.css`, `css/dashboard.css`, `css/reports.css`
- JS: `js/common.js` (commun) + `js/admin.js`, `js/gateway.js`, `js/dashboard.js`, `js/reports.js`

Ouverture
---------

- Ouvrir `frontend/index.html` (redirige vers Administration) ou une page directement.

Remarques
---------

- Données persistées via `localStorage`.
- Chaque page importe son JS via `<script type="module">`.
- Les seuils (alertes, graphiques) sont dans `js/common.js`.

