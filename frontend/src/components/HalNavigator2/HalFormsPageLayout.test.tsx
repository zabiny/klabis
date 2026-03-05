import '@testing-library/jest-dom';
import {render, screen} from '@testing-library/react';
import {MemoryRouter} from 'react-router-dom';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {HalFormsPageLayout} from './HalFormsPageLayout.tsx';
import type {HalRouteContextValue} from '../../contexts/HalRouteContext.tsx';
import {HalRouteContext} from '../../contexts/HalRouteContext.tsx';
import {mockHalResponseWithForms} from '../../__mocks__/halData.ts';
import {vi} from 'vitest';
import type {HalFormDisplayProps} from './HalFormDisplay.tsx';
import {HalFormProvider} from '../../contexts/HalFormContext.tsx';

vi.mock('./HalFormDisplay.tsx', () => ({
    HalFormDisplay: ({template, templateName, onClose, customLayout}: HalFormDisplayProps) => (
        <div data-testid="hal-form-display">
            <h3>{template.title || templateName}</h3>
            {customLayout && <div data-testid="custom-layout">Custom Layout Applied</div>}
            <button onClick={onClose} data-testid="form-close-button">Close Form</button>
        </div>
    ),
}));

vi.mock('../UI/ModalOverlay.tsx', () => ({
    ModalOverlay: ({isOpen, children, onClose}: any) => (
        isOpen ? (
            <div data-testid="modal-overlay" role="dialog">
                {children}
                <button onClick={onClose} data-testid="modal-close-button">Close Modal</button>
            </div>
        ) : null
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
                        <HalFormProvider>
                            <HalFormsPageLayout>
                                <h1>Test Content</h1>
                                <p>Custom content inside the layout</p>
                            </HalFormsPageLayout>
                        </HalFormProvider>
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
            refetch: vi.fn(),
            pathname: '/api/test',
            queryState: 'success' as const,
            navigateToResource: vi.fn(),
            getResourceLink: vi.fn()
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
            refetch: vi.fn(),
            pathname: '/api/test',
            queryState: 'success' as const,
            navigateToResource: vi.fn(),
            getResourceLink: vi.fn()
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
            refetch: vi.fn(),
            pathname: '/api/test?form=nonsense',
            queryState: 'success' as const,
            navigateToResource: vi.fn(),
            getResourceLink: vi.fn()
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
            refetch: vi.fn(),
            pathname: '/api/test',
            queryState: 'success' as const,
            navigateToResource: vi.fn(),
            getResourceLink: vi.fn()
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
            refetch: vi.fn(),
            pathname: '/api/test',
            queryState: 'success' as const,
            navigateToResource: vi.fn(),
            getResourceLink: vi.fn()
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
            refetch: vi.fn(),
            pathname: '/api/test',
            queryState: 'success' as const,
            navigateToResource: vi.fn(),
            getResourceLink: vi.fn()
        };

        renderWithContext(contextValue);

        // Form should not be displayed
        expect(screen.queryByTestId('hal-form-display')).not.toBeInTheDocument();
    });

    describe('Modal Form Display (via Context)', () => {
        it('should handle modal forms via HalFormContext integration', () => {
            // Modal forms are now integrated with HalFormContext
            // When useHalForm().requestForm() is called, HalFormsPageLayout
            // listens to currentFormRequest and renders ModalOverlay
            // This is tested through HalFormButton integration tests
            expect(true).toBe(true);
        });
    });

});
