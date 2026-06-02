import '@testing-library/jest-dom';
import {render, screen} from '@testing-library/react';
import {Form, Formik} from 'formik';
import {halFormsFieldsFactory} from './HalFormsFieldFactory.tsx';
import type {HalFormsProperty} from '../../../api';
import type {HalFormsInputProps} from './types.ts';
import {vi} from 'vitest';

vi.mock('../../../hooks/useHalFormOptions', () => ({
    useHalFormOptions: vi.fn(() => ({
        options: [
            {value: 'A', label: 'A'},
            {value: 'B', label: 'B'},
        ],
        isLoading: false,
        error: null,
    })),
}));

const makeProps = (prop: HalFormsProperty): HalFormsInputProps => ({
    prop,
    errorText: undefined,
    subElementProps: vi.fn(),
});

const renderInFormik = (element: React.ReactElement | null, initialValue: string = '') => {
    if (!element) throw new Error('Factory returned null');
    const name = 'testField';
    return render(
        <Formik initialValues={{[name]: initialValue}} onSubmit={vi.fn()}>
            <Form>{element}</Form>
        </Formik>
    );
};

describe('halFormsFieldsFactory', () => {
    describe('select field dispatch', () => {
        it('renders select when type is "select"', () => {
            const prop: HalFormsProperty = {
                name: 'testField',
                type: 'select',
                options: {inline: ['A', 'B']},
            };
            const element = halFormsFieldsFactory('select', makeProps(prop));
            renderInFormik(element);
            expect(screen.getByRole('combobox')).toBeInTheDocument();
        });

        it('renders select when type is "text" but options are present', () => {
            const prop: HalFormsProperty = {
                name: 'testField',
                type: 'text',
                options: {inline: ['A', 'B']},
            };
            const element = halFormsFieldsFactory('text', makeProps(prop));
            renderInFormik(element);
            expect(screen.getByRole('combobox')).toBeInTheDocument();
        });

        it('renders text input when type is "text" and no options present', () => {
            const prop: HalFormsProperty = {
                name: 'testField',
                type: 'text',
            };
            const element = halFormsFieldsFactory('text', makeProps(prop));
            renderInFormik(element);
            expect(screen.getByRole('textbox')).toBeInTheDocument();
            expect(screen.queryByRole('combobox')).not.toBeInTheDocument();
        });

        it('returns null when options.inline is an empty array', () => {
            const prop: HalFormsProperty = {
                name: 'category',
                type: 'text',
                options: {inline: []},
            };
            const element = halFormsFieldsFactory('text', makeProps(prop));
            expect(element).toBeNull();
        });

        it('renders select when options.inline has items', () => {
            const prop: HalFormsProperty = {
                name: 'category',
                type: 'text',
                options: {inline: ['A', 'B']},
            };
            const element = halFormsFieldsFactory('text', makeProps(prop));
            renderInFormik(element);
            expect(screen.getByRole('combobox')).toBeInTheDocument();
        });
    });

    describe('multi-select with inline options', () => {
        it('renders checkbox group when multi=true and options.inline has items', () => {
            const prop: HalFormsProperty = {
                name: 'orisDisciplineIds',
                type: 'number',
                multi: true,
                options: {inline: [{value: '1', prompt: 'Orientační běh'}, {value: '3', prompt: 'Lyžařský OB'}]},
            };
            const element = halFormsFieldsFactory('number', makeProps(prop));
            renderInFormik(element, '');
            // useHalFormOptions mock returns [{value: 'A', label: 'A'}, {value: 'B', label: 'B'}]
            expect(screen.getByLabelText('A')).toBeInTheDocument();
            expect(screen.queryByRole('combobox')).not.toBeInTheDocument();
        });

        it('renders checkbox group when multiple=true and options.inline has items', () => {
            const prop: HalFormsProperty = {
                name: 'disciplineIds',
                type: 'text',
                multiple: true,
                options: {inline: ['A', 'B']},
            };
            const element = halFormsFieldsFactory('text', makeProps(prop));
            renderInFormik(element, '');
            expect(screen.getByLabelText('A')).toBeInTheDocument();
            expect(screen.queryByRole('combobox')).not.toBeInTheDocument();
        });

        it('renders select (not checkbox group) when multi=false and options.inline has items', () => {
            const prop: HalFormsProperty = {
                name: 'disciplineId',
                type: 'text',
                multi: false,
                options: {inline: ['A', 'B']},
            };
            const element = halFormsFieldsFactory('text', makeProps(prop));
            renderInFormik(element, '');
            expect(screen.getByRole('combobox')).toBeInTheDocument();
        });
    });
});
