import '@testing-library/jest-dom';
import {render, screen} from '@testing-library/react';
import {Form, Formik} from 'formik';
import {halFormsFieldsFactory} from './HalFormsFieldFactory.tsx';
import type {HalFormsProperty} from '../../../api';
import type {HalFormsInputProps} from './types.ts';
import {vi} from 'vitest';

vi.mock('../../../hooks/useHalFormOptions', () => ({
    useHalFormOptions: vi.fn((_optionDef: unknown) => ({
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
    });
});
