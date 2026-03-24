import { fileURLToPath, URL } from 'node:url'

import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import vueDevTools from 'vite-plugin-vue-devtools'

const devProxyTarget = process.env.VITE_DEV_PROXY_TARGET || 'http://localhost:8080'

export default defineConfig({
  plugins: [vue(), vueDevTools()],

  define: {
    'process.env': {},
  },

  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },

  server: {
    port: 3000,
    host: '0.0.0.0',
    open: true,

    proxy: {
      '/api': {
        target: devProxyTarget,
        changeOrigin: true,
      },
      '/admin': {
        target: devProxyTarget,
        changeOrigin: true,
      },
    },
  },
})
