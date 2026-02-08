import {defineConfig} from 'vitest/config';
import react from '@vitejs/plugin-react';
import {resolve} from 'path';

export default defineConfig({
    plugins: [react()],
    test: {
        globals: true,
        environment: 'jsdom',
        setupFiles: ['./src/setupTests.ts'],
        css: true,
        include: ['src/**/*.{test,spec}.{ts,tsx}'],
        coverage: {
            provider: 'v8',
            reporter: ['text', 'json', 'html'],
            exclude: [
                'node_modules/',
                'src/setupTests.ts',
                'src/__mocks__/**',
                '**/*.d.ts',
                '**/*.config.*',
                '**/dist/**',
            ],
        },
    },
    resolve: {
        alias: {
            '~': resolve(__dirname, './src'),
            '@': resolve(__dirname, './src'),
        },
    },
});
