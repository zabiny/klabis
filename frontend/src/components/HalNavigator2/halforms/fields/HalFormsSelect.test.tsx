import '@testing-library/jest-dom';
import {render, screen} from '@testing-library/react';
import {Form, Formik} from 'formik';
import {vi} from 'vitest';
import {HalFormsSelect} from './HalFormsSelect.tsx';
import type {HalFormsProperty} from '../../../../api';
import type {HalFormsInputProps} from '../types.ts';
import {getFieldLabel} from '../../../../localization';

vi.mock('../../../../hooks/useHalFormOptions', () => ({
    useHalFormOptions: () => ({
        options: [{value: 'a', label: 'Option A'}],
        isLoading: false,
        error: null,
    }),
}));

const renderSelect = (prop: HalFormsProperty, renderMode?: HalFormsInputProps['renderMode']) =>
    render(
        <Formik initialValues={{[prop.name]: ''}} onSubmit={vi.fn()}>
            <Form>
                <HalFormsSelect prop={prop} subElementProps={vi.fn()} renderMode={renderMode}/>
            </Form>
        </Formik>
    );

describe('HalFormsSelect', () => {
    it('names the control by the localized field label in input mode', () => {
        renderSelect({name: 'location', type: 'text'}, 'input');
        expect(screen.getByRole('combobox', {name: getFieldLabel('location')})).toBeInTheDocument();
    });

    it('prefers prompt over the localized label in input mode', () => {
        renderSelect({name: 'location', prompt: 'Vlastní popisek', type: 'text'}, 'input');
        expect(screen.getByRole('combobox', {name: 'Vlastní popisek'})).toBeInTheDocument();
    });

    it('labels the control by the localized field label in field mode without aria-label', () => {
        renderSelect({name: 'location', type: 'text'});
        expect(screen.getByLabelText(getFieldLabel('location'))).not.toHaveAttribute('aria-label');
    });
});
