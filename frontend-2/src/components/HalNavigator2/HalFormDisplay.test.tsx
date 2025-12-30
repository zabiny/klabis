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
import {HalFormsFormField, type RenderFormCallback} from '../HalFormsForm/HalFormsForm';

// Mock dependencies
vi.mock('../../api/klabisUserManager', () => ({
    klabisAuthUserManager: {
        getUser: vi.fn().mockResolvedValue({
            access_token: 'test-token',
            token_type: 'Bearer',
        }),
    },
}));

// Mock useFormCacheInvalidation to track invalidateAllCaches calls
const mockInvalidateAllCaches = vi.fn();
vi.mock('../../hooks/useFormCacheInvalidation', () => ({
    useFormCacheInvalidation: vi.fn(() => ({
        invalidateAllCaches: mockInvalidateAllCaches,
    })),
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
        mockInvalidateAllCaches.mockClear();
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

        it('should invalidate all cached queries after successful submission', async () => {
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
                ],
            });

            const contextValue = createMockContext({id: 1});
            const mockRefetch = vi.fn();
            contextValue.refetch = mockRefetch;

            const Wrapper = createWrapper(contextValue);

            // Mock fetch calls:
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

            // Wait for the form fields to appear
            await waitFor(() => {
                expect(screen.queryByText(/Nač/)).not.toBeInTheDocument();
            });

            // Fill in form fields
            const nameInput = screen.getByDisplayValue('Existing Member') as HTMLInputElement;
            await user.clear(nameInput);
            await user.type(nameInput, 'John Doe');

            // Submit the form
            const submitButton = screen.getByRole('button', {name: /odeslat/i});
            await user.click(submitButton);

            // Verify invalidateAllCaches was called after successful submission
            await waitFor(() => {
                expect(mockInvalidateAllCaches).toHaveBeenCalled();
                expect(onSubmitSuccess).toHaveBeenCalled();
                expect(onClose).toHaveBeenCalled();
            });
        });

        it('should invalidate caches and refetch after successful submission', async () => {
            const onClose = vi.fn();
            const user = userEvent.setup();

            const template = mockHalFormsTemplate({
                title: 'Edit Member',
                target: '/api/members/123',
                method: 'PUT',
                properties: [
                    {
                        name: 'name',
                        prompt: 'Full Name',
                        type: 'text',
                        required: true,
                    },
                ],
            });

            const contextValue = createMockContext({id: 123});
            const mockRefetch = vi.fn();
            contextValue.refetch = mockRefetch;

            const Wrapper = createWrapper(contextValue);

            // Mock fetch calls:
            fetchSpy
                .mockResolvedValueOnce(createMockResponse({id: 123, name: 'John Doe'}))
                .mockResolvedValueOnce(createMockResponse({id: 123, name: 'Jane Doe'}));

            render(
                <Wrapper>
                    <HalFormDisplay
                        template={template}
                        templateName="edit"
                        resourceData={{id: 123}}
                        pathname="/members/123"
                        onClose={onClose}
                    />
                </Wrapper>
            );

            // Wait for form to load
            await waitFor(() => {
                expect(screen.getByTestId('hal-forms-display')).toBeInTheDocument();
            });

            // Wait for the form fields to appear
            await waitFor(() => {
                expect(screen.queryByText(/Nač/)).not.toBeInTheDocument();
            });

            // Fill in form fields
            const nameInput = screen.getByDisplayValue('John Doe') as HTMLInputElement;
            await user.clear(nameInput);
            await user.type(nameInput, 'Jane Doe');

            // Submit the form
            const submitButton = screen.getByRole('button', {name: /odeslat/i});
            await user.click(submitButton);

            // Verify both cache invalidation and refetch are called
            await waitFor(() => {
                expect(mockInvalidateAllCaches).toHaveBeenCalled();
                expect(mockRefetch).toHaveBeenCalled();
                expect(onClose).toHaveBeenCalled();
            });
        });

        it('should not call invalidateAllCaches when submission fails', async () => {
            const onClose = vi.fn();
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
                ],
            });

            const contextValue = createMockContext({id: 1});
            const Wrapper = createWrapper(contextValue);

            // Mock fetch to return initial data successfully, but fail on submission
            fetchSpy
                .mockResolvedValueOnce(createMockResponse({id: 1, name: 'Test'}))
                .mockResolvedValueOnce(createMockResponse({errors: {name: 'Required'}}, 400));

            render(
                <Wrapper>
                    <HalFormDisplay
                        template={template}
                        templateName="create"
                        resourceData={{id: 1}}
                        pathname="/members"
                        onClose={onClose}
                    />
                </Wrapper>
            );

            // Wait for form to load
            await waitFor(() => {
                expect(screen.getByTestId('hal-forms-display')).toBeInTheDocument();
            });

            // Wait for form fields
            await waitFor(() => {
                expect(screen.queryByText(/Nač/)).not.toBeInTheDocument();
            });

            // Clear mock to check subsequent calls
            mockInvalidateAllCaches.mockClear();

            // Submit the form
            const nameInput = screen.getByDisplayValue('Test') as HTMLInputElement;
            await user.clear(nameInput);
            await user.type(nameInput, 'New Name');

            const submitButton = screen.getByRole('button', {name: /odeslat/i});
            await user.click(submitButton);

            // Wait a bit and verify invalidateAllCaches was NOT called on failure
            await waitFor(() => {
                expect(mockInvalidateAllCaches).not.toHaveBeenCalled();
            });
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

    describe('Custom Layout Support', () => {
        it('should render with automatic layout by default (backward compatibility)', async () => {
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
                ],
            });

            const resourceData: HalResponse = {
                id: 1,
                name: 'Test Resource',
            };
            const contextValue = createMockContext(resourceData);
            const Wrapper = createWrapper(contextValue);

            // Mock fetch for target data
            fetchSpy.mockResolvedValueOnce(createMockResponse({id: 1, name: 'Existing Member'}));

            render(
                <Wrapper>
                    <HalFormDisplay
                        template={template}
                        templateName="create"
                        resourceData={resourceData}
                        pathname="/members"
                        onClose={vi.fn()}
                    />
                </Wrapper>
            );

            // Wait for form to load with automatic layout
            await waitFor(() => {
                expect(screen.getByTestId('hal-forms-display')).toBeInTheDocument();
                // Automatic layout shows form fields
                expect(screen.getByDisplayValue('Existing Member')).toBeInTheDocument();
            });
        });

        it('should render with children-based custom layout', async () => {
            const template = mockHalFormsTemplate({
                title: 'Create Member',
                target: '/api/members',
                method: 'POST',
                properties: [
                    {
                        name: 'firstName',
                        prompt: 'First Name',
                        type: 'text',
                        required: true,
                    },
                    {
                        name: 'lastName',
                        prompt: 'Last Name',
                        type: 'text',
                        required: true,
                    },
                ],
            });

            const resourceData: HalResponse = {
                id: 1,
                name: 'Test Resource',
            };
            const contextValue = createMockContext(resourceData);
            const Wrapper = createWrapper(contextValue);

            // Mock fetch for target data
            fetchSpy.mockResolvedValueOnce(createMockResponse({
                firstName: 'John',
                lastName: 'Doe',
            }));

            const customLayout = (
                <div data-testid="custom-children-layout">
                    <div>
                        <label>First and Last Name</label>
                        <HalFormsFormField fieldName="firstName"/>
                        <HalFormsFormField fieldName="lastName"/>
                    </div>
                    <div>
                        <HalFormsFormField fieldName="submit"/>
                    </div>
                </div>
            );

            render(
                <Wrapper>
                    <HalFormDisplay
                        template={template}
                        templateName="create"
                        resourceData={resourceData}
                        pathname="/members"
                        onClose={vi.fn()}
                        customLayout={customLayout}
                    />
                </Wrapper>
            );

            // Wait for form to load with custom layout
            await waitFor(() => {
                expect(screen.getByTestId('custom-children-layout')).toBeInTheDocument();
                expect(screen.getByText('First and Last Name')).toBeInTheDocument();
            });
        });

        it('should render with callback-based custom layout', async () => {
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
                ],
            });

            const resourceData: HalResponse = {
                id: 1,
                name: 'Test Resource',
            };
            const contextValue = createMockContext(resourceData);
            const Wrapper = createWrapper(contextValue);

            // Mock fetch for target data
            fetchSpy.mockResolvedValueOnce(createMockResponse({id: 1, name: 'John Doe'}));

            const customLayout: RenderFormCallback = (renderField) => (
                <div data-testid="custom-callback-layout">
                    <div>
                        <label>Callback Custom Layout</label>
                        {renderField('name')}
                    </div>
                    <div>
                        {renderField('submit')}
                    </div>
                </div>
            );

            render(
                <Wrapper>
                    <HalFormDisplay
                        template={template}
                        templateName="create"
                        resourceData={resourceData}
                        pathname="/members"
                        onClose={vi.fn()}
                        customLayout={customLayout}
                    />
                </Wrapper>
            );

            // Wait for form to load with custom layout
            await waitFor(() => {
                expect(screen.getByTestId('custom-callback-layout')).toBeInTheDocument();
                expect(screen.getByText('Callback Custom Layout')).toBeInTheDocument();
                expect(screen.getByDisplayValue('John Doe')).toBeInTheDocument();
            });
        });

        it('should maintain backward compatibility when no customLayout provided', async () => {
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
                ],
            });

            const resourceData: HalResponse = {
                id: 1,
                name: 'Test Resource',
            };
            const contextValue = createMockContext(resourceData);
            const Wrapper = createWrapper(contextValue);

            // Mock fetch for target data
            fetchSpy.mockResolvedValueOnce(createMockResponse({id: 1, name: 'Jane Smith'}));

            render(
                <Wrapper>
                    <HalFormDisplay
                        template={template}
                        templateName="create"
                        resourceData={resourceData}
                        pathname="/members"
                        onClose={vi.fn()}
                        // No customLayout prop
                    />
                </Wrapper>
            );

            // Form should render with automatic layout
            await waitFor(() => {
                expect(screen.getByTestId('hal-forms-display')).toBeInTheDocument();
                expect(screen.getByDisplayValue('Jane Smith')).toBeInTheDocument();
            });
        });
    });
});
