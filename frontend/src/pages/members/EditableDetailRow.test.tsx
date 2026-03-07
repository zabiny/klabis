import '@testing-library/jest-dom';
import {render, screen} from '@testing-library/react';
import {Formik, Form} from 'formik';
import {vi, describe, it, expect} from 'vitest';
import type {HalFormsTemplate} from '../../api';
import {EditableDetailRow} from './EditableDetailRow';

const renderWithFormik = (
    ui: React.ReactElement,
    initialValues: Record<string, unknown> = {}
) => {
    return render(
        <Formik initialValues={initialValues} onSubmit={vi.fn()}>
            <Form>{ui}</Form>
        </Formik>
    );
};

const makeTemplate = (properties: HalFormsTemplate['properties']): HalFormsTemplate => ({
    method: 'PUT',
    properties,
});

describe('EditableDetailRow', () => {
    describe('read-only mode (isEditing=false)', () => {
        it('renders label and children as read-only text', () => {
            renderWithFormik(
                <EditableDetailRow
                    label="Jméno"
                    fieldName="firstName"
                    template={makeTemplate([{name: 'firstName', type: 'text'}])}
                    isEditing={false}
                >
                    Jan
                </EditableDetailRow>
            );
            expect(screen.getByText('Jméno')).toBeInTheDocument();
            expect(screen.getByText('Jan')).toBeInTheDocument();
            expect(screen.queryByRole('textbox')).not.toBeInTheDocument();
        });
    });

    describe('edit mode (isEditing=true)', () => {
        it('renders input when field is in template and not readOnly', () => {
            renderWithFormik(
                <EditableDetailRow
                    label="Jméno"
                    fieldName="firstName"
                    template={makeTemplate([{name: 'firstName', type: 'text', prompt: 'Jméno'}])}
                    isEditing={true}
                >
                    Jan
                </EditableDetailRow>,
                {firstName: 'Jan'}
            );
            const input = screen.getByRole('textbox');
            expect(input).toBeInTheDocument();
            expect(input).toHaveValue('Jan');
        });

        it('stays read-only when field is readOnly in template', () => {
            renderWithFormik(
                <EditableDetailRow
                    label="Reg. číslo"
                    fieldName="registrationNumber"
                    template={makeTemplate([{name: 'registrationNumber', type: 'text', readOnly: true}])}
                    isEditing={true}
                >
                    SKI2601
                </EditableDetailRow>,
                {registrationNumber: 'SKI2601'}
            );
            expect(screen.getByText('SKI2601')).toBeInTheDocument();
            expect(screen.queryByRole('textbox')).not.toBeInTheDocument();
        });

        it('stays read-only when field is not in template', () => {
            renderWithFormik(
                <EditableDetailRow
                    label="Jméno"
                    fieldName="firstName"
                    template={makeTemplate([])}
                    isEditing={true}
                >
                    Jan
                </EditableDetailRow>,
                {firstName: 'Jan'}
            );
            expect(screen.getByText('Jan')).toBeInTheDocument();
            expect(screen.queryByRole('textbox')).not.toBeInTheDocument();
        });

        it('renders email input for email type', () => {
            renderWithFormik(
                <EditableDetailRow
                    label="E-mail"
                    fieldName="email"
                    template={makeTemplate([{name: 'email', type: 'email'}])}
                    isEditing={true}
                >
                    test@example.com
                </EditableDetailRow>,
                {email: 'test@example.com'}
            );
            const input = screen.getByDisplayValue('test@example.com');
            expect(input).toHaveAttribute('type', 'email');
        });

        it('renders tel input for tel type', () => {
            renderWithFormik(
                <EditableDetailRow
                    label="Telefon"
                    fieldName="phone"
                    template={makeTemplate([{name: 'phone', type: 'tel'}])}
                    isEditing={true}
                >
                    +420123456789
                </EditableDetailRow>,
                {phone: '+420123456789'}
            );
            const input = screen.getByDisplayValue('+420123456789');
            expect(input).toHaveAttribute('type', 'tel');
        });

        it('renders date input for date type', () => {
            renderWithFormik(
                <EditableDetailRow
                    label="Datum narození"
                    fieldName="dateOfBirth"
                    template={makeTemplate([{name: 'dateOfBirth', type: 'date'}])}
                    isEditing={true}
                >
                    15. 3. 1990
                </EditableDetailRow>,
                {dateOfBirth: '1990-03-15'}
            );
            const input = screen.getByDisplayValue('1990-03-15');
            expect(input).toHaveAttribute('type', 'date');
        });

        it('renders textarea for textarea type', () => {
            renderWithFormik(
                <EditableDetailRow
                    label="Poznámka"
                    fieldName="note"
                    template={makeTemplate([{name: 'note', type: 'textarea'}])}
                    isEditing={true}
                >
                    Nějaká poznámka
                </EditableDetailRow>,
                {note: 'Nějaká poznámka'}
            );
            expect(screen.getByRole('textbox')).toBeInTheDocument();
            expect(screen.getByDisplayValue('Nějaká poznámka')).toBeInTheDocument();
        });

        it('renders number input for number type', () => {
            renderWithFormik(
                <EditableDetailRow
                    label="Číslo čipu"
                    fieldName="chipNumber"
                    template={makeTemplate([{name: 'chipNumber', type: 'number'}])}
                    isEditing={true}
                >
                    12345
                </EditableDetailRow>,
                {chipNumber: 12345}
            );
            const input = screen.getByRole('spinbutton');
            expect(input).toHaveAttribute('type', 'number');
        });

        it('renders select for property with inline options', () => {
            renderWithFormik(
                <EditableDetailRow
                    label="Pohlaví"
                    fieldName="gender"
                    template={makeTemplate([{
                        name: 'gender',
                        type: 'text',
                        options: {
                            inline: [
                                {value: 'MALE', prompt: 'Muž'},
                                {value: 'FEMALE', prompt: 'Žena'},
                            ]
                        }
                    }])}
                    isEditing={true}
                >
                    Muž
                </EditableDetailRow>,
                {gender: 'MALE'}
            );
            const select = screen.getByRole('combobox');
            expect(select).toBeInTheDocument();
            expect(screen.getByText('Muž')).toBeInTheDocument();
            expect(screen.getByText('Žena')).toBeInTheDocument();
        });

        it('renders checkbox for boolean type', () => {
            renderWithFormik(
                <EditableDetailRow
                    label="Aktivní"
                    fieldName="active"
                    template={makeTemplate([{name: 'active', type: 'boolean'}])}
                    isEditing={true}
                >
                    Ano
                </EditableDetailRow>,
                {active: true}
            );
            const checkbox = screen.getByRole('checkbox');
            expect(checkbox).toBeInTheDocument();
            expect(checkbox).toBeChecked();
        });

        it('uses template prompt as label when editing', () => {
            renderWithFormik(
                <EditableDetailRow
                    label="Jméno"
                    fieldName="firstName"
                    template={makeTemplate([{name: 'firstName', type: 'text', prompt: 'Křestní jméno'}])}
                    isEditing={true}
                >
                    Jan
                </EditableDetailRow>,
                {firstName: 'Jan'}
            );
            expect(screen.getByText('Křestní jméno')).toBeInTheDocument();
        });

        it('marks field as required when template property is required', () => {
            renderWithFormik(
                <EditableDetailRow
                    label="Jméno"
                    fieldName="firstName"
                    template={makeTemplate([{name: 'firstName', type: 'text', required: true}])}
                    isEditing={true}
                >
                    Jan
                </EditableDetailRow>,
                {firstName: 'Jan'}
            );
            const input = screen.getByRole('textbox');
            expect(input).toBeRequired();
        });
    });

    describe('composite fields', () => {
        it('renders sub-fields for address composite when editing', () => {
            renderWithFormik(
                <EditableDetailRow
                    label="Ulice"
                    fieldName="address.street"
                    template={makeTemplate([{name: 'address', type: 'AddressRequest'}])}
                    isEditing={true}
                >
                    Hlavní 15
                </EditableDetailRow>,
                {address: {street: 'Hlavní 15'}}
            );
            const input = screen.getByRole('textbox');
            expect(input).toBeInTheDocument();
            expect(input).toHaveValue('Hlavní 15');
        });

        it('renders sub-fields for guardian composite when editing', () => {
            renderWithFormik(
                <EditableDetailRow
                    label="Jméno"
                    fieldName="guardian.firstName"
                    template={makeTemplate([{name: 'guardian', type: 'GuardianRequest'}])}
                    isEditing={true}
                >
                    Marie
                </EditableDetailRow>,
                {guardian: {firstName: 'Marie'}}
            );
            const input = screen.getByRole('textbox');
            expect(input).toBeInTheDocument();
            expect(input).toHaveValue('Marie');
        });

        it('renders date input for composite date sub-fields', () => {
            renderWithFormik(
                <EditableDetailRow
                    label="Platnost do"
                    fieldName="identityCard.validityDate"
                    template={makeTemplate([{name: 'identityCard', type: 'IdentityCardRequest'}])}
                    isEditing={true}
                >
                    30. 6. 2030
                </EditableDetailRow>,
                {identityCard: {validityDate: '2030-06-30'}}
            );
            const input = screen.getByDisplayValue('2030-06-30');
            expect(input).toHaveAttribute('type', 'date');
        });
    });
});
