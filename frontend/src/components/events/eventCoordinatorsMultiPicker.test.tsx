import {render, screen, waitFor} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {vi} from 'vitest';
import {Form, Formik} from 'formik';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {eventFormFieldsFactory} from './eventFormFieldsFactory.tsx';
import type {HalFormsInputProps} from '../HalNavigator2/halforms';
import {createMockResponse} from '../../__mocks__/mockFetch';

// Regression for the coordinators bug (app-review-fixes-2026-09, task 7.5): creating/updating an
// event with a multi member picker ("coordinators") failed on submit with a Yup validation error
// ("coordinators must be a `array` type, but the final value was a string"), because
// klabisCustomFieldFactory's MemberId/UUID branch always rendered a single-value picker,
// regardless of the property's multi flag.
//
// D7 (design.md) fixes this generically: a multi property whose field type has a registered
// custom widget (here "MemberId" — the backend now emits this type hint for member-id fields,
// task 7.6, in place of the ambiguous "UUID") renders one row per item via
// HalFormsCollectionField, each row using that same custom widget (HalFormsMemberId) with the
// options link available per row. This test exercises the real factory + real HalFormsMemberId +
// real useHalFormOptions end-to-end (only fetch is mocked) for both create (empty) and edit
// (pre-filled) forms.
vi.mock('../../api/klabisUserManager', () => ({
    klabisAuthUserManager: {
        getUser: vi.fn().mockReturnValue({access_token: 'test-token', token_type: 'Bearer'}),
    },
}));

const coordinatorsProp: HalFormsInputProps['prop'] = {
    name: 'coordinators',
    prompt: 'Koordinátoři',
    type: 'MemberId',
    multi: true,
    options: {link: {href: 'http://localhost:8443/api/members/options'}},
};

const membersOptions = [
    {value: '5287571b-030b-48fb-95c5-fe0663704866', prompt: 'Jan Novák'},
    {value: 'a1b2c3d4-0000-0000-0000-000000000001', prompt: 'Petra Svobodová'},
];

const renderField = (initialValues: Record<string, unknown>, onSubmit = vi.fn()) => {
    const queryClient = new QueryClient({defaultOptions: {queries: {retry: false, gcTime: 0}}});
    const element = eventFormFieldsFactory('MemberId', {
        prop: coordinatorsProp,
        errorText: undefined,
        subElementProps: vi.fn(),
    });
    if (!element) throw new Error('factory returned null');

    let submittedValues: Record<string, unknown> | undefined;
    const handleSubmit = (values: Record<string, unknown>) => {
        submittedValues = values;
        onSubmit(values);
    };

    const utils = render(
        <QueryClientProvider client={queryClient}>
            <Formik initialValues={initialValues} onSubmit={handleSubmit}>
                {({submitForm}) => (
                    <Form>
                        {element}
                        <button type="button" onClick={submitForm}>Submit</button>
                    </Form>
                )}
            </Formik>
        </QueryClientProvider>
    );

    return {...utils, getSubmitted: () => submittedValues};
};

describe('coordinators multi member picker (regression)', () => {
    beforeEach(() => {
        vi.clearAllMocks();
        (globalThis as Record<string, unknown>).fetch = vi.fn().mockResolvedValue(createMockResponse(membersOptions));
    });

    afterEach(() => {
        delete (globalThis as Record<string, unknown>).fetch;
    });

    it('renders no rows and submits an empty array on create (no coordinators yet)', async () => {
        const {getSubmitted} = renderField({coordinators: []});

        expect(screen.queryByTestId('collection-item')).not.toBeInTheDocument();
        await userEvent.setup().click(screen.getByText('Submit'));

        expect(getSubmitted()?.coordinators).toEqual([]);
    });

    it('pre-fills one member-picker row per existing coordinator on edit, each fetching options from the link', async () => {
        renderField({coordinators: [
            '5287571b-030b-48fb-95c5-fe0663704866',
            'a1b2c3d4-0000-0000-0000-000000000001',
        ]});

        const rows = await waitFor(() => screen.getAllByTestId('collection-item'));
        expect(rows).toHaveLength(2);

        const selects = await waitFor(() => screen.getAllByRole('combobox') as HTMLSelectElement[]);
        expect(selects).toHaveLength(2);
        expect(selects[0].value).toBe('5287571b-030b-48fb-95c5-fe0663704866');
        expect(selects[1].value).toBe('a1b2c3d4-0000-0000-0000-000000000001');
    });

    it('keeps the value an array on submit after changing a prefilled row', async () => {
        const user = userEvent.setup();
        const {getSubmitted} = renderField({coordinators: [
            '5287571b-030b-48fb-95c5-fe0663704866',
            'a1b2c3d4-0000-0000-0000-000000000001',
        ]});

        await waitFor(() => {
            const selects = screen.getAllByRole('combobox') as HTMLSelectElement[];
            expect(selects[1]).not.toBeDisabled();
        });
        const selects = screen.getAllByRole('combobox') as HTMLSelectElement[];
        await user.selectOptions(selects[1], '5287571b-030b-48fb-95c5-fe0663704866');
        await user.click(screen.getByText('Submit'));

        expect(getSubmitted()?.coordinators).toEqual([
            '5287571b-030b-48fb-95c5-fe0663704866',
            '5287571b-030b-48fb-95c5-fe0663704866',
        ]);
        expect(Array.isArray(getSubmitted()?.coordinators)).toBe(true);
    });
});
