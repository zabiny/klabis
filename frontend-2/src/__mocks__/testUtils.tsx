import React, {type ReactElement} from 'react';
import {render, type RenderOptions} from '@testing-library/react';
import {BrowserRouter} from 'react-router-dom';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';

/**
 * Create a test query client with appropriate settings for testing
 */
export const createTestQueryClient = () =>
    new QueryClient({
        defaultOptions: {
            queries: {
                retry: false,
                gcTime: 0, // Disable garbage collection between tests
            },
        },
    });

/**
 * All providers wrapper for component tests
 */
interface AllProvidersProps {
    children: React.ReactNode;
}

export const AllProviders = ({children}: AllProvidersProps) => {
    const testQueryClient = createTestQueryClient();

    return (
        <QueryClientProvider client={testQueryClient}>
            <BrowserRouter>
                {children}
            </BrowserRouter>
        </QueryClientProvider>
    );
};

/**
 * Custom render function that includes providers
 */
export const renderWithProviders = (
    ui: ReactElement,
    options?: Omit<RenderOptions, 'wrapper'>,
) =>
    render(ui, {wrapper: AllProviders, ...options});

/**
 * Helper to wait for promises to resolve (for async operations)
 */
export const waitForAsync = () =>
    new Promise((resolve) => setTimeout(resolve, 0));

/**
 * Mock fetch for API calls
 */
export const mockFetch = (response: any = {}, options?: { status?: number; ok?: boolean }) => {
    global.fetch = jest.fn(() =>
        Promise.resolve({
            ok: options?.ok ?? true,
            status: options?.status ?? 200,
            json: () => Promise.resolve(response),
            text: () => Promise.resolve(JSON.stringify(response)),
        } as Response),
    );
};

export const resetFetch = () => {
    jest.clearAllMocks();
};
