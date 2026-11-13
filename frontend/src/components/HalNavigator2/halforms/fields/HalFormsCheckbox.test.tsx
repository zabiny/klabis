import '@testing-library/jest-dom';
import {render, screen} from '@testing-library/react';
import {Form, Formik} from 'formik';
import {vi} from 'vitest';
import {HalFormsCheckbox} from './HalFormsCheckbox.tsx';
import type {HalFormsProperty} from '../../../../api';
import type {HalFormsInputProps} from '../types.ts';
import {getFieldLabel} from '../../../../localization';

const renderCheckbox = (prop: HalFormsProperty, renderMode?: HalFormsInputProps['renderMode']) =>
    render(
        <Formik initialValues={{[prop.name]: false}} onSubmit={vi.fn()}>
            <Form>
                <HalFormsCheckbox prop={prop} subElementProps={vi.fn()} renderMode={renderMode}/>
            </Form>
        </Formik>
    );

describe('HalFormsCheckbox', () => {
    it('names the control by the localized field label in input mode', () => {
        renderCheckbox({name: 'location', type: 'checkbox'}, 'input');
        expect(screen.getByRole('checkbox', {name: getFieldLabel('location')})).toBeInTheDocument();
    });

    it('prefers prompt over the localized label in input mode', () => {
        renderCheckbox({name: 'location', prompt: 'Vlastní popisek', type: 'checkbox'}, 'input');
        expect(screen.getByRole('checkbox', {name: 'Vlastní popisek'})).toBeInTheDocument();
    });

    it('does not add aria-label in field mode', () => {
        renderCheckbox({name: 'location', type: 'checkbox'});
        expect(screen.getByLabelText(getFieldLabel('location'))).not.toHaveAttribute('aria-label');
    });
});
