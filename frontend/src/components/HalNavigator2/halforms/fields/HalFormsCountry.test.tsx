import '@testing-library/jest-dom';
import {render, screen} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {Form, Formik, useFormikContext} from 'formik';
import {vi} from 'vitest';
import {HalFormsCountry} from './HalFormsCountry.tsx';
import type {HalFormsProperty} from '../../../../api';

const ValueProbe = () => {
    const {values} = useFormikContext<Record<string, unknown>>();
    return <span data-testid="value">{String(values.nationality)}</span>;
};

const renderPicker = (initial: string, prop: Partial<HalFormsProperty> = {}, errorText?: string) =>
    render(
        <Formik initialValues={{nationality: initial}} onSubmit={vi.fn()}>
            <Form>
                <HalFormsCountry prop={{name: 'nationality', type: 'Country', ...prop}} errorText={errorText}
                                 subElementProps={vi.fn()}/>
                <ValueProbe/>
            </Form>
        </Formik>
    );

describe('HalFormsCountry', () => {
    it('offers presets in fixed order followed by other country', () => {
        renderPicker('CZ');
        const labels = screen.getAllByRole('option').map(o => o.textContent);
        expect(labels).toEqual([
            'Vyberte stát', 'Česko', 'Slovensko', 'Rakousko', 'Polsko', 'Německo', 'Švédsko', 'Norsko', 'Dánsko', 'Jiný stát…'
        ]);
    });

    it('selects the stored preset and shows no code input', () => {
        renderPicker('SK');
        expect(screen.getByRole('combobox')).toHaveValue('SK');
        expect(screen.queryByRole('textbox')).not.toBeInTheDocument();
    });

    it('emits the preset code when a preset is chosen', async () => {
        renderPicker('CZ');
        await userEvent.selectOptions(screen.getByRole('combobox'), 'PL');
        expect(screen.getByTestId('value')).toHaveTextContent('PL');
    });

    it('opens in other mode with the code filled in for a non-preset value', () => {
        renderPicker('FR');
        expect(screen.getByRole('combobox')).toHaveValue('__other__');
        expect(screen.getByRole('textbox')).toHaveValue('FR');
        expect(screen.getByText('Francie')).toBeInTheDocument();
    });

    it('reveals the code input when other country is chosen', async () => {
        renderPicker('CZ');
        await userEvent.selectOptions(screen.getByRole('combobox'), '__other__');
        expect(screen.getByRole('textbox')).toHaveValue('');
    });

    it('emits upper-case code typed into the input and shows its name', async () => {
        renderPicker('FR');
        const input = screen.getByRole('textbox');
        await userEvent.clear(input);
        await userEvent.type(input, 'it');
        expect(screen.getByTestId('value')).toHaveTextContent('IT');
        expect(screen.getByText('Itálie')).toBeInTheDocument();
    });

    it('shows an unknown code hint for an unresolvable code', () => {
        renderPicker('QQ');
        expect(screen.getByText('Neznámý kód státu')).toBeInTheDocument();
    });

    it('disables controls when read-only', () => {
        renderPicker('FR', {readOnly: true});
        expect(screen.getByRole('combobox')).toBeDisabled();
        expect(screen.getByRole('textbox')).toBeDisabled();
    });

    it('shows the error text', () => {
        renderPicker('CZ', {}, 'Chyba');
        expect(screen.getByText('Chyba')).toBeInTheDocument();
    });
});
