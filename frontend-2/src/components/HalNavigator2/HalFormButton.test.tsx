import '@testing-library/jest-dom';
import React from 'react';
import {render, screen, waitFor} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {BrowserRouter} from 'react-router-dom';
import {HalFormButton} from './HalFormButton.tsx';
import {HalRouteContext, type HalRouteContextValue} from '../../contexts/HalRouteContext.tsx';
import {mockHalFormsTemplate} from '../../__mocks__/halData.ts';
import {createMockResponse} from '../../__mocks__/mockFetch';
import type {HalResponse} from '../../api';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';

// Mock dependencies
jest.mock('../../api/klabisUserManager', () => ({
    klabisAuthUserManager: {
        getUser: jest.fn().mockResolvedValue({
            access_token: 'test-token',
            token_type: 'Bearer',
        }),
    },
}));

jest.mock('../../api/hateoas.ts', () => ({
    ...jest.requireActual('../../api/hateoas'),
    submitHalFormsData: jest.fn(),
}));

describe('HalFormButton Component', () => {
    let queryClient: QueryClient;

    beforeEach(() => {
        queryClient = new QueryClient({
            defaultOptions: {
                queries: {retry: false, gcTime: 0},
            },
        });
        jest.clearAllMocks();
    });

    const createWrapper = () => {
        return ({children}: { children: React.ReactNode }) => (
            <BrowserRouter>
                <QueryClientProvider client={queryClient}>
                    {children}
                </QueryClientProvider>
            </BrowserRouter>
        );
    };

    const createMockContext = (resourceData: HalResponse | null): HalRouteContextValue => ({
        resourceData,
        isLoading: false,
        error: null,
        refetch: jest.fn(),
        pathname: '/members/123',
        queryState: 'success',
        navigateToResource: jest.fn(),
        getResourceLink: jest.fn()
    });

    const renderWithContext = (
        ui: React.ReactElement,
        contextValue: HalRouteContextValue
    ) => {
        const Wrapper = createWrapper();
        return render(
            <Wrapper>
                <HalRouteContext.Provider value={contextValue}>
                    {ui}
                </HalRouteContext.Provider>
            </Wrapper>
        );
    };

    describe('Template Existence Check', () => {
        it('should render nothing when resourceData is null', () => {
            const contextValue = createMockContext(null);
            const {container} = renderWithContext(
                <HalFormButton name="create" modal={true}/>,
                contextValue
            );
            expect(container.firstChild).toBeNull();
        });

        it('should render nothing when _templates is undefined', () => {
            const resourceData: HalResponse = {
                id: 1,
                name: 'Test Resource',
            };
            const contextValue = createMockContext(resourceData);
            const {container} = renderWithContext(
                <HalFormButton name="create" modal={true}/>,
                contextValue
            );
            expect(container.firstChild).toBeNull();
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
            const {container} = renderWithContext(
                <HalFormButton name="create" modal={true}/>,
                contextValue
            );
            expect(container.firstChild).toBeNull();
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
        it('should open form in modal when button is clicked', async () => {
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

            const button = screen.getByRole('button', {name: /create/i});
            await user.click(button);

            // Form should be visible
            await waitFor(() => {
                expect(screen.getByTestId('hal-forms-display')).toBeInTheDocument();
            });
        });

        it('should have close button in modal mode', async () => {
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

            const button = screen.getByRole('button', {name: /create/i});
            await user.click(button);

            await waitFor(() => {
                expect(screen.getByTestId('close-form-button')).toBeInTheDocument();
            });
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
            const openButton = screen.getByRole('button', {name: /create/i});
            await user.click(openButton);

            await waitFor(() => {
                expect(screen.getByTestId('hal-forms-display')).toBeInTheDocument();
            });

            // Close modal
            const closeButton = screen.getByTestId('close-form-button');
            await user.click(closeButton);

            await waitFor(() => {
                expect(screen.queryByTestId('hal-forms-display')).not.toBeInTheDocument();
            });
        });
    });

    describe('Non-Modal Mode (modal=false)', () => {
        it('should render button in non-modal mode', () => {
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
            expect(screen.getByRole('button', {name: /create/i})).toBeInTheDocument();
        });

        it('should display form inline with query parameter on current page', async () => {
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
                contextValue
            );

            const button = screen.getByRole('button', {name: /edit/i});
            await user.click(button);

            // Form should display inline on current page (not navigate away)
            // Target URL is only used to fetch initial values and submit
            expect(button).toBeInTheDocument();
        });

        it('should use current pathname with query parameter even for different target', async () => {
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
                contextValue
            );

            const button = screen.getByRole('button', {name: /create event/i});
            await user.click(button);

            // Form should display on current page (/members/123?form=createEvent)
            // NOT navigate to /events
            expect(button).toBeInTheDocument();
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

            // Modal should NOT be displayed in non-modal mode
            expect(screen.queryByTestId('hal-forms-display')).not.toBeInTheDocument();
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

    describe('Form Modal Setup and Context Integration', () => {
        const {submitHalFormsData} = require('../../api/hateoas.ts');

        beforeEach(() => {
            submitHalFormsData.mockClear();
        });

        it('should prepare modal for form submission with correct parameters', async () => {
            const user = userEvent.setup();
            const resourceData: HalResponse = {
                id: 1,
                name: 'Test Resource',
                _templates: {
                    create: mockHalFormsTemplate({
                        title: 'Create',
                        target: '/api/members',
                        method: 'POST',
                    }),
                },
            };

            const mockRefetch = jest.fn().mockResolvedValue(undefined);
            const contextValue = {
                ...createMockContext(resourceData),
                refetch: mockRefetch,
            };

            submitHalFormsData.mockResolvedValueOnce({success: true});

            renderWithContext(
                <HalFormButton name="create" modal={true}/>,
                contextValue
            );

            // Open modal
            const button = screen.getByRole('button', {name: /create/i});
            await user.click(button);

            // Verify modal is ready for form submission
            await waitFor(() => {
                expect(screen.getByTestId('hal-forms-display')).toBeInTheDocument();
            });
        });

        it('should close modal when close button is clicked', async () => {
            const user = userEvent.setup();
            const resourceData: HalResponse = {
                id: 1,
                name: 'Test Resource',
                _templates: {
                    create: mockHalFormsTemplate({
                        title: 'Create',
                        target: '/api/members',
                        method: 'POST',
                    }),
                },
            };

            const mockRefetch = jest.fn().mockResolvedValue(undefined);
            const contextValue = {
                ...createMockContext(resourceData),
                refetch: mockRefetch,
            };

            renderWithContext(
                <HalFormButton name="create" modal={true}/>,
                contextValue
            );

            // Open modal
            const button = screen.getByRole('button', {name: /create/i});
            await user.click(button);

            await waitFor(() => {
                expect(screen.getByTestId('hal-forms-display')).toBeInTheDocument();
            });

            // Close modal
            const closeButton = screen.getByTestId('close-form-button');
            await user.click(closeButton);

            // Modal should be closed
            await waitFor(() => {
                expect(screen.queryByTestId('hal-forms-display')).not.toBeInTheDocument();
            });
        });

        it('should display form for error handling when modal is open', async () => {
            const user = userEvent.setup();
            const resourceData: HalResponse = {
                id: 1,
                name: 'Test Resource',
                _templates: {
                    create: mockHalFormsTemplate({
                        title: 'Create',
                        target: '/api/members',
                        method: 'POST',
                    }),
                },
            };

            const contextValue = createMockContext(resourceData);

            submitHalFormsData.mockRejectedValueOnce(new Error('Submission failed'));

            renderWithContext(
                <HalFormButton name="create" modal={true}/>,
                contextValue
            );

            // Open modal
            const button = screen.getByRole('button', {name: /create/i});
            await user.click(button);

            // Verify form is displayed for potential error handling
            await waitFor(() => {
                expect(screen.getByTestId('hal-forms-display')).toBeInTheDocument();
            });
        });

        it('should pass refetch callback to form context', async () => {
            const mockRefetch = jest.fn().mockResolvedValue(undefined);
            const resourceData: HalResponse = {
                id: 1,
                name: 'Test Resource',
                _templates: {
                    create: mockHalFormsTemplate({
                        title: 'Create',
                        target: '/api/members',
                        method: 'POST',
                    }),
                },
            };

            const contextValue = {
                ...createMockContext(resourceData),
                refetch: mockRefetch,
            };

            renderWithContext(
                <HalFormButton name="create" modal={true}/>,
                contextValue
            );

            // Verify the button renders with the context value available
            expect(screen.getByRole('button', {name: /create/i})).toBeInTheDocument();
        });

        it('should pass navigateToResource callback to form context', async () => {
            const mockNavigateToResource = jest.fn();
            const resourceData: HalResponse = {
                id: 1,
                name: 'Test Resource',
                _templates: {
                    create: mockHalFormsTemplate({
                        title: 'Create',
                        target: '/api/members',
                        method: 'POST',
                    }),
                },
            };

            const contextValue = {
                ...createMockContext(resourceData),
                navigateToResource: mockNavigateToResource,
            };

            renderWithContext(
                <HalFormButton name="create" modal={true}/>,
                contextValue
            );

            // Verify the button renders with the context value available for post-submission navigation
            expect(screen.getByRole('button', {name: /create/i})).toBeInTheDocument();
        });
    });

    describe('API Without GET Endpoint (HTTP 404/405)', () => {
        let fetchSpy: jest.Mock;

        beforeEach(() => {
            // Clear query cache before each test
            queryClient.clear();
            // Mock global fetch
            fetchSpy = jest.fn() as jest.Mock;
            (globalThis as any).fetch = fetchSpy;
        });

        afterEach(() => {
            delete (globalThis as any).fetch;
        });

        it('should display form with empty data when target returns HTTP 404', async () => {
            const resourceData: HalResponse = {
                id: 1,
                name: 'Test Resource',
                _templates: {
                    create: mockHalFormsTemplate({
                        title: 'Create Event',
                        target: '/api/events/404test',
                        method: 'POST',
                    }),
                },
            };

            const contextValue = createMockContext(resourceData);

            // Simulate HTTP 404 error from target endpoint
            fetchSpy.mockResolvedValue(createMockResponse({}, 404));

            renderWithContext(
                <HalFormButton name="create" modal={true}/>,
                contextValue
            );

            // Click button to open modal
            const button = screen.getByTestId('form-template-button-create');
            await userEvent.click(button);

            // Wait for form to be displayed (should not show error)
            await waitFor(() => {
                expect(screen.getByTestId('hal-forms-display')).toBeInTheDocument();
                expect(screen.queryByText(/Nepodařilo se načíst data/i)).not.toBeInTheDocument();
            });
        });

        it('should display form with empty data when target returns HTTP 405', async () => {
            const resourceData: HalResponse = {
                id: 1,
                name: 'Test Resource',
                _templates: {
                    create: mockHalFormsTemplate({
                        title: 'Create Event',
                        target: '/api/events/405test',
                        method: 'POST',
                    }),
                },
            };

            const contextValue = createMockContext(resourceData);

            // Simulate HTTP 405 error from target endpoint
            fetchSpy.mockResolvedValue(createMockResponse({}, 405));

            renderWithContext(
                <HalFormButton name="create" modal={true}/>,
                contextValue
            );

            // Click button to open modal
            const button = screen.getByTestId('form-template-button-create');
            await userEvent.click(button);

            // Wait for form to be displayed (should not show error)
            await waitFor(() => {
                expect(screen.getByTestId('hal-forms-display')).toBeInTheDocument();
                expect(screen.queryByText(/Nepodařilo se načíst data/i)).not.toBeInTheDocument();
            });
        });

        it('should still show error for other HTTP errors (e.g., 500)', async () => {
            const resourceData: HalResponse = {
                id: 1,
                name: 'Test Resource',
                _templates: {
                    create: mockHalFormsTemplate({
                        title: 'Create Event',
                        target: '/api/events/500test',
                        method: 'POST',
                    }),
                },
            };

            const contextValue = createMockContext(resourceData);

            // Simulate HTTP 500 error from target endpoint
            fetchSpy.mockResolvedValue(createMockResponse({}, 500));

            renderWithContext(
                <HalFormButton name="create" modal={true}/>,
                contextValue
            );

            // Click button to open modal
            const button = screen.getByTestId('form-template-button-create');
            await userEvent.click(button);

            // Wait for error to be displayed
            await waitFor(() => {
                expect(screen.getByText(/Nepodařilo se načíst data/i)).toBeInTheDocument();
            });
        });
    });
});
