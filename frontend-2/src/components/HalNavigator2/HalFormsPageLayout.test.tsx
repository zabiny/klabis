import '@testing-library/jest-dom';
import {render, screen} from '@testing-library/react';
import {MemoryRouter} from 'react-router-dom';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {HalFormsPageLayout} from './HalFormsPageLayout.tsx';
import type {HalRouteContextValue} from '../../contexts/HalRouteContext.tsx';
import {HalRouteContext} from '../../contexts/HalRouteContext.tsx';
import {mockHalResponseWithForms} from '../../__mocks__/halData.ts';

jest.mock('./HalFormDisplay.tsx', () => ({
    HalFormDisplay: ({template, templateName, onClose}: any) => (
        <div data-testid="hal-form-display">
            <h3>{template.title || templateName}</h3>
            <button onClick={onClose} data-testid="form-close-button">Close Form</button>
        </div>
    ),
}));

describe('HalFormsPageLayout', () => {
    let queryClient: QueryClient;

    beforeEach(() => {
        queryClient = new QueryClient({
            defaultOptions: {
                queries: {retry: false, gcTime: 0},
            },
        });
    });

    const renderWithContext = (
        contextValue: HalRouteContextValue,
        initialEntries: string[] = ['/api/test']
    ) => {
        return render(
            <QueryClientProvider client={queryClient}>
                <MemoryRouter initialEntries={initialEntries}>
                    <HalRouteContext.Provider value={contextValue}>
                        <HalFormsPageLayout>
                            <h1>Test Content</h1>
                            <p>Custom content inside the layout</p>
                        </HalFormsPageLayout>
                    </HalRouteContext.Provider>
                </MemoryRouter>
            </QueryClientProvider>
        );
    };

    it('should render children content when no form parameter is found', () => {
        const contextValue = {
            resourceData: mockHalResponseWithForms(),
            isLoading: false,
            error: null,
            refetch: jest.fn(),
            pathname: '/api/test',
            queryState: 'success' as const,
        };

        renderWithContext(contextValue);

        expect(screen.getByText('Test Content')).toBeInTheDocument();
        expect(screen.getByText('Custom content inside the layout')).toBeInTheDocument();
    });

    it('should NOT render children content when existing form parameter is present', () => {
        const contextValue = {
            resourceData: mockHalResponseWithForms(),
            isLoading: false,
            error: null,
            refetch: jest.fn(),
            pathname: '/api/test',
            queryState: 'success' as const,
        };

        renderWithContext(contextValue, ['/api/test?form=create']);

        expect(screen.queryByText('Test Content')).not.toBeInTheDocument();
        expect(screen.queryByText('Custom content inside the layout')).not.toBeInTheDocument();
    });

    it('should render children content when form parameter references non-existent form', () => {
        const contextValue = {
            resourceData: mockHalResponseWithForms(),
            isLoading: false,
            error: null,
            refetch: jest.fn(),
            pathname: '/api/test?form=nonsense',
            queryState: 'success' as const,
        };

        renderWithContext(contextValue);

        expect(screen.getByText('Test Content')).toBeInTheDocument();
        expect(screen.getByText('Custom content inside the layout')).toBeInTheDocument();
    });

    it('should display form when query parameter for existing form is present', () => {
        const contextValue = {
            resourceData: mockHalResponseWithForms(),
            isLoading: false,
            error: null,
            refetch: jest.fn(),
            pathname: '/api/test',
            queryState: 'success' as const,
        };

        renderWithContext(contextValue, ['/api/test?form=create']);

        // Form should be displayed
        expect(screen.queryByTestId('hal-form-display')).toBeInTheDocument();
    });

    it('should NOT display form when no query parameter is present', () => {
        const contextValue = {
            resourceData: mockHalResponseWithForms(),
            isLoading: false,
            error: null,
            refetch: jest.fn(),
            pathname: '/api/test',
            queryState: 'success' as const,
        };

        renderWithContext(contextValue);

        // Form should not be displayed
        expect(screen.queryByTestId('hal-form-display')).not.toBeInTheDocument();
    });

    it('should not display form when resource data is missing', () => {
        const contextValue = {
            resourceData: null,
            isLoading: false,
            error: null,
            refetch: jest.fn(),
            pathname: '/api/test',
            queryState: 'success' as const,
        };

        renderWithContext(contextValue);

        // Form should not be displayed
        expect(screen.queryByTestId('hal-form-display')).not.toBeInTheDocument();
    });

});
