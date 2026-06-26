import { defineConfig } from 'vite';

export default defineConfig({
  build: {
    chunkSizeWarningLimit: 1200,
  },
  server: {
    port: 5173,
    host: '0.0.0.0',
  },
});
