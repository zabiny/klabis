// Rozšíření matcherů pro DOM od @testing-library/jest-dom
import '@testing-library/jest-dom';

// Set up globalThis.__DEV__ for getApiBaseUrl()
// Vite will replace this at build time in production/dev builds,
// but in Jest tests we default to false (production-like behavior)
(globalThis as any).__DEV__ = false;

// Mock pro případné globální objekty nebo API
// global.ResizeObserver = jest.fn().mockImplementation(() => ({
//     observe: jest.fn(),
//     unobserve: jest.fn(),
//     disconnect: jest.fn(),
// }));

// Jest global setup
beforeAll(() => {
    // Globální setup před všemi testy
});

afterAll(() => {
    // Globální cleanup po všech testech
});

beforeEach(() => {
    // Setup před každým testem
    jest.clearAllMocks();
});

afterEach(() => {
    // Cleanup po každém testu
});
