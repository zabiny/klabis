import '@testing-library/jest-dom';
import {render, screen} from '@testing-library/react';
import {Field, Form, Formik} from 'formik';
import {vi} from 'vitest';
import {renderRegistrationForm} from './RegistrationForm.tsx';
import type {HalFormPanelRenderHelpers} from '../../components/HalNavigator2/HalFormPanel.tsx';

vi.mock('./RegistrationGuardianSections.tsx', () => ({
    RegistrationGuardianSections: () => null,
    RegistrationContactSection: () => null,
}));

const helpers = {
    renderInput: (name: string) => <Field key={name} as="input" data-testid={`input-${name}`} name={name}/>,
    renderField: () => null,
    hasField: (name: string) => ['nationality', 'birthNumber'].includes(name),
    hasType: () => false,
} as unknown as HalFormPanelRenderHelpers;

const renderForm = (initialValues: Record<string, unknown>) =>
    render(
        <Formik initialValues={initialValues} onSubmit={vi.fn()}>
            <Form>{renderRegistrationForm()(helpers)}</Form>
        </Formik>
    );

describe('renderRegistrationForm nationality default', () => {
    it('starts with CZ nationality and shows the birth number when empty', async () => {
        renderForm({nationality: '', birthNumber: ''});

        expect(await screen.findByTestId('input-nationality')).toHaveValue('CZ');
        expect(screen.getByText('Rodné číslo')).toBeInTheDocument();
    });

    it('keeps an existing nationality', () => {
        renderForm({nationality: 'SK', birthNumber: ''});

        expect(screen.getByTestId('input-nationality')).toHaveValue('SK');
    });
});
