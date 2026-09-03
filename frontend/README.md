# Packet Analyzer — React Frontend

A Vite + React dashboard for the Spring Boot DPI packet analyzer backend.
It replicates the previous static `index.html` dashboard as real
components (upload card, stats, protocol breakdown, applications,
alerts, flows), talking to the existing `/api/analyze-pcap` endpoint.

## Run it

```bash
cd frontend
npm install
npm run dev
```

This starts the app at `http://localhost:5173`. The dev server proxies
any `/api/*` request to `http://localhost:8080` (see `vite.config.js`),
so just make sure the Spring Boot backend is running:

```bash
# from the project root
./mvnw spring-boot:run
```

Because of the proxy, no CORS setup is required in dev — the browser
only ever talks to `localhost:5173`. (`CorsConfig.java` still allows
`localhost:5173` directly, in case you run the built app against the
API from a different origin.)

## Build for production

```bash
npm run build
```

Outputs static assets to `frontend/dist`. Two ways to serve it:

1. **Standalone** — serve `frontend/dist` with any static host / CDN,
   pointed at wherever the Spring Boot API is deployed (update the CORS
   allowed origins accordingly).
2. **Same-origin with Spring Boot** — copy the contents of
   `frontend/dist` into `src/main/resources/static`, replacing the old
   static files. Spring Boot will then serve the React app and the API
   from the same origin, and the relative `/api/...` calls in
   `src/api.js` work unchanged with no CORS config needed at all.

## Project layout

```
frontend/
  src/
    api.js              # fetch wrapper for /api/analyze-pcap
    App.jsx             # top-level state (selected file, analysis result, status)
    components/         # Sidebar, TopBar, UploadCard, StatsGrid, panels, etc.
    utils/format.js      # number/byte formatting, flow/application normalization
    styles/index.css     # design tokens + component styles
```
