import '@testing-library/jest-dom';
import {render, screen} from '@testing-library/react';
import {Form, Formik} from 'formik';
import {vi} from 'vitest';
import {HalFormsInput} from './HalFormsInput.tsx';
import type {HalFormsProperty} from '../../../../api';
import type {HalFormsInputProps} from '../types.ts';
import {getFieldLabel} from '../../../../localization';

const renderInput = (prop: HalFormsProperty, renderMode?: HalFormsInputProps['renderMode']) =>
    render(
        <Formik initialValues={{[prop.name]: ''}} onSubmit={vi.fn()}>
            <Form>
                <HalFormsInput prop={prop} subElementProps={vi.fn()} renderMode={renderMode}/>
            </Form>
        </Formik>
    );

describe('HalFormsInput', () => {
    it('names the control by the localized field label in input mode', () => {
        renderInput({name: 'location', type: 'text'}, 'input');
        expect(screen.getByRole('textbox', {name: getFieldLabel('location')})).toBeInTheDocument();
    });

    it('prefers prompt over the localized label in input mode', () => {
        renderInput({name: 'location', prompt: 'Vlastní popisek', type: 'text'}, 'input');
        expect(screen.getByRole('textbox', {name: 'Vlastní popisek'})).toBeInTheDocument();
    });

    it('does not add aria-label in field mode', () => {
        renderInput({name: 'location', type: 'text'});
        expect(screen.getByLabelText(getFieldLabel('location'))).not.toHaveAttribute('aria-label');
    });
});
