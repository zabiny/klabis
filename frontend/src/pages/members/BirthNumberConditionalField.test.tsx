import '@testing-library/jest-dom';
import {type ReactElement, type ReactNode} from 'react';
import {render, screen} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {Formik, Form, Field} from 'formik';
import {vi} from 'vitest';
import {BirthNumberConditionalField} from './BirthNumberConditionalField';
import {isCzNationality} from './isCzNationality';

const renderInput = (name: string): ReactNode => (
    <Field key={name} as="input" data-testid={`input-${name}`} name={name}/>
);

const renderInFormik = (initialValues: Record<string, unknown>): ReactElement => (
    <Formik initialValues={initialValues} onSubmit={vi.fn()}>
        <Form>
            <Field as="input" data-testid="input-nationality" name="nationality"/>
            <BirthNumberConditionalField renderInput={renderInput}/>
        </Form>
    </Formik>
);

describe('BirthNumberConditionalField', () => {
    describe('isCzNationality', () => {
        it.each([
            ['CZ', true],
            ['CZE', true],
            ['SK', false],
            ['', false],
            [null, false],
            [undefined, false],
        ])('returns %s for %s', (input, expected) => {
            expect(isCzNationality(input)).toBe(expected);
        });
    });

    it('shows birth number field when nationality is CZ', () => {
        render(renderInFormik({nationality: 'CZ', birthNumber: ''}));
        expect(screen.getByText('Rodné číslo')).toBeInTheDocument();
    });

    it('shows birth number field when nationality is CZE', () => {
        render(renderInFormik({nationality: 'CZE', birthNumber: ''}));
        expect(screen.getByText('Rodné číslo')).toBeInTheDocument();
    });

    it('hides birth number field when nationality is non-CZ', () => {
        render(renderInFormik({nationality: 'SK', birthNumber: ''}));
        expect(screen.queryByText('Rodné číslo')).not.toBeInTheDocument();
    });

    it('hides birth number field when nationality is empty', () => {
        render(renderInFormik({nationality: '', birthNumber: ''}));
        expect(screen.queryByText('Rodné číslo')).not.toBeInTheDocument();
    });

    it('clears birth number value and hides field when nationality changes from CZ to non-CZ', async () => {
        const user = userEvent.setup();
        render(renderInFormik({nationality: 'CZ', birthNumber: '9003151234'}));

        expect(screen.getByText('Rodné číslo')).toBeInTheDocument();
        expect(screen.getByTestId('input-birthNumber')).toHaveValue('9003151234');

        const nationalityInput = screen.getByTestId('input-nationality');
        await user.clear(nationalityInput);
        await user.type(nationalityInput, 'SK');

        expect(screen.queryByText('Rodné číslo')).not.toBeInTheDocument();
    });
});
