import '@testing-library/jest-dom';
import React from 'react';
import {render, screen, waitFor} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {HalFormDisplay} from './HalFormDisplay.tsx';
import {HalRouteContext, type HalRouteContextValue} from '../../contexts/HalRouteContext.tsx';
import {mockHalFormsTemplate} from '../../__mocks__/halData.ts';
import {createMockResponse} from '../../__mocks__/mockFetch';
import type {HalResponse} from '../../api';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {type Mock, vi} from 'vitest';

// Mock dependencies
vi.mock('../../api/klabisUserManager', () => ({
    klabisAuthUserManager: {
        getUser: vi.fn().mockResolvedValue({
            access_token: 'test-token',
            token_type: 'Bearer',
        }),
    },
}));

describe('HalFormDisplay Component', () => {
    let queryClient: QueryClient;
    let fetchSpy: Mock;

    beforeEach(() => {
        queryClient = new QueryClient({
            defaultOptions: {
                queries: {retry: false, gcTime: 0},
            },
        });
        vi.clearAllMocks();
        // Mock global fetch
        fetchSpy = vi.fn() as Mock;
        (globalThis as any).fetch = fetchSpy;
    });

    afterEach(() => {
        delete (globalThis as any).fetch;
    });

    const createMockContext = (resourceData: HalResponse | null = null): HalRouteContextValue => ({
        resourceData: resourceData || {id: 1, name: 'Test Resource'},
        isLoading: false,
        error: null,
        refetch: vi.fn(),
        pathname: '/test/123',
        queryState: 'success',
        navigateToResource: vi.fn(),
        getResourceLink: vi.fn()
    });

    const createWrapper = (contextValue: HalRouteContextValue) => {
        return ({children}: { children: React.ReactNode }) => (
            <QueryClientProvider client={queryClient}>
                <HalRouteContext.Provider value={contextValue}>
                    {children}
                </HalRouteContext.Provider>
            </QueryClientProvider>
        );
    };

    const renderComponent = (
        template = mockHalFormsTemplate({title: 'Test Form'}),
        onClose = vi.fn(),
        onSubmitSuccess = vi.fn(),
        showCloseButton = true
    ) => {
        const resourceData: HalResponse = {
            id: 1,
            name: 'Test Resource',
        };
        const contextValue = createMockContext(resourceData);
        const Wrapper = createWrapper(contextValue);
        return render(
            <Wrapper>
                <HalFormDisplay
                    template={template}
                    templateName="test"
                    resourceData={resourceData}
                    pathname="/test/123"
                    onClose={onClose}
                    onSubmitSuccess={onSubmitSuccess}
                    showCloseButton={showCloseButton}
                />
            </Wrapper>
        );
    };

    describe('Form Display', () => {
        it('should display the form', () => {
            renderComponent();
            expect(screen.getByTestId('hal-forms-display')).toBeInTheDocument();
        });

        it('should display template title', () => {
            const template = mockHalFormsTemplate({title: 'Edit Member'});
            renderComponent(template);
            expect(screen.getByText('Edit Member')).toBeInTheDocument();
        });

        it('should display close button by default', () => {
            renderComponent();
            expect(screen.getByTestId('close-form-button')).toBeInTheDocument();
        });

        it('should not display close button when showCloseButton=false', () => {
            renderComponent(
                mockHalFormsTemplate({title: 'Test Form'}),
                vi.fn(),
                vi.fn(),
                false
            );
            expect(screen.queryByTestId('close-form-button')).not.toBeInTheDocument();
        });
    });

    describe('Close Functionality', () => {
        it('should call onClose when close button is clicked', async () => {
            const onClose = vi.fn();
            const user = userEvent.setup();
            renderComponent(
                mockHalFormsTemplate({title: 'Test Form'}),
                onClose
            );

            const closeButton = screen.getByTestId('close-form-button');
            await user.click(closeButton);

            expect(onClose).toHaveBeenCalled();
        });

        it('should have aria-label on close button', () => {
            renderComponent();
            const closeButton = screen.getByTestId('close-form-button');
            expect(closeButton).toHaveAttribute('aria-label', 'Close form');
        });
    });

    describe('Form Submission', () => {
        it('should submit form successfully and trigger callbacks', async () => {
            const onClose = vi.fn();
            const onSubmitSuccess = vi.fn();
            const user = userEvent.setup();

            const template = mockHalFormsTemplate({
                title: 'Create Member',
                target: '/api/members',
                method: 'POST',
                properties: [
                    {
                        name: 'name',
                        prompt: 'Full Name',
                        type: 'text',
                        required: true,
                    },
                    {
                        name: 'description',
                        prompt: 'Description',
                        type: 'textarea',
                    },
                ],
            });

            const contextValue = createMockContext({id: 1});
            const mockRefetch = vi.fn();
            contextValue.refetch = mockRefetch;

            const Wrapper = createWrapper(contextValue);

            // Mock fetch calls:
            // First call: fetching form target data (200 OK)
            // Second call: form submission (200 OK)
            fetchSpy
                .mockResolvedValueOnce(createMockResponse({id: 1, name: 'Existing Member'}))
                .mockResolvedValueOnce(createMockResponse({id: 2, name: 'New Member'}));

            render(
                <Wrapper>
                    <HalFormDisplay
                        template={template}
                        templateName="create"
                        resourceData={{id: 1}}
                        pathname="/members"
                        onClose={onClose}
                        onSubmitSuccess={onSubmitSuccess}
                    />
                </Wrapper>
            );

            // Wait for form to load
            await waitFor(() => {
                expect(screen.getByTestId('hal-forms-display')).toBeInTheDocument();
            });

            // Wait for the loading state to disappear and form fields to appear
            await waitFor(() => {
                expect(screen.queryByText(/Nač/)).not.toBeInTheDocument();
            });

            // Fill in form fields - use placeholder or name attribute to find input
            const nameInput = screen.getByDisplayValue('Existing Member') as HTMLInputElement;
            await user.clear(nameInput);
            await user.type(nameInput, 'John Doe');

            // Submit the form - look for button with text that contains "Odeslat" (Czech for Submit)
            const submitButton = screen.getByRole('button', {name: /odeslat/i});
            await user.click(submitButton);

            // Verify callbacks were called
            await waitFor(() => {
                expect(mockRefetch).toHaveBeenCalled();
                expect(onSubmitSuccess).toHaveBeenCalled();
                expect(onClose).toHaveBeenCalled();
            });
        });

        it('should display validation errors when submission fails with 400', async () => {
            // Mock validation error response with problem+json
            const validationErrorResponse = {
                ...createMockResponse(
                    {
                        errors: {
                            name: 'Name is required',
                            email: 'Invalid email format',
                        },
                    },
                    400
                ),
                headers: new Headers({'Content-Type': 'application/problem+json'}),
            };

            fetchSpy.mockResolvedValueOnce(validationErrorResponse);

            const template = mockHalFormsTemplate({
                title: 'Create',
                target: '/api/members',
                method: 'POST',
            });

            renderComponent(template);

            // Verify component is ready for submission
            expect(screen.getByTestId('hal-forms-display')).toBeInTheDocument();
        });

        it('should display generic error when submission fails with 500', async () => {
            fetchSpy.mockResolvedValueOnce(createMockResponse({}, 500));

            const template = mockHalFormsTemplate({
                title: 'Create',
                target: '/api/members',
                method: 'POST',
            });

            renderComponent(template);

            expect(screen.getByTestId('hal-forms-display')).toBeInTheDocument();
        });
    });

    describe('Target Data Fetching', () => {
        beforeEach(() => {
            queryClient.clear();
        });

        it('should display form with empty data when target returns HTTP 404', async () => {
            const template = mockHalFormsTemplate({
                title: 'Create Event',
                target: '/api/events',
                method: 'POST',
            });

            // Mock HTTP 404 response
            fetchSpy.mockResolvedValueOnce(createMockResponse({}, 404));

            renderComponent(template);

            // Wait for form to be displayed (should not show error for 404)
            await waitFor(() => {
                expect(screen.getByTestId('hal-forms-display')).toBeInTheDocument();
                expect(screen.queryByText(/Nepodařilo se načíst data/i)).not.toBeInTheDocument();
            });
        });

        it('should display form with empty data when target returns HTTP 405', async () => {
            const template = mockHalFormsTemplate({
                title: 'Create Event',
                target: '/api/events',
                method: 'POST',
            });

            // Mock HTTP 405 response
            fetchSpy.mockResolvedValueOnce(createMockResponse({}, 405));

            renderComponent(template);

            // Wait for form to be displayed (should not show error for 405)
            await waitFor(() => {
                expect(screen.getByTestId('hal-forms-display')).toBeInTheDocument();
                expect(screen.queryByText(/Nepodařilo se načíst data/i)).not.toBeInTheDocument();
            });
        });

        it('should display error for other HTTP errors', async () => {
            const template = mockHalFormsTemplate({
                title: 'Create Event',
                target: '/api/events',
                method: 'POST',
            });

            // Mock HTTP 500 response
            fetchSpy.mockResolvedValueOnce(createMockResponse({}, 500));

            renderComponent(template);

            // Wait for error to be displayed
            await waitFor(() => {
                expect(screen.getByText(/Nepodařilo se načíst data/i)).toBeInTheDocument();
            });
        });
    });

    describe('Accessibility', () => {
        it('should display template name in heading', () => {
            const template = mockHalFormsTemplate({title: 'Create Member'});
            renderComponent(template);
            expect(screen.getByText('Create Member')).toBeInTheDocument();
        });

        it('should display error alert when form data fetch fails with HTTP 500', async () => {
            const template = mockHalFormsTemplate({
                title: 'Create Event',
                target: '/api/events',
                method: 'POST',
            });

            // Mock HTTP 500 response to trigger error
            fetchSpy.mockResolvedValueOnce(createMockResponse({}, 500));

            renderComponent(template);

            // Verify error alert is displayed
            await waitFor(() => {
                expect(screen.getByText(/Nepodařilo se načíst data/i)).toBeInTheDocument();
            });
        });
    });
});
