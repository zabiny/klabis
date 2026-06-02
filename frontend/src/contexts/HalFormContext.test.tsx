import {act, render, renderHook} from '@testing-library/react';
import {BrowserRouter, MemoryRouter, Route, Routes, useNavigate} from 'react-router-dom';
import {HalFormProvider} from './HalFormContext';
import {type HalFormRequest, useHalForm} from './halFormContext';
import {describe, expect, it, vi} from 'vitest';
import type {ReactNode} from 'react';

const createWrapper = () => ({children}: { children: ReactNode }) => (
    <BrowserRouter>
        <HalFormProvider>{children}</HalFormProvider>
    </BrowserRouter>
);


describe('HalFormContext', () => {
    describe('useHalForm Hook', () => {
        it('should throw error when used outside HalFormProvider', () => {
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
            expect(result.current).toHaveProperty('displayHalForm');
            expect(result.current).toHaveProperty('closeForm');
        });

        it('should have null currentFormRequest initially', () => {
            const {result} = renderHook(() => useHalForm(), {wrapper: createWrapper()});

            expect(result.current.currentFormRequest).toBeNull();
        });
    });

    describe('requestForm Function', () => {
        it('should update currentFormRequest when modal form is requested', () => {
            const {result} = renderHook(() => useHalForm(), {wrapper: createWrapper()});

            const formRequest: HalFormRequest = {
                templateName: 'edit',
                modal: true,
            };

            act(() => {
                result.current.displayHalForm(formRequest);
            });

            expect(result.current.currentFormRequest).toEqual(formRequest);
        });

        it('should update currentFormRequest when inline form is requested', () => {
            const {result} = renderHook(() => useHalForm(), {wrapper: createWrapper()});

            const inlineRequest: HalFormRequest = {
                templateName: 'create',
                modal: false,
            };

            act(() => {
                result.current.displayHalForm(inlineRequest);
            });

            expect(result.current.currentFormRequest).toEqual(inlineRequest);
            expect(result.current.currentFormRequest?.modal).toBe(false);
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
                result.current.displayHalForm(formRequest1);
            });

            expect(result.current.currentFormRequest?.templateName).toBe('edit');

            act(() => {
                result.current.displayHalForm(formRequest2);
            });

            expect(result.current.currentFormRequest?.templateName).toBe('delete');
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
                result.current.displayHalForm(formRequest);
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

        it('should clear inline form request when closeForm is called', () => {
            const {result} = renderHook(() => useHalForm(), {wrapper: createWrapper()});

            act(() => {
                result.current.displayHalForm({templateName: 'create', modal: false});
            });

            expect(result.current.currentFormRequest?.modal).toBe(false);

            act(() => {
                result.current.closeForm();
            });

            expect(result.current.currentFormRequest).toBeNull();
        });
    });

    describe('Provider Behavior', () => {
        it('should render children', () => {
            const {result} = renderHook(() => useHalForm(), {wrapper: createWrapper()});

            expect(result.current).toBeDefined();
            expect(result.current.currentFormRequest).toBeNull();
        });

        it('should provide context to multiple hook calls within same render', () => {
            const {result} = renderHook(() => useHalForm(), {wrapper: createWrapper()});

            const formRequest: HalFormRequest = {
                templateName: 'edit',
                modal: true,
            };

            act(() => {
                result.current.displayHalForm(formRequest);
            });

            expect(result.current.currentFormRequest).toEqual(formRequest);
        });

        it('should reset currentFormRequest when pathname changes', () => {
            const Probe = ({onReady}: {onReady: (ctx: ReturnType<typeof useHalForm>) => void}) => {
                const ctx = useHalForm();
                onReady(ctx);
                return null;
            };

            let ctx!: ReturnType<typeof useHalForm>;
            const TestApp = () => (
                <MemoryRouter initialEntries={['/events']}>
                    <HalFormProvider>
                        <Routes>
                            <Route path="/events" element={<Probe onReady={(c) => { ctx = c; }}/>}/>
                            <Route path="/members" element={<Probe onReady={(c) => { ctx = c; }}/>}/>
                        </Routes>
                        <NavigateButton/>
                    </HalFormProvider>
                </MemoryRouter>
            );

            const NavigateButton = () => {
                const nav = useNavigate();
                return <button data-testid="nav" onClick={() => nav('/members')}>go</button>;
            };

            const {getByTestId} = render(<TestApp/>);

            act(() => {
                ctx.displayHalForm({templateName: 'createEvent', modal: false});
            });
            expect(ctx.currentFormRequest?.templateName).toBe('createEvent');

            act(() => {
                getByTestId('nav').click();
            });
            expect(ctx.currentFormRequest).toBeNull();
        });
    });

    describe('Context Value Structure', () => {
        it('should have all required properties', () => {
            const {result} = renderHook(() => useHalForm(), {wrapper: createWrapper()});

            expect(result.current).toHaveProperty('currentFormRequest');
            expect(result.current).toHaveProperty('displayHalForm');
            expect(result.current).toHaveProperty('closeForm');
        });

        it('should have correct property types', () => {
            const {result} = renderHook(() => useHalForm(), {wrapper: createWrapper()});

            expect(typeof result.current.displayHalForm).toBe('function');
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
                result.current.displayHalForm(formRequest);
            });

            expect(result.current.currentFormRequest?.templateName).toBe('testTemplate');
        });

        it('should contain modal flag in form request', () => {
            const {result} = renderHook(() => useHalForm(), {wrapper: createWrapper()});

            const formRequest: HalFormRequest = {
                templateName: 'test',
                modal: true,
            };

            act(() => {
                result.current.displayHalForm(formRequest);
            });

            expect(result.current.currentFormRequest?.modal).toBe(true);
        });

        it('should optionally contain children in form request', () => {
            const {result} = renderHook(() => useHalForm(), {wrapper: createWrapper()});

            const children = () => <div>Custom</div>;
            const formRequest: HalFormRequest = {
                templateName: 'test',
                modal: false,
                children,
            };

            act(() => {
                result.current.displayHalForm(formRequest);
            });

            expect(result.current.currentFormRequest?.children).toBeDefined();
        });
    });
});
