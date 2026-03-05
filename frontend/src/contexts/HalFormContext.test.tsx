import {act, renderHook} from '@testing-library/react';
import {BrowserRouter, MemoryRouter} from 'react-router-dom';
import {HalFormProvider, type HalFormRequest, useHalForm} from './HalFormContext';
import {describe, expect, it, vi} from 'vitest';
import type {ReactNode} from 'react';
import type {RenderFormCallback} from '../components/HalNavigator2/halforms';

// Helper to wrap provider with required Router context
const createWrapper = () => ({children}: { children: ReactNode }) => (
    <BrowserRouter>
        <HalFormProvider>{children}</HalFormProvider>
    </BrowserRouter>
);

// Helper to create wrapper with specific URL for testing query parameters
const createWrapperWithUrl = (initialUrl: string) => ({children}: { children: ReactNode }) => (
    <MemoryRouter initialEntries={[initialUrl]}>
        <HalFormProvider>{children}</HalFormProvider>
    </MemoryRouter>
);

// Helper to create wrapper without Router (testing graceful degradation)
const createWrapperWithoutRouter = () => ({children}: { children: ReactNode }) => (
    <HalFormProvider>{children}</HalFormProvider>
);

describe('HalFormContext', () => {
    describe('useHalForm Hook', () => {
        it('should throw error when used outside HalFormProvider', () => {
            // Suppress console.error for this test
            const consoleSpy = vi.spyOn(console, 'error').mockImplementation(() => {
            });

            expect(() => {
                renderHook(() => useHalForm());
            }).toThrow('useHalForm must be used within a component wrapped by HalFormProvider');

            consoleSpy.mockRestore();
        });

        it('should return context value when used within HalFormProvider', () => {
            const {result} = renderHook(() => useHalForm(), {wrapper: createWrapper()});

            expect(result.current).toBeDefined();
            expect(result.current).toHaveProperty('currentFormRequest');
            expect(result.current).toHaveProperty('requestForm');
            expect(result.current).toHaveProperty('closeForm');
        });

        it('should have null currentFormRequest initially', () => {
            const {result} = renderHook(() => useHalForm(), {wrapper: createWrapper()});

            expect(result.current.currentFormRequest).toBeNull();
        });
    });

    describe('requestForm Function', () => {
        it('should update currentFormRequest when called', () => {
            const {result} = renderHook(() => useHalForm(), {wrapper: createWrapper()});

            const formRequest: HalFormRequest = {
                templateName: 'edit',
                modal: true,
            };

            act(() => {
                result.current.requestForm(formRequest);
            });

            expect(result.current.currentFormRequest).toEqual(formRequest);
        });

        it('should update currentFormRequest with custom layout', () => {
            const {result} = renderHook(() => useHalForm(), {wrapper: createWrapper()});

            const customLayout: RenderFormCallback = () => <div>Custom Layout</div>;
            const formRequest: HalFormRequest = {
                templateName: 'create',
                modal: true,
                customLayout,
            };

            act(() => {
                result.current.requestForm(formRequest);
            });

            expect(result.current.currentFormRequest).toEqual(formRequest);
            expect(result.current.currentFormRequest?.customLayout).toBe(customLayout);
        });

        it('should allow multiple requestForm calls (last request wins)', () => {
            const {result} = renderHook(() => useHalForm(), {wrapper: createWrapper()});

            const formRequest1: HalFormRequest = {
                templateName: 'edit',
                modal: true,
            };

            const formRequest2: HalFormRequest = {
                templateName: 'delete',
                modal: true,
            };

            act(() => {
                result.current.requestForm(formRequest1);
            });

            expect(result.current.currentFormRequest?.templateName).toBe('edit');

            act(() => {
                result.current.requestForm(formRequest2);
            });

            expect(result.current.currentFormRequest?.templateName).toBe('delete');
        });

        it('should support both modal and inline requests', () => {
            const {result} = renderHook(() => useHalForm(), {wrapper: createWrapper()});

            const modalRequest: HalFormRequest = {
                templateName: 'edit',
                modal: true,
            };

            act(() => {
                result.current.requestForm(modalRequest);
            });

            expect(result.current.currentFormRequest?.modal).toBe(true);

            const inlineRequest: HalFormRequest = {
                templateName: 'view',
                modal: false,
            };

            act(() => {
                result.current.requestForm(inlineRequest);
            });

            expect(result.current.currentFormRequest?.modal).toBe(false);
        });
    });

    describe('closeForm Function', () => {
        it('should clear currentFormRequest when called', () => {
            const {result} = renderHook(() => useHalForm(), {wrapper: createWrapper()});

            const formRequest: HalFormRequest = {
                templateName: 'edit',
                modal: true,
            };

            act(() => {
                result.current.requestForm(formRequest);
            });

            expect(result.current.currentFormRequest).not.toBeNull();

            act(() => {
                result.current.closeForm();
            });

            expect(result.current.currentFormRequest).toBeNull();
        });

        it('should be safe to call when no form is open', () => {
            const {result} = renderHook(() => useHalForm(), {wrapper: createWrapper()});

            expect(result.current.currentFormRequest).toBeNull();

            act(() => {
                result.current.closeForm();
            });

            expect(result.current.currentFormRequest).toBeNull();
        });
    });

    describe('Provider Behavior', () => {
        it('should render children', () => {
            const {result} = renderHook(() => useHalForm(), {wrapper: createWrapper()});

            // Verify provider is working by checking that the hook returns expected value
            expect(result.current).toBeDefined();
            expect(result.current.currentFormRequest).toBeNull();
        });

        it('should provide context to multiple hook calls within same render', () => {
            let callCount = 0;
            const {result} = renderHook(() => {
                callCount++;
                return useHalForm();
            }, {wrapper: createWrapper()});

            const formRequest: HalFormRequest = {
                templateName: 'edit',
                modal: true,
            };

            act(() => {
                result.current.requestForm(formRequest);
            });

            // The hook should return the updated state
            expect(result.current.currentFormRequest).toEqual(formRequest);
        });
    });

    describe('Context Value Structure', () => {
        it('should have all required properties', () => {
            const {result} = renderHook(() => useHalForm(), {wrapper: createWrapper()});

            expect(result.current).toHaveProperty('currentFormRequest');
            expect(result.current).toHaveProperty('requestForm');
            expect(result.current).toHaveProperty('closeForm');
        });

        it('should have correct property types', () => {
            const {result} = renderHook(() => useHalForm(), {wrapper: createWrapper()});

            expect(typeof result.current.requestForm).toBe('function');
            expect(typeof result.current.closeForm).toBe('function');
            expect(result.current.currentFormRequest === null || typeof result.current.currentFormRequest === 'object').toBe(true);
        });
    });

    describe('Form Request Structure', () => {
        it('should contain templateName in form request', () => {
            const {result} = renderHook(() => useHalForm(), {wrapper: createWrapper()});

            const formRequest: HalFormRequest = {
                templateName: 'testTemplate',
                modal: true,
            };

            act(() => {
                result.current.requestForm(formRequest);
            });

            expect(result.current.currentFormRequest?.templateName).toBe('testTemplate');
        });

        it('should contain modal flag in form request', () => {
            const {result} = renderHook(() => useHalForm(), {wrapper: createWrapper()});

            const formRequest: HalFormRequest = {
                templateName: 'test',
                modal: false,
            };

            act(() => {
                result.current.requestForm(formRequest);
            });

            expect(result.current.currentFormRequest?.modal).toBe(false);
        });

        it('should optionally contain customLayout in form request', () => {
            const {result} = renderHook(() => useHalForm(), {wrapper: createWrapper()});

            const customLayout: RenderFormCallback = (_form) => <div>Custom</div>;
            const formRequest: HalFormRequest = {
                templateName: 'test',
                modal: true,
                customLayout,
            };

            act(() => {
                result.current.requestForm(formRequest);
            });

            expect(result.current.currentFormRequest?.customLayout).toBeDefined();
        });
    });

    describe('URL Query Parameter Detection', () => {
        it('should detect URL parameter ?form=templateName and set inline form request', () => {
            const {result} = renderHook(() => useHalForm(), {
                wrapper: createWrapperWithUrl('/?form=edit'),
            });

            expect(result.current.currentFormRequest?.templateName).toBe('edit');
            expect(result.current.currentFormRequest?.modal).toBe(false);
        });

        it('should detect different template names from URL parameter', () => {
            const {result} = renderHook(() => useHalForm(), {
                wrapper: createWrapperWithUrl('/?form=create'),
            });

            expect(result.current.currentFormRequest?.templateName).toBe('create');
            expect(result.current.currentFormRequest?.modal).toBe(false);
        });

        it('should have null currentFormRequest when no URL parameter is present', () => {
            const {result} = renderHook(() => useHalForm(), {
                wrapper: createWrapperWithUrl('/'),
            });

            expect(result.current.currentFormRequest).toBeNull();
        });

        it('should preserve modal forms when URL has no form parameter', () => {
            const {result} = renderHook(() => useHalForm(), {
                wrapper: createWrapperWithUrl('/'),
            });

            const modalRequest: HalFormRequest = {
                templateName: 'edit',
                modal: true,
            };

            act(() => {
                result.current.requestForm(modalRequest);
            });

            expect(result.current.currentFormRequest?.modal).toBe(true);
            expect(result.current.currentFormRequest?.templateName).toBe('edit');
        });

        it('should not clear modal forms when URL parameter is absent', () => {
            const {result, rerender} = renderHook(() => useHalForm(), {
                wrapper: createWrapperWithUrl('/'),
            });

            const modalRequest: HalFormRequest = {
                templateName: 'delete',
                modal: true,
            };

            act(() => {
                result.current.requestForm(modalRequest);
            });

            expect(result.current.currentFormRequest?.modal).toBe(true);

            rerender();

            expect(result.current.currentFormRequest?.modal).toBe(true);
            expect(result.current.currentFormRequest?.templateName).toBe('delete');
        });

        it('should clear inline form when URL parameter is removed', () => {
            const {result} = renderHook(() => useHalForm(), {
                wrapper: createWrapperWithUrl('/?form=edit'),
            });

            expect(result.current.currentFormRequest?.templateName).toBe('edit');
            expect(result.current.currentFormRequest?.modal).toBe(false);

            const {result: resultAfterUrlChange} = renderHook(() => useHalForm(), {
                wrapper: createWrapperWithUrl('/'),
            });

            expect(resultAfterUrlChange.current.currentFormRequest).toBeNull();
        });

        it('should not crash when used outside Router context', () => {
            const {result} = renderHook(() => useHalForm(), {
                wrapper: createWrapperWithoutRouter(),
            });

            // Should gracefully handle being outside Router
            expect(result.current).toBeDefined();
            expect(result.current.currentFormRequest).toBeNull();
        });

        it('should allow requestForm to work outside Router context', () => {
            const {result} = renderHook(() => useHalForm(), {
                wrapper: createWrapperWithoutRouter(),
            });

            const modalRequest: HalFormRequest = {
                templateName: 'edit',
                modal: true,
            };

            act(() => {
                result.current.requestForm(modalRequest);
            });

            expect(result.current.currentFormRequest?.templateName).toBe('edit');
            expect(result.current.currentFormRequest?.modal).toBe(true);
        });
    });
});
