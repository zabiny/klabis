import React from 'react';
import {act, renderHook, waitFor} from '@testing-library/react';
import {useHalActions} from './useHalActions';
import {mockHalFormsTemplate} from '../__mocks__/halData';
import {createMockResponse} from '../__mocks__/mockFetch';
import * as ReactRouterDom from 'react-router-dom';
import {BrowserRouter} from 'react-router-dom';
import {type Mock, vi} from 'vitest';
import * as HalRouteContextModule from '../contexts/HalRouteContext';
import * as HateoasModule from '../api/hateoas';
import * as NavigationPathModule from '../utils/navigationPath';

// Mock dependencies
vi.mock('react-router-dom', async () => ({
    ...(await vi.importActual('react-router-dom')),
    useNavigate: vi.fn(),
}));

vi.mock('../contexts/HalRouteContext', async () => ({
    ...(await vi.importActual('../contexts/HalRouteContext')),
    useHalRoute: vi.fn(),
}));

vi.mock('../api/hateoas', async () => ({
    ...(await vi.importActual('../api/hateoas')),
    submitHalFormsData: vi.fn(),
}));

vi.mock('../utils/navigationPath', async () => ({
    ...(await vi.importActual('../utils/navigationPath')),
    extractNavigationPath: vi.fn((href) => href),
}));

const useNavigate = vi.mocked(ReactRouterDom.useNavigate);
const useHalRoute = vi.mocked(HalRouteContextModule.useHalRoute);
const submitHalFormsData = vi.mocked(HateoasModule.submitHalFormsData);
const extractNavigationPath = vi.mocked(NavigationPathModule.extractNavigationPath);

