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
            '/api': {               // proxy API calls to backend
                target: 'https://localhost:8443',
                secure: false,
                changeOrigin: true,
                // DON'T rewrite - backend expects /api prefix
            },
            '/.well-known': {       // rewrite OAuth2 authorization. Only this is needed - all other URIs (token, etc) returned there are going directly to 8443
                target: 'https://localhost:8443',
                secure: false,
                changeOrigin: true,
            },
            '/login': {             // proxy Spring Security form login POST to backend
                target: 'https://localhost:8443',
                secure: false,
                changeOrigin: true,
                bypass(req) {
                    // Only proxy POST requests; GET /login is served by React SPA
                    if (req.method === 'GET') return req.url;
                },
            },
            '/oauth2': {            // proxy OAuth2 endpoints
                target: 'https://localhost:8443',
                secure: false,
                changeOrigin: true,
            }
        },
    }
})
