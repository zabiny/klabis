import {render, screen, waitFor} from '@testing-library/react';
import {vi} from 'vitest';
import {Form, Formik} from 'formik';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {HalFormsCheckboxGroup} from './HalFormsCheckboxGroup.tsx';
import type {HalFormsInputProps} from '../types.ts';
import {createMockResponse} from '../../../../__mocks__/mockFetch';

// Regression test for design.md D6 (app-review-fixes-2026-09): createEventType/updateEventType's
// disciplineIds HAL-FORMS property switched from options.inline to an absolute options.link
// (http://host/api/disciplines/options). Unlike HalFormsCheckboxGroup.test.tsx, this exercises the
// real useHalFormOptions hook end-to-end instead of mocking it, to confirm the absolute-href link
// path actually resolves and multi-select options render.
vi.mock('../../../../api/klabisUserManager', () => ({
    klabisAuthUserManager: {
        getUser: vi.fn().mockReturnValue({access_token: 'test-token', token_type: 'Bearer'}),
    },
}));

describe('HalFormsCheckboxGroup with real link-based options (disciplineIds)', () => {
    const disciplineIdsProp: HalFormsInputProps['prop'] = {
        name: 'disciplineIds',
        prompt: 'ORIS disciplíny',
        type: 'text',
        multi: true,
        options: {link: {href: 'http://localhost:8443/api/disciplines/options'}},
    };

    const renderWithFormik = (initialValues: Record<string, unknown>) => {
        const queryClient = new QueryClient({defaultOptions: {queries: {retry: false, gcTime: 0}}});
        return render(
            <QueryClientProvider client={queryClient}>
                <Formik initialValues={initialValues} onSubmit={vi.fn()}>
                    {() => (
                        <Form>
                            <HalFormsCheckboxGroup
                                prop={disciplineIdsProp}
                                errorText={undefined}
                                subElementProps={vi.fn()}
                            />
                        </Form>
                    )}
                </Formik>
            </QueryClientProvider>
        );
    };

    beforeEach(() => {
        vi.clearAllMocks();
    });

    afterEach(() => {
        delete (globalThis as Record<string, unknown>).fetch;
    });

    it('fetches options from the absolute link href and renders them as checkboxes', async () => {
        const fetchSpy = vi.fn().mockResolvedValue(
            createMockResponse([
                {value: '1', prompt: 'Orientační běh'},
                {value: '3', prompt: 'Lyžařský OB'},
            ])
        );
        (globalThis as Record<string, unknown>).fetch = fetchSpy;

        renderWithFormik({disciplineIds: []});

        await waitFor(() => {
            expect(screen.getByLabelText('Orientační běh')).toBeInTheDocument();
        });
        expect(screen.getByLabelText('Lyžařský OB')).toBeInTheDocument();

        // absolute href is normalized down to the bare backend path, then authorizedFetch
        // re-prepends /api — the host/port is stripped either way.
        expect(fetchSpy).toHaveBeenCalledWith(
            '/api/disciplines/options',
            expect.objectContaining({
                headers: expect.objectContaining({Authorization: expect.stringContaining('Bearer')}),
            })
        );
    });
});
