import {defineConfig} from 'vite'
import react from '@vitejs/plugin-react'
import {resolve} from 'path';
import devtoolsJson from 'vite-plugin-devtools-json';

// https://vite.dev/config/
export default defineConfig({
    plugins: [
        react(),
        devtoolsJson()
    ],
    resolve: {
        alias: {
            '@': resolve(__dirname, 'src'),
        },
    },
    base: '/',
    server: {
        port: 3000,
        open: true,
        proxy: {
            '/api': {
                //target: 'https://api.klabis.otakar.io',
                target: 'https://localhost:8443',
                secure: false,
                changeOrigin: true,
                rewrite: (path) => path.replace(/^\/api/, ''),
            },
        },
    }
})
