/** @type {import('jest').Config} */
module.exports = {
    preset: 'ts-jest',
    testEnvironment: 'jsdom',
    moduleNameMapper: {
        '^~/(.*)$': '<rootDir>/src/$1',
    },
    // Spustí se před každým testem a načte rozšíření jest‑dom
    setupFilesAfterEnv: ['<rootDir>/src/setupTests.ts'],
};