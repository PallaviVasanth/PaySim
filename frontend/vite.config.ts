import react from '@vitejs/plugin-react';
import { defineConfig, loadEnv } from 'vite';

// In dev, the browser talks to Vite (same origin) and Vite forwards API calls to Spring Boot.
export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, '.', '');
  const backend = env.VITE_DEV_PROXY_TARGET || 'http://localhost:8080';
  return {
    plugins: [react()],
    server: {
      port: 5173,
      proxy: {
        '/api': backend,
        '/actuator': backend,
        '/swagger-ui': backend,
        '/v3/api-docs': backend,
      },
    },
  };
});
