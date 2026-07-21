import {defineConfig} from 'vite'
import react from '@vitejs/plugin-react'
import dts from 'vite-plugin-dts'
import {resolve} from 'node:path'

// https://vite.dev/config/
export default defineConfig({
    plugins: [
        react(),
        dts({
            include: ['src'],
            rollupTypes: true,
        }),
    ],
    build: {
        lib: {
            entry: resolve(import.meta.dirname, 'src/index.ts'),
            formats: ['es'],
            fileName: 'index',
        },
        rollupOptions: {
            external: ['react', 'react-dom', 'react/jsx-runtime'],
        },
        sourcemap: true,
    },
})
