import React from 'react';
import {fireEvent, render, screen, waitFor} from '@testing-library/react';
import {Form} from '../Form';
import {Field} from '../Field';
import {ValidationErrors} from "../types";

interface PersonData {
    firstName: string;
    lastName: string;
    address: {
        city: string;
        street: string;
    };
    email: string;
}

describe('Form Integration Tests', () => {
    const mockSubmit = jest.fn();

    beforeEach(() => {
        jest.clearAllMocks();
    });

    const validateForm = (data: PersonData): ValidationErrors => {
        const errors: Record<string, string> = {};

        if (!data.firstName) {
            errors.firstName = 'Jméno je povinné';
        }

        if (!data.lastName) {
            errors.lastName = 'Příjmení je povinné';
        }

        if (!data.address?.city) {
            errors['address.city'] = 'Město je povinné';
        }

        if (data.email && !data.email.includes('@')) {
            errors.email = 'Neplatný email';
        }

        if (data.firstName === 'admin' && data.lastName === 'admin') {
            errors.general = 'Admin účet není povolen';
        }

        return Object.keys(errors).length > 0 ? errors : {};
    };

    const CompleteForm: React.FC<{ initialData: PersonData }> = ({initialData}) => (
        <Form value={initialData} onSubmit={mockSubmit} validate={validateForm}>
            <Field name="firstName"/>

            <Field name="lastName"/>

            <Field name="address.city"/>

            <Field
                name="email"
                inputType="email"
                validate={(value) => value && !value.includes('@') ? 'Neplatný email formát' : null}/>
        </Form>
    );

    it('should complete full form workflow successfully', async () => {
        const initialData: PersonData = {
            firstName: '',
            lastName: '',
            address: {city: '', street: ''},
            email: ''
        };

        render(<CompleteForm initialData={initialData}/>);

        // Vyplnit všechna pole
        fireEvent.change(screen.getByTestId('firstName'), {target: {value: 'Jan'}});
        fireEvent.change(screen.getByTestId('lastName'), {target: {value: 'Novák'}});
        fireEvent.change(screen.getByTestId('city'), {target: {value: 'Praha'}});
        fireEvent.change(screen.getByTestId('email'), {target: {value: 'jan@example.com'}});

        // Odeslat formulář
        fireEvent.click(screen.getByRole('button', {name: 'Odeslat'}));

        await waitFor(() => {
            expect(mockSubmit).toHaveBeenCalledWith({
                firstName: 'Jan',
                lastName: 'Novák',
                address: {city: 'Praha', street: ''},
                email: 'jan@example.com'
            });
        });
    });

    it('should show multiple validation errors', async () => {
        const initialData: PersonData = {
            firstName: '',
            lastName: '',
            address: {city: '', street: ''},
            email: 'invalid-email'
        };

        render(<CompleteForm initialData={initialData}/>);

        fireEvent.click(screen.getByRole('button', {name: 'Odeslat'}));

        await waitFor(() => {
            expect(screen.getByTestId('firstName-error')).toHaveTextContent('Jméno je povinné');
            expect(screen.getByTestId('lastName-error')).toHaveTextContent('Příjmení je povinné');
            expect(screen.getByTestId('city-error')).toHaveTextContent('Město je povinné');
            expect(screen.getByTestId('email-error')).toHaveTextContent('Neplatný email formát');
            expect(mockSubmit).not.toHaveBeenCalled();
        });
    });

    it('should clear errors progressively as fields are fixed', async () => {
        const initialData: PersonData = {
            firstName: '',
            lastName: '',
            address: {city: '', street: ''},
            email: ''
        };

        render(<CompleteForm initialData={initialData}/>);

        // Vyvolat chyby
        fireEvent.click(screen.getByRole('button', {name: 'Odeslat'}));

        await waitFor(() => {
            expect(screen.getByTestId('firstName-error')).toBeInTheDocument();
        });

        // Opravit jméno
        fireEvent.change(screen.getByTestId('firstName'), {target: {value: 'Jan'}});

        await waitFor(() => {
            expect(screen.queryByTestId('firstName-error')).not.toBeInTheDocument();
            expect(screen.getByTestId('lastName-error')).toBeInTheDocument(); // Stále existuje
        });
    });

    it('should show form-level errors for orphan validation results', async () => {
        const initialData: PersonData = {
            firstName: 'admin',
            lastName: 'admin',
            address: {city: 'Praha', street: ''},
            email: 'admin@example.com'
        };

        render(<CompleteForm initialData={initialData}/>);

        fireEvent.click(screen.getByRole('button', {name: 'Odeslat'}));

        await waitFor(() => {
            expect(screen.getByText('Admin účet není povolen')).toBeInTheDocument();
            expect(mockSubmit).not.toHaveBeenCalled();
        });
    });

    it('should prioritize field-level validation over form-level for same field', async () => {
        const initialData: PersonData = {
            firstName: 'Jan',
            lastName: 'Novák',
            address: {city: 'Praha', street: ''},
            email: 'invalid'
        };

        render(<CompleteForm initialData={initialData}/>);

        fireEvent.click(screen.getByRole('button', {name: 'Odeslat'}));

        await waitFor(() => {
            // Field-level validace má přednost
            expect(screen.getByTestId('email-error')).toHaveTextContent('Neplatný email formát');
            expect(mockSubmit).not.toHaveBeenCalled();
        });
    });

    it('should handle complex nested data updates', async () => {
        const initialData: PersonData = {
            firstName: 'Jan',
            lastName: 'Novák',
            address: {city: 'Brno', street: 'Stará 1'},
            email: 'jan@example.com'
        };

        render(<CompleteForm initialData={initialData}/>);

        // Změnit město
        fireEvent.change(screen.getByTestId('city'), {target: {value: 'Ostrava'}});

        fireEvent.click(screen.getByRole('button', {name: 'Odeslat'}));

        await waitFor(() => {
            expect(mockSubmit).toHaveBeenCalledWith({
                firstName: 'Jan',
                lastName: 'Novák',
                address: {city: 'Ostrava', street: 'Stará 1'}, // Město změněno, ulice zůstala
                email: 'jan@example.com'
            });
        });
    });
});
