import '@testing-library/jest-dom';
import {render, screen} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {Form, Formik} from 'formik';
import {vi} from 'vitest';
import {HalFormsTextArea} from './HalFormsTextArea.tsx';
import type {HalFormsProperty} from '../../../../api';
import type {HalFormsInputProps} from '../types.ts';

const createProp = (overrides: Partial<HalFormsProperty> = {}): HalFormsProperty => ({
    name: 'reason',
    prompt: 'Důvod',
    type: 'textarea',
    required: false,
    ...overrides,
});

const renderWithFormik = (prop: HalFormsProperty, initialValue = '') => {
    const fieldProps: HalFormsInputProps = {
        prop,
        errorText: undefined,
        subElementProps: vi.fn(),
    };
    return render(
        <Formik initialValues={{[prop.name]: initialValue}} onSubmit={vi.fn()}>
            <Form>
                <HalFormsTextArea {...fieldProps} />
            </Form>
        </Formik>
    );
};

describe('HalFormsTextArea', () => {
    it('renders textarea with label', () => {
        renderWithFormik(createProp());
        expect(screen.getByText('Důvod')).toBeInTheDocument();
        expect(screen.getByRole('textbox')).toBeInTheDocument();
    });

    it('does not show char counter when maxLength is not set', () => {
        renderWithFormik(createProp());
        expect(screen.queryByText(/\//)).not.toBeInTheDocument();
    });

    it('shows char counter when maxLength is set', () => {
        renderWithFormik(createProp({maxLength: 500}));
        expect(screen.getByText('0 / 500')).toBeInTheDocument();
    });

    it('updates char counter as user types', async () => {
        const user = userEvent.setup();
        renderWithFormik(createProp({maxLength: 500}));

        await user.type(screen.getByRole('textbox'), 'ahoj');

        expect(screen.getByText('4 / 500')).toBeInTheDocument();
    });

    it('shows current length when initial value is provided', () => {
        renderWithFormik(createProp({maxLength: 500}), 'hello world');
        expect(screen.getByText('11 / 500')).toBeInTheDocument();
    });
});
