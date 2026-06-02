import '@testing-library/jest-dom';
import React from 'react';
import {render, screen} from '@testing-library/react';
import {act} from '@testing-library/react';
import {MemoryRouter} from 'react-router-dom';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {HalFormsPageLayout} from './HalFormsPageLayout.tsx';
import type {HalRouteContextValue} from '../../contexts/HalRouteContext.tsx';
import {HalRouteContext} from '../../contexts/HalRouteContext.tsx';
import {mockHalResponseWithForms} from '../../__mocks__/halData.ts';
import {vi} from 'vitest';
import type {HalFormDisplayProps} from './HalFormDisplay.tsx';
import type {HalFormPanelProps} from './HalFormPanel.tsx';
import {HalFormProvider, useHalForm} from '../../contexts/HalFormContext.tsx';

vi.mock('./HalFormDisplay.tsx', () => ({
    HalFormDisplay: ({template, templateName, onClose}: HalFormDisplayProps) => (
        <div data-testid="hal-form-display">
            <h3>{template.title || templateName}</h3>
            <button onClick={onClose} data-testid="form-close-button">Close Form</button>
        </div>
    ),
}));

vi.mock('./HalFormPanel.tsx', () => ({
    HalFormPanel: ({templateName, children, onCancel}: HalFormPanelProps) => {
        const helpers = {
            renderInput: (name: string) => <input key={name} data-testid={`input-${name}`}/>,
            renderField: (name: string) => <div key={name} data-testid={`field-${name}`}/>,
            renderLabel: (name: string) => name,
            hasField: () => true,
            hasType: () => false,
        };
        return (
            <div data-testid="hal-form-panel">
                <h3>{templateName}</h3>
                {children(helpers)}
                <button onClick={onCancel} data-testid="panel-cancel-button">Cancel</button>
            </div>
        );
    },
}));

vi.mock('../UI/Modal.tsx', () => ({
    Modal: ({isOpen, children, onClose, title}: {isOpen: boolean; children: React.ReactNode; onClose: () => void; title?: string}) => (
        isOpen ? (
            <div data-testid="modal-overlay" role="dialog">
                {title && <h4 data-testid="modal-overlay-title">{title}</h4>}
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

    const baseContextValue = {
        resourceData: mockHalResponseWithForms(),
        isLoading: false,
        error: null,
        refetch: vi.fn(),
        pathname: '/api/test',
        queryState: 'success' as const,
        navigateToResource: vi.fn(),
        getResourceLink: vi.fn(),
    };

    it('should render children content when no form is requested', () => {
        renderWithContext(baseContextValue);

        expect(screen.getByText('Test Content')).toBeInTheDocument();
        expect(screen.getByText('Custom content inside the layout')).toBeInTheDocument();
    });

    it('should not display form when no form is requested', () => {
        renderWithContext(baseContextValue);

        expect(screen.queryByTestId('hal-form-display')).not.toBeInTheDocument();
        expect(screen.queryByTestId('hal-form-panel')).not.toBeInTheDocument();
    });

    it('should not display form when resource data is missing', () => {
        const contextValue = {...baseContextValue, resourceData: null};
        renderWithContext(contextValue);

        expect(screen.queryByTestId('hal-form-display')).not.toBeInTheDocument();
        expect(screen.queryByTestId('hal-form-panel')).not.toBeInTheDocument();
    });

    it('should render children content when form references non-existent template', () => {
        const contextValue = {...baseContextValue, resourceData: mockHalResponseWithForms()};
        renderWithContext(contextValue);

        expect(screen.getByText('Test Content')).toBeInTheDocument();
    });

    describe('Modal Form Display (via Context)', () => {
        it('should render modal when modal form is requested via context', async () => {
            const TriggerModal = () => {
                const {displayHalForm} = useHalForm();
                return (
                    <button
                        onClick={() => displayHalForm({templateName: 'create', modal: true})}
                        data-testid="trigger"
                    >
                        Open Modal
                    </button>
                );
            };

            render(
                <QueryClientProvider client={queryClient}>
                    <MemoryRouter initialEntries={['/api/test']}>
                        <HalRouteContext.Provider value={baseContextValue}>
                            <HalFormProvider>
                                <TriggerModal/>
                                <HalFormsPageLayout>
                                    <h1>Test Content</h1>
                                </HalFormsPageLayout>
                            </HalFormProvider>
                        </HalRouteContext.Provider>
                    </MemoryRouter>
                </QueryClientProvider>
            );

            act(() => {
                screen.getByTestId('trigger').click();
            });

            expect(screen.getByTestId('modal-overlay')).toBeInTheDocument();
            expect(screen.queryByTestId('hal-form-panel')).not.toBeInTheDocument();
        });
    });

    describe('Inline Form Display (via Context)', () => {
        it('should render HalFormPanel when inline form is requested via context', async () => {
            const TriggerInline = () => {
                const {displayHalForm} = useHalForm();
                return (
                    <button
                        onClick={() => displayHalForm({
                            templateName: 'create',
                            modal: false,
                            children: ({renderField}) => <div>{renderField('submit')}</div>,
                        })}
                        data-testid="trigger"
                    >
                        Open Inline
                    </button>
                );
            };

            render(
                <QueryClientProvider client={queryClient}>
                    <MemoryRouter initialEntries={['/api/test']}>
                        <HalRouteContext.Provider value={baseContextValue}>
                            <HalFormProvider>
                                <TriggerInline/>
                                <HalFormsPageLayout>
                                    <h1>Test Content</h1>
                                </HalFormsPageLayout>
                            </HalFormProvider>
                        </HalRouteContext.Provider>
                    </MemoryRouter>
                </QueryClientProvider>
            );

            act(() => {
                screen.getByTestId('trigger').click();
            });

            expect(screen.getByTestId('hal-form-panel')).toBeInTheDocument();
            expect(screen.queryByTestId('modal-overlay')).not.toBeInTheDocument();
            // Page content hidden when inline form is active
            expect(screen.queryByText('Test Content')).not.toBeInTheDocument();
        });

        it('should fall back to page content when inline request has no children', async () => {
            const TriggerInlineNoChildren = () => {
                const {displayHalForm} = useHalForm();
                return (
                    <button
                        onClick={() => displayHalForm({templateName: 'create', modal: false})}
                        data-testid="trigger"
                    >
                        Open Inline
                    </button>
                );
            };

            render(
                <QueryClientProvider client={queryClient}>
                    <MemoryRouter initialEntries={['/api/test']}>
                        <HalRouteContext.Provider value={baseContextValue}>
                            <HalFormProvider>
                                <TriggerInlineNoChildren/>
                                <HalFormsPageLayout>
                                    <h1>Test Content</h1>
                                </HalFormsPageLayout>
                            </HalFormProvider>
                        </HalRouteContext.Provider>
                    </MemoryRouter>
                </QueryClientProvider>
            );

            act(() => {
                screen.getByTestId('trigger').click();
            });

            // When inline request has no children, it falls back to HalFormDisplay (default form layout)
            expect(screen.getByTestId('hal-form-display')).toBeInTheDocument();
            expect(screen.queryByText('Test Content')).not.toBeInTheDocument();
        });
    });
});
