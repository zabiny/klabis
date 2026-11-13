import '@testing-library/jest-dom';
import {render, screen} from '@testing-library/react';
import {Form, Formik} from 'formik';
import {vi} from 'vitest';
import {HalFormsRadio} from './HalFormsRadio.tsx';
import {labels} from '../../../../localization';

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

    describe('input render mode', () => {
        const renderInputMode = (prop: Parameters<typeof HalFormsRadio>[0]['prop']) =>
            render(
                <Formik initialValues={{[prop.name]: ''}} onSubmit={vi.fn()}>
                    <Form>
                        <HalFormsRadio prop={prop} errorText={undefined} subElementProps={vi.fn()} renderMode="input"/>
                    </Form>
                </Formik>
            );

        it('names the group by the localized field label', () => {
            renderInputMode({name: 'ruleType', type: 'radio', options: {inline: ['A', 'B']}});
            expect(screen.getByRole('group', {name: labels.fields.ruleType})).toBeInTheDocument();
        });

        it('names the group by prompt when provided', () => {
            renderInputMode({name: 'ruleType', prompt: 'Vlastní název', type: 'radio', options: {inline: ['A', 'B']}});
            expect(screen.getByRole('group', {name: 'Vlastní název'})).toBeInTheDocument();
        });

        it('does not render a visible label', () => {
            renderInputMode({name: 'ruleType', type: 'radio', options: {inline: ['A', 'B']}});
            expect(screen.queryByText(labels.fields.ruleType)).not.toBeInTheDocument();
        });
    });
});
