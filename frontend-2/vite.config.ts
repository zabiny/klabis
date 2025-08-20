import {defineConfig} from 'vite'
import react from '@vitejs/plugin-react'
import {resolve} from 'path';

// https://vite.dev/config/
export default defineConfig({
    plugins: [react()],
    resolve: {
        alias: {
            '@': resolve(__dirname, 'src'),
        },
    },
    server: {
        port: 3000,
        open: true,
        proxy: {
            '/api': {
                //target: 'https://api.klabis.otakar.io',
                target: 'http://localhost:8080',
                changeOrigin: true,
                rewrite: (path) => path.replace(/^\/api/, ''),
            },
        },
    }
})
