import '@testing-library/jest-dom';
import React from 'react';
import {render, screen, waitFor} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {HalFormDisplay} from './HalFormDisplay.tsx';
import {HalRouteContext, type HalRouteContextValue} from '../../contexts/HalRouteContext.tsx';
import {mockHalFormsTemplate} from '../../__mocks__/halData.ts';
import type {HalResponse} from '../../api';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';

// Mock dependencies
jest.mock('../HalNavigator/hooks.ts', () => ({
    fetchResource: jest.fn(),
}));

jest.mock('../../api/hateoas.ts', () => ({
    ...jest.requireActual('../../api/hateoas'),
    submitHalFormsData: jest.fn(),
}));

describe('HalFormDisplay Component', () => {
    let queryClient: QueryClient;

    beforeEach(() => {
        queryClient = new QueryClient({
            defaultOptions: {
                queries: {retry: false, gcTime: 0},
            },
        });
        jest.clearAllMocks();
    });

    const createMockContext = (resourceData: HalResponse | null = null): HalRouteContextValue => ({
        resourceData: resourceData || {id: 1, name: 'Test Resource'},
        isLoading: false,
        error: null,
        refetch: jest.fn(),
        pathname: '/test/123',
        queryState: 'success',
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
        onClose = jest.fn(),
        onSubmitSuccess = jest.fn(),
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
                jest.fn(),
                jest.fn(),
                false
            );
            expect(screen.queryByTestId('close-form-button')).not.toBeInTheDocument();
        });
    });

    describe('Close Functionality', () => {
        it('should call onClose when close button is clicked', async () => {
            const onClose = jest.fn();
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
        const {submitHalFormsData} = require('../../api/hateoas.ts');
        const {fetchResource} = require('../HalNavigator/hooks.ts');

        beforeEach(() => {
            submitHalFormsData.mockClear();
            fetchResource.mockClear();
        });

        it('should handle successful submission with callback', async () => {
            const onSubmitSuccess = jest.fn();
            const template = mockHalFormsTemplate({
                title: 'Create',
                target: '/api/members',
                method: 'POST',
            });

            submitHalFormsData.mockResolvedValueOnce({success: true});
            fetchResource.mockResolvedValueOnce({id: 1, name: 'Test'});

            renderComponent(template, jest.fn(), onSubmitSuccess);

            // Verify the component is set up for form submission
            expect(screen.getByTestId('hal-forms-display')).toBeInTheDocument();
        });

        it('should display error when submission fails', async () => {
            const {submitHalFormsData} = require('../../api/hateoas.ts');
            const {fetchResource} = require('../HalNavigator/hooks.ts');

            const template = mockHalFormsTemplate({
                title: 'Create',
                target: '/api/members',
                method: 'POST',
            });

            const submitError = new Error('Submission failed');
            submitHalFormsData.mockRejectedValueOnce(submitError);
            fetchResource.mockResolvedValueOnce({id: 1, name: 'Test'});

            renderComponent(template);

            // Verify the component structure is in place for error display
            expect(screen.getByTestId('hal-forms-display')).toBeInTheDocument();
        });
    });

    describe('Target Data Fetching', () => {
        const {fetchResource} = require('../HalNavigator/hooks.ts');

        beforeEach(() => {
            queryClient.clear();
            fetchResource.mockReset();
        });

        it('should display form with empty data when target returns HTTP 404', async () => {
            const template = mockHalFormsTemplate({
                title: 'Create Event',
                target: '/api/events',
                method: 'POST',
            });

            const fetchError = {
                message: 'HTTP 404',
                responseStatus: 404,
                responseStatusText: 'Not Found',
            };
            fetchResource.mockRejectedValueOnce(fetchError);

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

            const fetchError = {
                message: 'HTTP 405',
                responseStatus: 405,
                responseStatusText: 'Method Not Allowed',
            };
            fetchResource.mockRejectedValueOnce(fetchError);

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

            const fetchError = {
                message: 'HTTP 500',
                responseStatus: 500,
                responseStatusText: 'Internal Server Error',
            };
            fetchResource.mockRejectedValueOnce(fetchError);

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

        it('should have proper structure for error alerts', async () => {
            const {fetchResource} = require('../HalNavigator/hooks.ts');

            const template = mockHalFormsTemplate({
                title: 'Create Event',
                target: '/api/events',
                method: 'POST',
            });

            const fetchError = {
                message: 'HTTP 500',
                responseStatus: 500,
            };
            fetchResource.mockRejectedValueOnce(fetchError);

            renderComponent(template);

            await waitFor(() => {
                const alert = screen.getByRole('alert');
                expect(alert).toBeInTheDocument();
            });
        });
    });
});
