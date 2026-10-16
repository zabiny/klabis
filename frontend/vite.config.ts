import {defineConfig} from 'vite'
import react from '@vitejs/plugin-react'
import {resolve} from 'node:path';
import devtoolsJson from 'vite-plugin-devtools-json';
import {VitePWA} from 'vite-plugin-pwa';

// https://vite.dev/config/
export default defineConfig({
    plugins: [
        react(),
        devtoolsJson(),
        VitePWA({
            strategies: 'injectManifest',
            srcDir: 'src',
            filename: 'sw.ts',
            injectRegister: false,
            injectManifest: {
                injectionPoint: undefined,
            },
            includeAssets: ['favicon.svg', 'apple-touch-icon.png'],
            manifest: {
                id: '/',
                name: 'Klabis - Členská sekce',
                short_name: 'Klabis',
                description: 'Členská sekce klubu orientačního běhu',
                lang: 'cs',
                theme_color: '#0d9488',
                background_color: '#ffffff',
                display: 'standalone',
                orientation: 'portrait',
                scope: '/',
                start_url: '/',
                icons: [
                    {
                        src: 'pwa-192x192.png',
                        sizes: '192x192',
                        type: 'image/png',
                    },
                    {
                        src: 'pwa-512x512.png',
                        sizes: '512x512',
                        type: 'image/png',
                    },
                    {
                        src: 'pwa-512x512-maskable.png',
                        sizes: '512x512',
                        type: 'image/png',
                        purpose: 'maskable',
                    },
                ],
            },
            devOptions: {
                // Serve the manifest and the worker on the Vite dev server
                // (localhost:3000) so PWA install and push can be tested with HMR.
                enabled: true,
            },
        }),
    ],
    resolve: {
        alias: {
            '@': resolve(import.meta.dirname, 'src'),
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
