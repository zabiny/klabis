import {fireEvent, render, screen} from '@testing-library/react';
import {vi} from 'vitest';
import {Form, Formik} from 'formik';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {HalFormsCheckboxGroup} from './HalFormsCheckboxGroup.tsx';
import type {HalFormsInputProps} from '../types.ts';

vi.mock('../../../../hooks/useHalFormOptions', () => ({
    useHalFormOptions: () => ({
        options: [
            {value: 'uuid-1', label: 'Jan Novák (ZBM0001)'},
            {value: 'uuid-2', label: 'Eva Svobodová (ZBM0002)'},
        ],
        isLoading: false,
        error: null,
    }),
}));

const renderWithFormik = (initialValues: Record<string, unknown>, prop: HalFormsInputProps['prop']) => {
    const queryClient = new QueryClient({
        defaultOptions: {queries: {retry: false, gcTime: 0}},
    });
    const onSubmit = vi.fn();

    const {container} = render(
        <QueryClientProvider client={queryClient}>
            <Formik initialValues={initialValues} onSubmit={onSubmit}>
                {() => (
                    <Form>
                        <HalFormsCheckboxGroup
                            prop={prop}
                            errorText={undefined}
                            subElementProps={vi.fn()}
                        />
                        <button type="submit">Submit</button>
                    </Form>
                )}
            </Formik>
        </QueryClientProvider>
    );

    return {container, onSubmit};
};

const trainersProp: HalFormsInputProps['prop'] = {
    name: 'trainers',
    prompt: 'Trenéři',
    type: 'List',
    options: {link: {href: '/members/options'}},
};

describe('HalFormsCheckboxGroup', () => {

    describe('basic rendering', () => {

        it('renders all options from useHalFormOptions', () => {
            renderWithFormik({trainers: []}, trainersProp);

            expect(screen.getByLabelText('Jan Novák (ZBM0001)')).toBeInTheDocument();
            expect(screen.getByLabelText('Eva Svobodová (ZBM0002)')).toBeInTheDocument();
        });

        it('shows option as checked when its UUID string is in the form value array', () => {
            renderWithFormik({trainers: ['uuid-1']}, trainersProp);

            expect(screen.getByLabelText('Jan Novák (ZBM0001)')).toBeChecked();
            expect(screen.getByLabelText('Eva Svobodová (ZBM0002)')).not.toBeChecked();
        });

        it('shows no options checked when form value array is empty', () => {
            renderWithFormik({trainers: []}, trainersProp);

            expect(screen.getByLabelText('Jan Novák (ZBM0001)')).not.toBeChecked();
            expect(screen.getByLabelText('Eva Svobodová (ZBM0002)')).not.toBeChecked();
        });
    });

    describe('initial value normalization for HAL objects', () => {

        it('shows option as checked when initial value contains HAL trainer object with matching memberId', () => {
            const initialTrainers = [
                {memberId: 'uuid-1', _links: {member: {href: '/api/members/uuid-1'}}},
            ];

            renderWithFormik({trainers: initialTrainers}, trainersProp);

            expect(screen.getByLabelText('Jan Novák (ZBM0001)')).toBeChecked();
            expect(screen.getByLabelText('Eva Svobodová (ZBM0002)')).not.toBeChecked();
        });

        it('shows both options checked when initial value contains two HAL trainer objects', () => {
            const initialTrainers = [
                {memberId: 'uuid-1', _links: {member: {href: '/api/members/uuid-1'}}},
                {memberId: 'uuid-2', _links: {member: {href: '/api/members/uuid-2'}}},
            ];

            renderWithFormik({trainers: initialTrainers}, trainersProp);

            expect(screen.getByLabelText('Jan Novák (ZBM0001)')).toBeChecked();
            expect(screen.getByLabelText('Eva Svobodová (ZBM0002)')).toBeChecked();
        });

        it('submits UUID strings when initial value was HAL trainer objects (without user interaction)', async () => {
            const initialTrainers = [
                {memberId: 'uuid-1', _links: {member: {href: '/api/members/uuid-1'}}},
            ];
            const {onSubmit} = renderWithFormik({trainers: initialTrainers}, trainersProp);

            fireEvent.click(screen.getByRole('button', {name: 'Submit'}));

            await vi.waitFor(() => {
                expect(onSubmit).toHaveBeenCalledWith(
                    {trainers: ['uuid-1']},
                    expect.anything()
                );
            });
        });

        it('submits empty array when initial value was empty HAL object array', async () => {
            const {onSubmit} = renderWithFormik({trainers: []}, trainersProp);

            fireEvent.click(screen.getByRole('button', {name: 'Submit'}));

            await vi.waitFor(() => {
                expect(onSubmit).toHaveBeenCalledWith(
                    {trainers: []},
                    expect.anything()
                );
            });
        });
    });

    describe('checkbox interaction after HAL object initialization', () => {

        it('unchecks a pre-selected trainer when user clicks it', () => {
            const initialTrainers = [
                {memberId: 'uuid-1', _links: {member: {href: '/api/members/uuid-1'}}},
            ];

            renderWithFormik({trainers: initialTrainers}, trainersProp);

            const checkbox = screen.getByLabelText('Jan Novák (ZBM0001)');
            expect(checkbox).toBeChecked();

            fireEvent.click(checkbox);

            expect(checkbox).not.toBeChecked();
        });

        it('adds a new trainer UUID when user checks an unchecked option alongside pre-selected HAL objects', async () => {
            const initialTrainers = [
                {memberId: 'uuid-1', _links: {member: {href: '/api/members/uuid-1'}}},
            ];
            const {onSubmit} = renderWithFormik({trainers: initialTrainers}, trainersProp);

            fireEvent.click(screen.getByLabelText('Eva Svobodová (ZBM0002)'));
            fireEvent.click(screen.getByRole('button', {name: 'Submit'}));

            await vi.waitFor(() => {
                expect(onSubmit).toHaveBeenCalledWith(
                    {trainers: expect.arrayContaining(['uuid-1', 'uuid-2'])},
                    expect.anything()
                );
            });
        });
    });
});
