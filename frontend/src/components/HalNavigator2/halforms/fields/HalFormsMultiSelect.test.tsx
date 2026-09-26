import {render, screen} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {vi} from 'vitest';
import {Form, Formik} from 'formik';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {HalFormsMultiSelect} from './HalFormsMultiSelect.tsx';
import type {HalFormsInputProps} from '../types.ts';

const defaultMockOptions = {
    options: [
        {value: '1', label: 'Orientační běh'},
        {value: '2', label: 'Lyžařský OB'},
        {value: '3', label: 'MTBO'},
    ],
    isLoading: false,
    error: null,
};

const mockOptions = vi.fn(() => defaultMockOptions);

vi.mock('../../../../hooks/useHalFormOptions', () => ({
    useHalFormOptions: () => mockOptions(),
}));

const disciplinesProp: HalFormsInputProps['prop'] = {
    name: 'disciplineIds',
    prompt: 'ORIS disciplíny',
    type: 'UUID',
    multi: true,
    options: {link: {href: '/api/disciplines/options'}},
};

const renderWithFormik = (initialValues: Record<string, unknown>) => {
    const queryClient = new QueryClient({defaultOptions: {queries: {retry: false, gcTime: 0}}});
    let submitted: unknown;

    const utils = render(
        <QueryClientProvider client={queryClient}>
            <Formik initialValues={initialValues} onSubmit={(values) => { submitted = values.disciplineIds; }}>
                {({submitForm}) => (
                    <Form>
                        <HalFormsMultiSelect prop={disciplinesProp} errorText={undefined} subElementProps={vi.fn()} />
                        <button type="button" onClick={submitForm}>Submit</button>
                    </Form>
                )}
            </Formik>
        </QueryClientProvider>
    );

    return {...utils, getSubmitted: () => submitted};
};

describe('HalFormsMultiSelect keyboard interaction', () => {
    beforeEach(() => {
        mockOptions.mockReturnValue(defaultMockOptions);
    });

    it('exposes combobox/listbox ARIA wiring when open', async () => {
        const user = userEvent.setup();
        renderWithFormik({disciplineIds: []});

        const combobox = screen.getByRole('combobox');
        expect(combobox).toHaveAttribute('aria-expanded', 'false');

        await user.click(combobox);

        expect(combobox).toHaveAttribute('aria-expanded', 'true');
        expect(combobox).toHaveAttribute('aria-controls', screen.getByRole('listbox').id);
        expect(combobox).toHaveAttribute('aria-activedescendant', screen.getAllByRole('option')[0].id);
    });

    it('moves the active option with ArrowDown/ArrowUp and wraps at the ends', async () => {
        const user = userEvent.setup();
        renderWithFormik({disciplineIds: []});

        const combobox = screen.getByRole('combobox');
        await user.click(combobox);
        const options = screen.getAllByRole('option');
        expect(combobox).toHaveAttribute('aria-activedescendant', options[0].id);

        await user.keyboard('{ArrowDown}');
        expect(combobox).toHaveAttribute('aria-activedescendant', options[1].id);

        await user.keyboard('{ArrowDown}{ArrowDown}');
        expect(combobox).toHaveAttribute('aria-activedescendant', options[0].id);

        await user.keyboard('{ArrowUp}');
        expect(combobox).toHaveAttribute('aria-activedescendant', options[2].id);
    });

    it('selects the active option on Enter and submits it as an array', async () => {
        const user = userEvent.setup();
        const {getSubmitted} = renderWithFormik({disciplineIds: []});

        const combobox = screen.getByRole('combobox');
        await user.click(combobox);
        await user.keyboard('{ArrowDown}'); // active -> "Lyžařský OB"
        await user.keyboard('{Enter}');

        expect(screen.getByText('Lyžařský OB')).toBeInTheDocument();
        expect(screen.queryByRole('listbox')).not.toBeInTheDocument();

        await user.click(screen.getByText('Submit'));
        expect(getSubmitted()).toEqual(['2']);
    });

    it('closes the dropdown on Escape without changing the selection', async () => {
        const user = userEvent.setup();
        renderWithFormik({disciplineIds: []});

        const combobox = screen.getByRole('combobox');
        await user.click(combobox);
        expect(screen.getByRole('listbox')).toBeInTheDocument();

        await user.keyboard('{Escape}');
        expect(screen.queryByRole('listbox')).not.toBeInTheDocument();
        expect(screen.queryByText('Orientační běh')).not.toBeInTheDocument();
    });

    it('removes the last chip with Backspace when the search input is empty', async () => {
        const user = userEvent.setup();
        const {getSubmitted} = renderWithFormik({disciplineIds: ['1', '2']});

        const chips = screen.getByTestId('multiselect-chips-disciplineIds');
        expect(chips).toHaveTextContent('Orientační běh');
        expect(chips).toHaveTextContent('Lyžařský OB');

        const combobox = screen.getByRole('combobox');
        await user.click(combobox);
        await user.keyboard('{Backspace}');

        expect(chips).not.toHaveTextContent('Lyžařský OB');
        expect(chips).toHaveTextContent('Orientační běh');

        await user.click(screen.getByText('Submit'));
        expect(getSubmitted()).toEqual(['1']);
    });

    it('does not remove a chip on Backspace while the search input has text', async () => {
        const user = userEvent.setup();
        renderWithFormik({disciplineIds: ['1']});

        const combobox = screen.getByRole('combobox');
        await user.type(combobox, 'x');
        await user.keyboard('{Backspace}');

        expect(screen.getByText('Orientační běh')).toBeInTheDocument();
    });
});
