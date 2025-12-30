/** @type {import('jest').Config} */
export default {
    preset: 'ts-jest',
    testEnvironment: 'jsdom',
    moduleNameMapper: {
        '^~/(.*)$': '<rootDir>/src/$1',
    },
    transform: {
        '^.+\\.tsx?$': ['ts-jest', {
            tsconfig: {
                jsx: 'react-jsx',
                esModuleInterop: true,
                allowSyntheticDefaultImports: true,
                allowImportingTsExtensions: true,
                skipLibCheck: true,
                noImplicitAny: false,
                types: ['jest', '@testing-library/jest-dom', 'node'],
                target: 'ES2022',
                lib: ['ES2022', 'DOM', 'DOM.Iterable'],
                module: 'commonjs',
            },
        }],
    },
    setupFilesAfterEnv: ['<rootDir>/src/setupTests.ts'],
};