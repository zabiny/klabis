import {render, screen, waitFor} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {vi} from 'vitest';
import {Form, Formik} from 'formik';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {klabisFieldsFactory} from './KlabisFieldsFactory.tsx';
import type {HalFormsInputProps} from './HalNavigator2/halforms';
import {createMockResponse} from '../__mocks__/mockFetch';

// Regression for the disciplineIds bug (app-review-fixes-2026-09 QA, second instance of the same
// class of bug as coordinators — see eventCoordinatorsMultiPicker.test.tsx). EventType
// create/updateEventType's "disciplineIds" is a Set<UUID> (multi:true) with a remote
// options.link to GET /api/disciplines/options (design.md D6, backend commit 0035b285). Like
// "coordinators", its HAL-FORMS "type" is "UUID" (Spring HATEOAS has no HtmlInputType mapping for
// java.util.UUID, so it falls back to the simple class name). Unlike "coordinators" (D7: field
// type "MemberId", per-row member picker), disciplineIds keeps the plain "UUID" type, which has
// no registered custom widget — so it resolves to the generic HalFormsMultiSelect (searchable
// dropdown + removable chips), the D7 branch for "multi + options + no custom widget". Before the
// fix, ANY multi property of type "UUID" fell into the single-value member-picker widget
// regardless of its options, silently submitting a scalar. This is EventTypesPage's actual path:
// it renders via HalFormDisplay with no custom fieldsFactory, so it goes through the plain
// klabisFieldsFactory (unlike "coordinators", which goes through eventFormFieldsFactory) —
// exercised here directly with real HalFormsMultiSelect + useHalFormOptions (only fetch mocked).
vi.mock('../api/klabisUserManager', () => ({
    klabisAuthUserManager: {
        getUser: vi.fn().mockReturnValue({access_token: 'test-token', token_type: 'Bearer'}),
    },
}));

const disciplineIdsProp: HalFormsInputProps['prop'] = {
    name: 'disciplineIds',
    prompt: 'ORIS disciplíny',
    type: 'UUID',
    multi: true,
    options: {link: {href: 'http://localhost:8443/api/disciplines/options'}},
};

const disciplineOptions = [
    {value: '1', prompt: 'Orientační běh'},
    {value: '3', prompt: 'Lyžařský OB'},
];

const renderField = (initialValues: Record<string, unknown>) => {
    const queryClient = new QueryClient({defaultOptions: {queries: {retry: false, gcTime: 0}}});
    const element = klabisFieldsFactory('UUID', {
        prop: disciplineIdsProp,
        errorText: undefined,
        subElementProps: vi.fn(),
    });
    if (!element) throw new Error('factory returned null');

    let submittedValues: Record<string, unknown> | undefined;
    const utils = render(
        <QueryClientProvider client={queryClient}>
            <Formik initialValues={initialValues} onSubmit={(values) => { submittedValues = values; }}>
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

describe('disciplineIds multi picker on EventType create/edit (regression)', () => {
    beforeEach(() => {
        vi.clearAllMocks();
        (globalThis as Record<string, unknown>).fetch = vi.fn().mockResolvedValue(createMockResponse(disciplineOptions));
    });

    afterEach(() => {
        delete (globalThis as Record<string, unknown>).fetch;
    });

    it('renders the searchable multi-select fetched from the link, not checkboxes or a single dropdown', async () => {
        renderField({disciplineIds: []});

        await waitFor(() => {
            expect(screen.getByRole('combobox')).not.toBeDisabled();
        });
        expect(screen.queryByRole('checkbox')).not.toBeInTheDocument();
    });

    it('submits an array of discipline ids on create (starting empty)', async () => {
        const user = userEvent.setup();
        const {getSubmitted} = renderField({disciplineIds: []});

        await waitFor(() => expect(screen.getByRole('combobox')).not.toBeDisabled());
        await user.click(screen.getByRole('combobox'));
        await user.click(screen.getByRole('option', {name: 'Orientační běh'}));
        await user.click(screen.getByText('Submit'));

        expect(getSubmitted()?.disciplineIds).toEqual(['1']);
    });

    it('pre-fills a chip from an existing array value on edit and keeps it an array on submit', async () => {
        const user = userEvent.setup();
        const {getSubmitted} = renderField({disciplineIds: ['1']});

        await waitFor(() => {
            expect(screen.getByText('Orientační běh')).toBeInTheDocument();
        });
        expect(screen.queryByText('Lyžařský OB')).not.toBeInTheDocument();

        await user.click(screen.getByRole('combobox'));
        await user.click(screen.getByRole('option', {name: 'Lyžařský OB'}));
        await user.click(screen.getByText('Submit'));

        expect(getSubmitted()?.disciplineIds).toEqual(expect.arrayContaining(['1', '3']));
        expect(Array.isArray(getSubmitted()?.disciplineIds)).toBe(true);
    });
});
