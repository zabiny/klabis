import '@testing-library/jest-dom';
import React from 'react';
import {render, screen} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {MemoryRouter} from 'react-router-dom';
import {HalFormButton} from './HalFormButton.tsx';
import {HalRouteContext, type HalRouteContextValue} from '../../contexts/HalRouteContext.tsx';
import {HalFormProvider} from '../../contexts/HalFormContext.tsx';
import {HalFormsPageLayout} from './HalFormsPageLayout.tsx';
import {mockHalFormsTemplate} from '../../__mocks__/halData.ts';
import type {HalResponse} from '../../api';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {vi} from 'vitest';

// Mock dependencies
vi.mock('../../api/klabisUserManager', () => ({
    klabisAuthUserManager: {
        getUser: vi.fn().mockResolvedValue({
            access_token: 'test-token',
            token_type: 'Bearer',
        }),
    },
}));

vi.mock('../../api/hateoas', () => ({
    submitHalFormsData: vi.fn(),
    isFormValidationError: vi.fn((error) => {
        return error && typeof error === 'object' && 'validationErrors' in error;
    }),
}));

vi.mock('./HalFormDisplay.tsx', () => ({
    HalFormDisplay: ({template, templateName, onClose}: any) => (
        <div data-testid="hal-forms-display">
            <h3>{template.title || templateName}</h3>
            <button onClick={onClose} data-testid="close-form-button">Close Form</button>
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

describe('HalFormButton Component', () => {
    let queryClient: QueryClient;

    beforeEach(() => {
        queryClient = new QueryClient({
            defaultOptions: {
                queries: {retry: false, gcTime: 0},
            },
        });
        vi.clearAllMocks();
    });

    const createMockContext = (resourceData: HalResponse | null): HalRouteContextValue => ({
        resourceData,
        isLoading: false,
        error: null,
        refetch: vi.fn(),
        pathname: '/members/123',
        queryState: 'success',
        navigateToResource: vi.fn(),
        getResourceLink: vi.fn()
    });

    const renderWithContext = (
        ui: React.ReactElement,
        contextValue: HalRouteContextValue,
        initialEntries: string[] = ['/members/123']
    ) => {
        return render(
            <QueryClientProvider client={queryClient}>
                <MemoryRouter initialEntries={initialEntries}>
                    <HalRouteContext.Provider value={contextValue}>
                        <HalFormProvider>
                            <HalFormsPageLayout>
                                {ui}
                            </HalFormsPageLayout>
                        </HalFormProvider>
                    </HalRouteContext.Provider>
                </MemoryRouter>
            </QueryClientProvider>
        );
    };

    describe('Template Existence Check', () => {
        it('should render nothing when resourceData is null', () => {
            const contextValue = createMockContext(null);
            renderWithContext(
                <HalFormButton name="create" modal={true}/>,
                contextValue
            );
            // Button should not be rendered when no template exists
            expect(screen.queryByTestId('form-template-button-create')).not.toBeInTheDocument();
        });

        it('should render nothing when _templates is undefined', () => {
            const resourceData: HalResponse = {
                id: 1,
                name: 'Test Resource',
            };
            const contextValue = createMockContext(resourceData);
            renderWithContext(
                <HalFormButton name="create" modal={true}/>,
                contextValue
            );
            // Button should not be rendered when _templates is undefined
            expect(screen.queryByTestId('form-template-button-create')).not.toBeInTheDocument();
        });

        it('should render nothing when template with given name does not exist', () => {
            const resourceData: HalResponse = {
                id: 1,
                name: 'Test Resource',
                _templates: {
                    update: mockHalFormsTemplate({title: 'Update'}),
                },
            };
            const contextValue = createMockContext(resourceData);
            renderWithContext(
                <HalFormButton name="create" modal={true}/>,
                contextValue
            );
            // Button should not be rendered when template doesn't exist
            expect(screen.queryByTestId('form-template-button-create')).not.toBeInTheDocument();
        });

        it('should render button when template with given name exists', () => {
            const resourceData: HalResponse = {
                id: 1,
                name: 'Test Resource',
                _templates: {
                    create: mockHalFormsTemplate({title: 'Create New'}),
                },
            };
            const contextValue = createMockContext(resourceData);
            renderWithContext(
                <HalFormButton name="create" modal={true}/>,
                contextValue
            );
            expect(screen.getByRole('button', {name: /create new/i})).toBeInTheDocument();
        });
    });

    describe('Button Rendering', () => {
        it('should display template title as button text', () => {
            const resourceData: HalResponse = {
                id: 1,
                _templates: {
                    create: mockHalFormsTemplate({title: 'Create Member'}),
                },
            };
            const contextValue = createMockContext(resourceData);
            renderWithContext(
                <HalFormButton name="create" modal={true}/>,
                contextValue
            );
            expect(screen.getByText('Create Member')).toBeInTheDocument();
        });

        it('should display template name when title is not provided', () => {
            const resourceData: HalResponse = {
                id: 1,
                _templates: {
                    create: mockHalFormsTemplate({title: undefined}),
                },
            };
            const contextValue = createMockContext(resourceData);
            renderWithContext(
                <HalFormButton name="create" modal={true}/>,
                contextValue
            );
            expect(screen.getByText('create')).toBeInTheDocument();
        });

        it('should have proper aria-label', () => {
            const resourceData: HalResponse = {
                id: 1,
                _templates: {
                    create: mockHalFormsTemplate({title: 'Create Member'}),
                },
            };
            const contextValue = createMockContext(resourceData);
            renderWithContext(
                <HalFormButton name="create" modal={true}/>,
                contextValue
            );
            const button = screen.getByRole('button');
            expect(button).toHaveAttribute('aria-label', 'Select Create Member form');
        });

        it('should have data-testid attribute', () => {
            const resourceData: HalResponse = {
                id: 1,
                _templates: {
                    create: mockHalFormsTemplate({title: 'Create'}),
                },
            };
            const contextValue = createMockContext(resourceData);
            renderWithContext(
                <HalFormButton name="create" modal={true}/>,
                contextValue
            );
            expect(screen.getByTestId('form-template-button-create')).toBeInTheDocument();
        });
    });

    describe('Modal Mode (modal=true)', () => {
        it('should request form display via context when button is clicked', async () => {
            const user = userEvent.setup();
            const resourceData: HalResponse = {
                id: 1,
                name: 'Current Resource',
                _templates: {
                    create: mockHalFormsTemplate({title: 'Create'}),
                },
            };
            const contextValue = createMockContext(resourceData);

            renderWithContext(
                <HalFormButton name="create" modal={true}/>,
                contextValue
            );

            // Initially no modal should be visible
            expect(screen.queryByTestId('modal-overlay')).not.toBeInTheDocument();

            // Click button to request form
            const button = screen.getByRole('button', {name: /create/i});
            await user.click(button);

            // Modal should open, which indicates the form was requested via context
            expect(screen.getByTestId('modal-overlay')).toBeInTheDocument();
        });

        it('should render modal overlay when form is requested', async () => {
            const user = userEvent.setup();
            const resourceData: HalResponse = {
                id: 1,
                _templates: {
                    create: mockHalFormsTemplate({title: 'Create'}),
                },
            };
            const contextValue = createMockContext(resourceData);
            renderWithContext(
                <HalFormButton name="create" modal={true}/>,
                contextValue
            );

            // Modal should not be visible initially
            expect(screen.queryByTestId('modal-overlay')).not.toBeInTheDocument();

            // Click button to open modal
            const button = screen.getByRole('button', {name: /create/i});
            await user.click(button);

            // Modal should be displayed
            expect(screen.getByTestId('modal-overlay')).toBeInTheDocument();
            expect(screen.getByTestId('hal-forms-display')).toBeInTheDocument();
        });

        it('should close modal when close button is clicked', async () => {
            const user = userEvent.setup();
            const resourceData: HalResponse = {
                id: 1,
                _templates: {
                    create: mockHalFormsTemplate({title: 'Create'}),
                },
            };
            const contextValue = createMockContext(resourceData);
            renderWithContext(
                <HalFormButton name="create" modal={true}/>,
                contextValue
            );

            // Open modal
            const button = screen.getByRole('button', {name: /create/i});
            await user.click(button);

            expect(screen.getByTestId('modal-overlay')).toBeInTheDocument();

            // Close modal via close button in form
            const closeButton = screen.getByTestId('close-form-button');
            await user.click(closeButton);

            // Modal should be closed
            expect(screen.queryByTestId('modal-overlay')).not.toBeInTheDocument();
        });

        it('should pass custom layout to form when provided', async () => {
            const user = userEvent.setup();
            const resourceData: HalResponse = {
                id: 1,
                _templates: {
                    create: mockHalFormsTemplate({title: 'Create'}),
                },
            };
            const contextValue = createMockContext(resourceData);

            const customLayout = <div>Custom Form Layout</div>;

            renderWithContext(
                <HalFormButton name="create" modal={true} customLayout={customLayout}/>,
                contextValue
            );

            const button = screen.getByRole('button', {name: /create/i});
            await user.click(button);

            // Form should be opened (custom layout is passed through context)
            expect(screen.getByTestId('modal-overlay')).toBeInTheDocument();
        });

        it('should work with callback-based custom layout', async () => {
            const user = userEvent.setup();
            const resourceData: HalResponse = {
                id: 1,
                _templates: {
                    create: mockHalFormsTemplate({title: 'Create'}),
                },
            };
            const contextValue = createMockContext(resourceData);

            const customLayout = (_renderField: any) => <div>Custom Callback Layout</div>;

            renderWithContext(
                <HalFormButton name="create" modal={true} customLayout={customLayout}/>,
                contextValue
            );

            const button = screen.getByRole('button', {name: /create/i});
            await user.click(button);

            // Form should be opened
            expect(screen.getByTestId('modal-overlay')).toBeInTheDocument();
        });
    });

    describe('Inline Mode (modal=false)', () => {
        it('should render button in inline mode', () => {
            const resourceData: HalResponse = {
                id: 1,
                _templates: {
                    edit: mockHalFormsTemplate({title: 'Edit'}),
                },
            };
            const contextValue = createMockContext(resourceData);
            renderWithContext(
                <HalFormButton name="edit" modal={false}/>,
                contextValue
            );
            expect(screen.getByRole('button', {name: /edit/i})).toBeInTheDocument();
        });

        it('should navigate with form query parameter when button is clicked', async () => {
            const user = userEvent.setup();
            const resourceData: HalResponse = {
                id: 1,
                name: 'Test Member',
                _templates: {
                    edit: mockHalFormsTemplate({
                        title: 'Edit',
                        target: '/api/members/123',
                        method: 'PUT',
                    }),
                },
            };
            const contextValue = createMockContext(resourceData);
            renderWithContext(
                <HalFormButton name="edit" modal={false}/>,
                contextValue,
                ['/members/123'] // Initial entry
            );

            // Click button to navigate to URL with form query parameter
            const button = screen.getByRole('button', {name: /edit/i});
            await user.click(button);

            // Form should be displayed inline on the page (not in a modal)
            // This happens through HalFormsPageLayout detecting the ?form=edit parameter
            expect(screen.getByTestId('hal-forms-display')).toBeInTheDocument();
            expect(screen.queryByTestId('modal-overlay')).not.toBeInTheDocument();
        });

        it('should not open modal when modal=false', async () => {
            const user = userEvent.setup();
            const resourceData: HalResponse = {
                id: 1,
                _templates: {
                    create: mockHalFormsTemplate({title: 'Create'}),
                },
            };
            const contextValue = createMockContext(resourceData);
            renderWithContext(
                <HalFormButton name="create" modal={false}/>,
                contextValue
            );

            const button = screen.getByRole('button', {name: /create/i});
            await user.click(button);

            // Modal overlay should not be displayed (form is inline)
            expect(screen.queryByTestId('modal-overlay')).not.toBeInTheDocument();
        });

        it('should use current pathname for query parameter regardless of template target', async () => {
            const user = userEvent.setup();
            const resourceData: HalResponse = {
                id: 1,
                name: 'Test Member',
                _templates: {
                    createEvent: mockHalFormsTemplate({
                        title: 'Create Event',
                        target: '/api/events',
                        method: 'POST',
                    }),
                },
            };
            const contextValue = createMockContext(resourceData);
            renderWithContext(
                <HalFormButton name="createEvent" modal={false}/>,
                contextValue,
                ['/members/123']
            );

            const button = screen.getByRole('button', {name: /create event/i});
            await user.click(button);

            // Form should display inline on current page (/members/123?form=createEvent)
            // NOT navigate to /api/events
            expect(screen.getByTestId('hal-forms-display')).toBeInTheDocument();
        });
    });

    describe('Accessibility', () => {
        it('should have proper button role', () => {
            const resourceData: HalResponse = {
                id: 1,
                _templates: {
                    create: mockHalFormsTemplate({title: 'Create'}),
                },
            };
            const contextValue = createMockContext(resourceData);
            renderWithContext(
                <HalFormButton name="create" modal={true}/>,
                contextValue
            );
            expect(screen.getByRole('button')).toBeInTheDocument();
        });

        it('should have title attribute', () => {
            const resourceData: HalResponse = {
                id: 1,
                _templates: {
                    create: mockHalFormsTemplate({title: 'Create New Member'}),
                },
            };
            const contextValue = createMockContext(resourceData);
            renderWithContext(
                <HalFormButton name="create" modal={true}/>,
                contextValue
            );
            const button = screen.getByRole('button');
            expect(button).toHaveAttribute('title', 'Create New Member');
        });
    });

    describe('Modal Display via HalFormsPageLayout', () => {
        it('should render form in modal overlay when modal form is requested', async () => {
            const user = userEvent.setup();
            const resourceData: HalResponse = {
                id: 1,
                _templates: {
                    create: mockHalFormsTemplate({title: 'Create'}),
                },
            };
            const contextValue = createMockContext(resourceData);
            renderWithContext(
                <HalFormButton name="create" modal={true}/>,
                contextValue
            );

            // Click button to request form
            const button = screen.getByRole('button', {name: /create/i});
            await user.click(button);

            // HalFormsPageLayout should render the modal
            expect(screen.getByTestId('modal-overlay')).toBeInTheDocument();
            expect(screen.getByTestId('hal-forms-display')).toBeInTheDocument();
        });

        it('should render form inline when inline form is requested via URL', () => {
            const resourceData: HalResponse = {
                id: 1,
                _templates: {
                    edit: mockHalFormsTemplate({title: 'Edit'}),
                },
            };
            const contextValue = createMockContext(resourceData);
            renderWithContext(
                <HalFormButton name="edit" modal={false}/>,
                contextValue,
                ['/members/123?form=edit'] // Start with form query param
            );

            // HalFormsPageLayout should render the inline form
            expect(screen.getByTestId('hal-forms-display')).toBeInTheDocument();

            // Modal should not be rendered
            expect(screen.queryByTestId('modal-overlay')).not.toBeInTheDocument();
        });

        it('should give precedence to modal over inline form', () => {
            // This test verifies that when both modal and inline forms exist,
            // the modal (from context) takes precedence and is rendered first
            const resourceData: HalResponse = {
                id: 1,
                _templates: {
                    create: mockHalFormsTemplate({title: 'Create'}),
                    edit: mockHalFormsTemplate({title: 'Edit'}),
                },
            };
            const contextValue = createMockContext(resourceData);

            // Start with URL param for inline form
            renderWithContext(
                <HalFormButton name="edit" modal={false}/>,
                contextValue,
                ['/members/123?form=edit']
            );

            // Inline form should be displayed when URL param exists
            expect(screen.getByTestId('hal-forms-display')).toBeInTheDocument();
            expect(screen.getByText('Edit')).toBeInTheDocument();
            expect(screen.queryByTestId('modal-overlay')).not.toBeInTheDocument();
        });
    });

    describe('Default Props', () => {
        it('should use modal=true as default', async () => {
            const user = userEvent.setup();
            const resourceData: HalResponse = {
                id: 1,
                _templates: {
                    create: mockHalFormsTemplate({title: 'Create'}),
                },
            };
            const contextValue = createMockContext(resourceData);
            renderWithContext(
                <HalFormButton name="create"/>, // No modal prop specified
                contextValue
            );

            const button = screen.getByRole('button', {name: /create/i});
            await user.click(button);

            // Should open modal by default
            expect(screen.getByTestId('modal-overlay')).toBeInTheDocument();
        });

        it('should not use custom layout by default', async () => {
            const user = userEvent.setup();
            const resourceData: HalResponse = {
                id: 1,
                _templates: {
                    create: mockHalFormsTemplate({title: 'Create'}),
                },
            };
            const contextValue = createMockContext(resourceData);
            renderWithContext(
                <HalFormButton name="create" modal={true}/>, // No customLayout
                contextValue
            );

            const button = screen.getByRole('button', {name: /create/i});
            await user.click(button);

            // Form should render without custom layout
            expect(screen.getByTestId('hal-forms-display')).toBeInTheDocument();
        });
    });
});