describe('useHalActions Hook', () => {
    let mockNavigate: Mock;
    let mockRefetch: Mock;

    beforeEach(() => {
        vi.clearAllMocks();

        mockNavigate = vi.fn();
        mockRefetch = vi.fn().mockResolvedValue(undefined);

        useNavigate.mockReturnValue(mockNavigate);
        useHalRoute.mockReturnValue({
            resourceData: null,
            navigateToResource: vi.fn(),
            isLoading: false,
            error: null,
            refetch: mockRefetch,
            pathname: '/items',
            queryState: 'success',
            getResourceLink: vi.fn(),
        });
    });

    const createWrapper = () => {
        return ({children}: { children: React.ReactNode }) => (
            <BrowserRouter>
                {children}
            </BrowserRouter>
        );
    };

    describe('Initial State', () => {
        it('should initialize with null template', () => {
            const {result} = renderHook(() => useHalActions(), {
                wrapper: createWrapper(),
            });

            expect(result.current.selectedTemplate).toBeNull();
        });

        it('should initialize with null submit error', () => {
            const {result} = renderHook(() => useHalActions(), {
                wrapper: createWrapper(),
            });

            expect(result.current.submitError).toBeNull();
        });

        it('should initialize with isSubmitting false', () => {
            const {result} = renderHook(() => useHalActions(), {
                wrapper: createWrapper(),
            });

            expect(result.current.isSubmitting).toBe(false);
        });

        it('should return all required properties', () => {
            const {result} = renderHook(() => useHalActions(), {
                wrapper: createWrapper(),
            });

            expect(result.current).toHaveProperty('selectedTemplate');
            expect(result.current).toHaveProperty('setSelectedTemplate');
            expect(result.current).toHaveProperty('submitError');
            expect(result.current).toHaveProperty('setSubmitError');
            expect(result.current).toHaveProperty('isSubmitting');
            expect(result.current).toHaveProperty('handleNavigateToItem');
            expect(result.current).toHaveProperty('handleFormSubmit');
        });
    });

    describe('Template Selection', () => {
        it('should set selected template', () => {
            const {result} = renderHook(() => useHalActions(), {
                wrapper: createWrapper(),
            });

            const template = mockHalFormsTemplate({title: 'Create'});

            act(() => {
                result.current.setSelectedTemplate(template);
            });

            expect(result.current.selectedTemplate).toEqual(template);
        });

        it('should clear selected template', () => {
            const {result} = renderHook(() => useHalActions(), {
                wrapper: createWrapper(),
            });

            const template = mockHalFormsTemplate();

            act(() => {
                result.current.setSelectedTemplate(template);
            });

            expect(result.current.selectedTemplate).toEqual(template);

            act(() => {
                result.current.setSelectedTemplate(null);
            });

            expect(result.current.selectedTemplate).toBeNull();
        });
    });

    describe('Error Management', () => {
        it('should set submit error', () => {
            const {result} = renderHook(() => useHalActions(), {
                wrapper: createWrapper(),
            });

            const error = new Error('Test error');

            act(() => {
                result.current.setSubmitError(error);
            });

            expect(result.current.submitError).toEqual(error);
        });

        it('should clear submit error', () => {
            const {result} = renderHook(() => useHalActions(), {
                wrapper: createWrapper(),
            });

            const error = new Error('Test error');

            act(() => {
                result.current.setSubmitError(error);
            });

            expect(result.current.submitError).toEqual(error);

            act(() => {
                result.current.setSubmitError(null);
            });

            expect(result.current.submitError).toBeNull();
        });
    });

    describe('Navigation', () => {
        it('should navigate to item href', () => {
            const {result} = renderHook(() => useHalActions(), {
                wrapper: createWrapper(),
            });

            act(() => {
                result.current.handleNavigateToItem('/api/items/1');
            });

            expect(mockNavigate).toHaveBeenCalledWith('/api/items/1');
        });

        it('should extract navigation path before navigating', () => {
            const {result} = renderHook(() => useHalActions(), {
                wrapper: createWrapper(),
            });

            act(() => {
                result.current.handleNavigateToItem('/api/items/123');
            });

            expect(extractNavigationPath).toHaveBeenCalledWith('/api/items/123');
            expect(mockNavigate).toHaveBeenCalled();
        });

        it('should handle complex URLs', () => {
            const {result} = renderHook(() => useHalActions(), {
                wrapper: createWrapper(),
            });

            const complexUrl = '/api/items/1?filter=active&sort=name';

            act(() => {
                result.current.handleNavigateToItem(complexUrl);
            });

            expect(extractNavigationPath).toHaveBeenCalledWith(complexUrl);
        });
    });

    describe('Form Submission', () => {
        it('should not submit when no template is selected', async () => {
            const {result} = renderHook(() => useHalActions(), {
                wrapper: createWrapper(),
            });

            const formData = {name: 'Test'};

            await act(async () => {
                await result.current.handleFormSubmit(formData);
            });

            expect(submitHalFormsData).not.toHaveBeenCalled();
        });

        it('should set isSubmitting to true during submission', async () => {
            const {result} = renderHook(() => useHalActions(), {
                wrapper: createWrapper(),
            });

            const template = mockHalFormsTemplate();

            act(() => {
                result.current.setSelectedTemplate(template);
            });

            // Mock to simulate a slow submission
            submitHalFormsData.mockImplementationOnce(
                () => new Promise((resolve) => setTimeout(resolve, 100)),
            );

            // Start submission without awaiting
            act(() => {
                result.current.handleFormSubmit({name: 'Test'});
            });

            // After starting submission, isSubmitting should be true (or become true soon)
            await waitFor(() => {
                expect(result.current.isSubmitting).toBe(true);
            }, {timeout: 100});
        });

        it('should set isSubmitting to false after submission', async () => {
            const {result} = renderHook(() => useHalActions(), {
                wrapper: createWrapper(),
            });

            const template = mockHalFormsTemplate();

            act(() => {
                result.current.setSelectedTemplate(template);
            });

            submitHalFormsData.mockResolvedValueOnce(createMockResponse({}));

            await act(async () => {
                await result.current.handleFormSubmit({name: 'Test'});
            });

            await waitFor(() => {
                expect(result.current.isSubmitting).toBe(false);
            });
        });

        it('should call submitHalFormsData with correct parameters', async () => {
            const {result} = renderHook(() => useHalActions(), {
                wrapper: createWrapper(),
            });

            const template = mockHalFormsTemplate({method: 'POST', target: '/api/items'});

            act(() => {
                result.current.setSelectedTemplate(template);
            });

            submitHalFormsData.mockResolvedValueOnce(createMockResponse({}));

            const formData = {name: 'New Item', description: 'Test'};

            await act(async () => {
                await result.current.handleFormSubmit(formData);
            });

            expect(submitHalFormsData).toHaveBeenCalledWith(
                expect.objectContaining({
                    target: '/api/items',
                    method: 'POST',
                }),
                formData,
            );
        });

        it('should use pathname as target if not specified in template', async () => {
            const {result} = renderHook(() => useHalActions(), {
                wrapper: createWrapper(),
            });

            const template = mockHalFormsTemplate({target: undefined});

            act(() => {
                result.current.setSelectedTemplate(template);
            });

            submitHalFormsData.mockResolvedValueOnce(createMockResponse({}));

            await act(async () => {
                await result.current.handleFormSubmit({name: 'Test'});
            });

            expect(submitHalFormsData).toHaveBeenCalledWith(
                expect.objectContaining({
                    target: '/api/items', // Uses the pathname from useHalRoute
                }),
                expect.any(Object),
            );
        });

        it('should clear submit error on successful submission', async () => {
            const {result} = renderHook(() => useHalActions(), {
                wrapper: createWrapper(),
            });

            const template = mockHalFormsTemplate();

            act(() => {
                result.current.setSelectedTemplate(template);
                result.current.setSubmitError(new Error('Previous error'));
            });

            submitHalFormsData.mockResolvedValueOnce(createMockResponse({}));

            await act(async () => {
                await result.current.handleFormSubmit({name: 'Test'});
            });

            expect(result.current.submitError).toBeNull();
        });

        it('should refetch data after successful submission', async () => {
            const {result} = renderHook(() => useHalActions(), {
                wrapper: createWrapper(),
            });

            const template = mockHalFormsTemplate();

            act(() => {
                result.current.setSelectedTemplate(template);
            });

            submitHalFormsData.mockResolvedValueOnce(createMockResponse({}));

            await act(async () => {
                await result.current.handleFormSubmit({name: 'Test'});
            });

            await waitFor(() => {
                expect(mockRefetch).toHaveBeenCalled();
            });
        });

        it('should close form after successful submission', async () => {
            const {result} = renderHook(() => useHalActions(), {
                wrapper: createWrapper(),
            });

            const template = mockHalFormsTemplate();

            act(() => {
                result.current.setSelectedTemplate(template);
            });

            submitHalFormsData.mockResolvedValueOnce(createMockResponse({}));

            await act(async () => {
                await result.current.handleFormSubmit({name: 'Test'});
            });

            await waitFor(() => {
                expect(result.current.selectedTemplate).toBeNull();
            });
        });

        it('should handle submission errors', async () => {
            const {result} = renderHook(() => useHalActions(), {
                wrapper: createWrapper(),
            });

            const template = mockHalFormsTemplate();
            const error = new Error('Submission failed');

            act(() => {
                result.current.setSelectedTemplate(template);
            });

            submitHalFormsData.mockRejectedValueOnce(error);

            await act(async () => {
                await result.current.handleFormSubmit({name: 'Test'});
            });

            await waitFor(() => {
                expect(result.current.submitError).toEqual(error);
            });
        });

        it('should convert non-Error to Error on submission failure', async () => {
            const {result} = renderHook(() => useHalActions(), {
                wrapper: createWrapper(),
            });

            const template = mockHalFormsTemplate();

            act(() => {
                result.current.setSelectedTemplate(template);
            });

            submitHalFormsData.mockRejectedValueOnce('String error');

            await act(async () => {
                await result.current.handleFormSubmit({name: 'Test'});
            });

            await waitFor(() => {
                expect(result.current.submitError).toEqual(new Error('Failed to submit form'));
            });
        });

        it('should keep form open on error', async () => {
            const {result} = renderHook(() => useHalActions(), {
                wrapper: createWrapper(),
            });

            const template = mockHalFormsTemplate();

            act(() => {
                result.current.setSelectedTemplate(template);
            });

            submitHalFormsData.mockRejectedValueOnce(new Error('Failed'));

            await act(async () => {
                await result.current.handleFormSubmit({name: 'Test'});
            });

            await waitFor(() => {
                expect(result.current.selectedTemplate).toEqual(template);
            });
        });

        it('should reset isSubmitting after error', async () => {
            const {result} = renderHook(() => useHalActions(), {
                wrapper: createWrapper(),
            });

            const template = mockHalFormsTemplate();

            act(() => {
                result.current.setSelectedTemplate(template);
            });

            submitHalFormsData.mockRejectedValueOnce(new Error('Failed'));

            await act(async () => {
                await result.current.handleFormSubmit({name: 'Test'});
            });

            await waitFor(() => {
                expect(result.current.isSubmitting).toBe(false);
            });
        });
    });

    describe('Multiple Operations', () => {
        it('should handle multiple submissions', async () => {
            const {result} = renderHook(() => useHalActions(), {
                wrapper: createWrapper(),
            });

            const template = mockHalFormsTemplate();

            act(() => {
                result.current.setSelectedTemplate(template);
            });

            submitHalFormsData.mockResolvedValue(createMockResponse({}));

            // First submission
            await act(async () => {
                await result.current.handleFormSubmit({name: 'First'});
            });

            expect(submitHalFormsData).toHaveBeenCalledTimes(1);

            // Reset for second submission
            act(() => {
                result.current.setSelectedTemplate(template);
            });

            // Second submission
            await act(async () => {
                await result.current.handleFormSubmit({name: 'Second'});
            });

            expect(submitHalFormsData).toHaveBeenCalledTimes(2);
        });
    });
});
