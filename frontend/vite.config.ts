import {defineConfig} from 'vite'
import react from '@vitejs/plugin-react'
import {resolve} from 'path';
import devtoolsJson from 'vite-plugin-devtools-json';
import {VitePWA} from 'vite-plugin-pwa';

// https://vite.dev/config/
export default defineConfig({
    plugins: [
        react(),
        devtoolsJson(),
        VitePWA({
            registerType: 'prompt',
            injectRegister: false,
            includeAssets: ['favicon.svg', 'apple-touch-icon.png'],
            manifest: {
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
            workbox: {
                globPatterns: ['**/*.{js,css,html,svg,png,ico,woff,woff2}'],
                // Never let the SW serve API, auth, or backend-rendered pages
                // (Swagger UI, OpenAPI doc, developer manual, actuator, h2-console)
                // from the SPA cache. The NavigationRoute scope is the entire origin,
                // so any path the SPA does not own must be explicitly excluded.
                navigateFallback: '/index.html',
                navigateFallbackDenylist: [
                    /^\/api\//,
                    /^\/oauth2\//,
                    /^\/login/,
                    /^\/logout/,
                    /^\/\.well-known\//,
                    /^\/silent-renew\.html$/,
                    /^\/swagger-ui(\/|\.html$)/,
                    /^\/v3\/api-docs/,
                    /^\/docs\//,
                    /^\/actuator\//,
                    /^\/h2-console\//,
                    /^\/error/,
                ],
                runtimeCaching: [
                    {
                        // Allow opaque/CORS responses for icons & fonts to be cached
                        urlPattern: ({request}) => request.destination === 'font',
                        handler: 'CacheFirst',
                        options: {
                            cacheName: 'klabis-fonts',
                            expiration: {maxEntries: 20, maxAgeSeconds: 60 * 60 * 24 * 365},
                        },
                    },
                ],
            },
            devOptions: {
                // Enable to test PWA install prompt on `npm run dev`
                enabled: false,
            },
        }),
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
