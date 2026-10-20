import '@testing-library/jest-dom';
import {render, screen} from '@testing-library/react';
import {Form, Formik} from 'formik';
import {vi} from 'vitest';
import {HalFormsRadio} from './HalFormsRadio.tsx';

vi.mock('../../../../hooks/useHalFormOptions.ts', () => ({
    useHalFormOptions: () => ({options: [{value: 'A', label: 'A'}], isLoading: false, error: null}),
}));

describe('HalFormsRadio', () => {
    it('falls back to the localized field label when prompt is missing', () => {
        render(
            <Formik initialValues={{ruleType: ''}} onSubmit={vi.fn()}>
                <Form>
                    <HalFormsRadio
                        prop={{name: 'ruleType', type: 'radio', options: {inline: ['A', 'B']}}}
                        errorText={undefined}
                        subElementProps={vi.fn()}
                    />
                </Form>
            </Formik>
        );
        expect(screen.getByText('Typ pravidla')).toBeInTheDocument();
    });
});
