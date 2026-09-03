import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";

// Vite dev server proxies /api to the Spring Boot backend so the
// browser only ever talks to one origin and no CORS config is needed
// in dev. In production, build this app and either serve it
// separately (point VITE-built app at the deployed API origin) or
// copy dist/ into src/main/resources/static so Spring Boot serves
// both the app and the API from one origin.
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      "/api": {
        target: "http://localhost:8080",
        changeOrigin: true
      }
    }
  },
  build: {
    outDir: "dist"
  }
});
