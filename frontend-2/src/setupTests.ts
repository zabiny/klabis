// Rozšíření matcherů pro DOM od @testing-library/jest-dom
import '@testing-library/jest-dom';

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
