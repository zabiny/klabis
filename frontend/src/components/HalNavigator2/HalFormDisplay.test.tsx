import '@testing-library/jest-dom';
import React from 'react';
import {render, screen, waitFor} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {HalFormDisplay} from './HalFormDisplay.tsx';
import {useHalPageData} from '../../hooks/useHalPageData';
import {mockHalFormsTemplate} from '../../__mocks__/halData.ts';
import {createMockResponse} from '../../__mocks__/mockFetch';
import type {HalResponse} from '../../api';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {type Mock, vi} from 'vitest';
import {HalFormsFormField, type RenderFormCallback} from './halforms/HalFormsForm.tsx';
import {MemoryRouter} from "react-router-dom";
import type {HalFormsTemplateMethod} from '../../api/types';

const mockNavigate = vi.fn();
vi.mock('react-router-dom', async () => {
    const actual = await vi.importActual('react-router-dom');
    return {
        ...actual,
        useNavigate: () => mockNavigate,
    };
});

const createMockResponseWithLocation = (data: any, status = 201, location: string | null = null): Response =>
    createMockResponse(data, status, location ? {'Location': location} : {});

vi.mock('../../hooks/useHalPageData', () => ({
    useHalPageData: vi.fn(),
}));

// Mock dependencies
vi.mock('../../api/klabisUserManager', () => ({
    klabisAuthUserManager: {
        getUser: vi.fn().mockReturnValue({
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

const mockAddToast = vi.fn();
vi.mock('../../contexts/ToastContext', () => ({
    useToast: () => ({addToast: mockAddToast}),
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

    const createMockPageData = (resourceData: HalResponse | null = null) => ({
        resourceData: resourceData || {id: 1, name: 'Test Resource'},
        isLoading: false,
        error: null,
        isAdmin: false,
        route: {
            pathname: '/test/123',
            navigateToResource: vi.fn(),
            refetch: vi.fn(),
            queryState: 'success',
            getResourceLink: vi.fn(),
        },
        actions: {
            handleNavigateToItem: vi.fn(),
        },
        getLinks: vi.fn(() => undefined),
        getTemplates: vi.fn(() => undefined),
        hasEmbedded: vi.fn(() => false),
        getEmbeddedItems: vi.fn(() => []),
        isCollection: vi.fn(() => false),
        hasLink: vi.fn(() => false),
        hasTemplate: vi.fn(() => false),
        hasForms: vi.fn(() => false),
        getPageMetadata: vi.fn(() => undefined),
    });

    const createWrapper = (pageData: any) => {
        const mockUseHalPageData = vi.mocked(useHalPageData);
        mockUseHalPageData.mockReturnValue(pageData);

        return ({children}: { children: React.ReactNode }) => (
            <QueryClientProvider client={queryClient}>
                <MemoryRouter>
                    {children}
                </MemoryRouter>
            </QueryClientProvider>
        );
    };

    const renderComponent = (
        template = mockHalFormsTemplate({title: 'Test Form'}),
        onClose = vi.fn(),
        onSubmitSuccess = vi.fn(),
    ) => {
        const resourceData: HalResponse = {
            id: 1,
            name: 'Test Resource',
        };
        const pageData = createMockPageData(resourceData);
        const Wrapper = createWrapper(pageData);
        return render(
            <Wrapper>
                <HalFormDisplay
                    template={template}
                    templateName="test"
                    resourceData={resourceData}
                    pathname="/test/123"
                    onClose={onClose}
                    onSubmitSuccess={onSubmitSuccess}
                />
            </Wrapper>
        );
    };

    describe('Form Display', () => {
        it('should display the form', () => {
            renderComponent();
            expect(screen.getByTestId('hal-forms-display')).toBeInTheDocument();
        });

        it('should not render a title header', () => {
            const template = mockHalFormsTemplate({title: 'Edit Member'});
            renderComponent(template);
            expect(screen.queryByRole('heading', {level: 4})).not.toBeInTheDocument();
        });

        it('should not render a close button', () => {
            renderComponent();
            expect(screen.queryByTestId('close-form-button')).not.toBeInTheDocument();
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

            const pageData = createMockPageData({id: 1});
            const mockRefetch = vi.fn();
            pageData.route.refetch = mockRefetch;

            const Wrapper = createWrapper(pageData);

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

            const pageData = createMockPageData({id: 1});
            const mockRefetch = vi.fn();
            pageData.route.refetch = mockRefetch;

            const Wrapper = createWrapper(pageData);

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

            const pageData = createMockPageData({id: 123});
            const mockRefetch = vi.fn();
            pageData.route.refetch = mockRefetch;

            const Wrapper = createWrapper(pageData);

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

            const pageData = createMockPageData({id: 1});
            const Wrapper = createWrapper(pageData);

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

        it('should pass response data to onSubmitSuccess callback', async () => {
            const onClose = vi.fn();
            const onSubmitSuccess = vi.fn();
            const user = userEvent.setup();

            const template = mockHalFormsTemplate({
                title: 'Create Member',
                target: '/api/members',
                method: 'POST',
                properties: [
                    {name: 'name', prompt: 'Full Name', type: 'text', required: true},
                ],
            });

            const pageData = createMockPageData({id: 1});
            const Wrapper = createWrapper(pageData);
            const responseData = {id: 99, name: 'New Member', _links: {self: {href: '/api/members/99'}}};

            fetchSpy
                .mockResolvedValueOnce(createMockResponse({id: 1, name: 'Existing'}))
                .mockResolvedValueOnce(createMockResponse(responseData));

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

            await waitFor(() => expect(screen.queryByText(/Nač/)).not.toBeInTheDocument());

            const nameInput = screen.getByDisplayValue('Existing') as HTMLInputElement;
            await user.clear(nameInput);
            await user.type(nameInput, 'John Doe');

            const submitButton = screen.getByRole('button', {name: /odeslat/i});
            await user.click(submitButton);

            await waitFor(() => {
                expect(onSubmitSuccess).toHaveBeenCalledWith(responseData);
            });
        });

        describe('auto-navigation on POST+Location', () => {
            const renderPostForm = (method: HalFormsTemplateMethod, pageData?: any) => {
                const template = mockHalFormsTemplate({
                    title: 'Create Group',
                    target: '/api/family-groups',
                    method,
                    properties: [{name: 'name', prompt: 'Name', type: 'text', required: true}],
                });
                const data = pageData ?? createMockPageData({id: 1});
                const Wrapper = createWrapper(data);
                render(
                    <Wrapper>
                        <HalFormDisplay
                            template={template}
                            templateName="create"
                            resourceData={{id: 1}}
                            pathname="/family-groups"
                            onClose={vi.fn()}
                        />
                    </Wrapper>
                );
                return template;
            };

            it('navigates to location path after POST with Location header', async () => {
                const user = userEvent.setup();
                const pageData = createMockPageData({id: 1});
                renderPostForm('POST', pageData);

                fetchSpy
                    .mockResolvedValueOnce(createMockResponse({name: 'Existing'}))
                    .mockResolvedValueOnce(createMockResponseWithLocation(null, 201, '/api/family-groups/xyz'));

                await waitFor(() => expect(screen.queryByText(/Nač/)).not.toBeInTheDocument());
                const nameInput = screen.getByDisplayValue('Existing') as HTMLInputElement;
                await user.clear(nameInput);
                await user.type(nameInput, 'My Group');
                await user.click(screen.getByRole('button', {name: /odeslat/i}));

                await waitFor(() => {
                    expect(mockNavigate).toHaveBeenCalledWith('/family-groups/xyz');
                });
            });

            it('does NOT call onClose after POST+Location (navigation supersedes close)', async () => {
                const user = userEvent.setup();
                const mockOnClose = vi.fn();
                const template = mockHalFormsTemplate({
                    title: 'Create Group',
                    target: '/api/family-groups',
                    method: 'POST',
                    properties: [{name: 'name', prompt: 'Name', type: 'text', required: true}],
                });
                const pageData = createMockPageData({id: 1});
                const Wrapper = createWrapper(pageData);
                render(
                    <Wrapper>
                        <HalFormDisplay
                            template={template}
                            templateName="create"
                            resourceData={{id: 1}}
                            pathname="/family-groups"
                            onClose={mockOnClose}
                        />
                    </Wrapper>
                );

                fetchSpy
                    .mockResolvedValueOnce(createMockResponse({name: 'Existing'}))
                    .mockResolvedValueOnce(createMockResponseWithLocation(null, 201, '/api/family-groups/xyz'));

                await waitFor(() => expect(screen.queryByText(/Nač/)).not.toBeInTheDocument());
                const nameInput = screen.getByDisplayValue('Existing') as HTMLInputElement;
                await user.clear(nameInput);
                await user.type(nameInput, 'My Group');
                await user.click(screen.getByRole('button', {name: /odeslat/i}));

                await waitFor(() => expect(mockNavigate).toHaveBeenCalledWith('/family-groups/xyz'));
                expect(mockOnClose).not.toHaveBeenCalled();
            });

            it('calls onClose after PUT success (no navigation)', async () => {
                const user = userEvent.setup();
                const mockOnClose = vi.fn();
                const template = mockHalFormsTemplate({
                    title: 'Edit Group',
                    target: '/api/family-groups/xyz',
                    method: 'PUT',
                    properties: [{name: 'name', prompt: 'Name', type: 'text', required: true}],
                });
                const pageData = createMockPageData({id: 1});
                const Wrapper = createWrapper(pageData);
                render(
                    <Wrapper>
                        <HalFormDisplay
                            template={template}
                            templateName="edit"
                            resourceData={{id: 1}}
                            pathname="/family-groups/xyz"
                            onClose={mockOnClose}
                        />
                    </Wrapper>
                );

                fetchSpy
                    .mockResolvedValueOnce(createMockResponse({name: 'Existing'}))
                    .mockResolvedValueOnce(createMockResponse({name: 'Updated'}, 200));

                await waitFor(() => expect(screen.queryByText(/Nač/)).not.toBeInTheDocument());
                const nameInput = screen.getByDisplayValue('Existing') as HTMLInputElement;
                await user.clear(nameInput);
                await user.type(nameInput, 'Updated Group');
                await user.click(screen.getByRole('button', {name: /odeslat/i}));

                await waitFor(() => expect(mockOnClose).toHaveBeenCalled());
                expect(mockNavigate).not.toHaveBeenCalled();
            });

            it('does NOT navigate after PUT even when Location header is present', async () => {
                const user = userEvent.setup();
                const pageData = createMockPageData({id: 1});
                renderPostForm('PUT', pageData);

                fetchSpy
                    .mockResolvedValueOnce(createMockResponse({name: 'Existing'}))
                    .mockResolvedValueOnce(createMockResponseWithLocation(null, 200, '/api/family-groups/xyz'));

                await waitFor(() => expect(screen.queryByText(/Nač/)).not.toBeInTheDocument());
                const nameInput = screen.getByDisplayValue('Existing') as HTMLInputElement;
                await user.clear(nameInput);
                await user.type(nameInput, 'Updated Group');
                await user.click(screen.getByRole('button', {name: /odeslat/i}));

                await waitFor(() => expect(mockInvalidateAllCaches).toHaveBeenCalled());
                expect(mockNavigate).not.toHaveBeenCalled();
            });

            it('does NOT navigate after DELETE', async () => {
                const user = userEvent.setup();
                const pageData = createMockPageData({id: 1});
                renderPostForm('DELETE', pageData);

                fetchSpy
                    .mockResolvedValueOnce(createMockResponse({name: 'Existing'}))
                    .mockResolvedValueOnce(createMockResponseWithLocation(null, 204, '/api/family-groups/xyz'));

                await waitFor(() => expect(screen.queryByText(/Nač/)).not.toBeInTheDocument());
                await user.click(screen.getByRole('button', {name: /odeslat/i}));

                await waitFor(() => expect(mockInvalidateAllCaches).toHaveBeenCalled());
                expect(mockNavigate).not.toHaveBeenCalled();
            });

            it('does NOT navigate after POST when Location header is null', async () => {
                const user = userEvent.setup();
                const pageData = createMockPageData({id: 1});
                renderPostForm('POST', pageData);

                fetchSpy
                    .mockResolvedValueOnce(createMockResponse({name: 'Existing'}))
                    .mockResolvedValueOnce(createMockResponseWithLocation({id: 42}, 201, null));

                await waitFor(() => expect(screen.queryByText(/Nač/)).not.toBeInTheDocument());
                const nameInput = screen.getByDisplayValue('Existing') as HTMLInputElement;
                await user.clear(nameInput);
                await user.type(nameInput, 'My Group');
                await user.click(screen.getByRole('button', {name: /odeslat/i}));

                await waitFor(() => expect(mockInvalidateAllCaches).toHaveBeenCalled());
                expect(mockNavigate).not.toHaveBeenCalled();
            });

            it('navigation happens after invalidateAllCaches, route.refetch, and addToast', async () => {
                const user = userEvent.setup();
                const callOrder: string[] = [];
                const pageData = createMockPageData({id: 1});
                mockInvalidateAllCaches.mockImplementation(async () => {
                    callOrder.push('invalidate');
                });
                pageData.route.refetch = vi.fn(async () => {
                    callOrder.push('refetch');
                });
                mockAddToast.mockImplementation(() => {
                    callOrder.push('toast');
                });
                mockNavigate.mockImplementation(() => {
                    callOrder.push('navigate');
                });

                renderPostForm('POST', pageData);

                fetchSpy
                    .mockResolvedValueOnce(createMockResponse({name: 'Existing'}))
                    .mockResolvedValueOnce(createMockResponseWithLocation(null, 201, '/api/family-groups/xyz'));

                await waitFor(() => expect(screen.queryByText(/Nač/)).not.toBeInTheDocument());
                const nameInput = screen.getByDisplayValue('Existing') as HTMLInputElement;
                await user.clear(nameInput);
                await user.type(nameInput, 'My Group');
                await user.click(screen.getByRole('button', {name: /odeslat/i}));

                await waitFor(() => {
                    expect(mockNavigate).toHaveBeenCalled();
                });
                expect(callOrder).toEqual(['invalidate', 'refetch', 'toast', 'navigate']);
            });
        });

        it('should apply postprocessPayload before submitting', async () => {
            const onClose = vi.fn();
            const user = userEvent.setup();

            const template = mockHalFormsTemplate({
                title: 'Edit Member',
                target: '/api/members/1',
                method: 'PATCH',
                properties: [
                    {name: 'name', prompt: 'Full Name', type: 'text', required: true},
                    {name: 'readOnlyField', prompt: 'Read Only', type: 'text'},
                ],
            });

            const pageData = createMockPageData({id: 1});
            const Wrapper = createWrapper(pageData);

            fetchSpy
                .mockResolvedValueOnce(createMockResponse({name: 'John', readOnlyField: 'read-only'}))
                .mockResolvedValueOnce(createMockResponse({id: 1, name: 'Jane'}));

            const postprocessPayload = vi.fn((payload: Record<string, unknown>) => {
                const {readOnlyField: _, ...rest} = payload;
                return rest;
            });

            render(
                <Wrapper>
                    <HalFormDisplay
                        template={template}
                        templateName="edit"
                        resourceData={{id: 1}}
                        pathname="/members/1"
                        onClose={onClose}
                        postprocessPayload={postprocessPayload}
                    />
                </Wrapper>
            );

            await waitFor(() => expect(screen.queryByText(/Nač/)).not.toBeInTheDocument());

            const nameInput = screen.getByDisplayValue('John') as HTMLInputElement;
            await user.clear(nameInput);
            await user.type(nameInput, 'Jane');

            const submitButton = screen.getByRole('button', {name: /odeslat/i});
            await user.click(submitButton);

            await waitFor(() => {
                expect(postprocessPayload).toHaveBeenCalled();
                const submittedPayload = (fetchSpy.mock.calls[1][1] as RequestInit).body as string;
                const body = JSON.parse(submittedPayload);
                expect(body).not.toHaveProperty('readOnlyField');
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
            const pageData = createMockPageData(resourceData);
            const Wrapper = createWrapper(pageData);

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
            const pageData = createMockPageData(resourceData);
            const Wrapper = createWrapper(pageData);

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
            const pageData = createMockPageData(resourceData);
            const Wrapper = createWrapper(pageData);

            // Mock fetch for target data
            fetchSpy.mockResolvedValueOnce(createMockResponse({id: 1, name: 'John Doe'}));

            const customLayout: RenderFormCallback = ({renderField}) => (
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
            const pageData = createMockPageData(resourceData);
            const Wrapper = createWrapper(pageData);

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
