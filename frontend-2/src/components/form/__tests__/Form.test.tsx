import {fireEvent, render, screen, waitFor} from '@testing-library/react';
import {Form} from '../Form';
import {Field} from '../Field';

describe('Form Component', () => {
    const mockSubmit = jest.fn();
    const mockValidate = jest.fn();

    beforeEach(() => {
        jest.clearAllMocks();
    });

    it('should render form with submit button', () => {
        render(
            <Form value={{}} onSubmit={mockSubmit}>
                <div>Test content</div>
            </Form>
        );

        expect(screen.getByRole('button', {name: 'Odeslat'})).toBeInTheDocument();
        expect(screen.getByRole('form', {name: "Klabis form"})).toBeInTheDocument();
    });

    it('should pass correct value to field component', () => {
        const formValue = {firstName: 'Jan'};

        render(
            <Form value={formValue} onSubmit={mockSubmit}>
                <Field name="firstName"/>
            </Form>
        );

        expect(screen.getByRole('textbox')).toHaveAttribute('value', 'Jan');
    });

    it('should handle field value changes', async () => {
        const formValue = {firstName: ''};

        render(
            <Form value={formValue} onSubmit={mockSubmit}>
                <Field name="firstName"/>
            </Form>
        );

        const input = screen.getByRole('textbox');
        fireEvent.change(input, {target: {value: 'Nové jméno'}});

        await waitFor(() => {
            expect((input as HTMLInputElement).value).toBe('Nové jméno');
        });
    });

    it('should call onSubmit with form data when validation passes', async () => {
        const formValue = {firstName: 'Jan'};

        render(
            <Form value={formValue} onSubmit={mockSubmit}>
                <Field name="firstName"/>
            </Form>
        );

        fireEvent.click(screen.getByRole('button', {name: 'Odeslat'}));

        await waitFor(() => {
            expect(mockSubmit).toHaveBeenCalledWith({firstName: 'Jan'});
        });
    });

    it('should not call onSubmit when form validation fails', async () => {
        mockValidate.mockReturnValue({firstName: 'Jméno je povinné'});

        render(
            <Form value={{firstName: ''}} onSubmit={mockSubmit} validate={mockValidate}>
                <Field name="firstName"/>
            </Form>
        );

        fireEvent.click(screen.getByRole('button', {name: 'Odeslat'}));

        await waitFor(() => {
            expect(mockValidate).toHaveBeenCalled();
            expect(mockSubmit).not.toHaveBeenCalled();
        });
    });

    it('should display field-level errors', async () => {
        mockValidate.mockReturnValue({firstName: 'Jméno je povinné'});

        render(
            <Form value={{firstName: ''}} onSubmit={mockSubmit} validate={mockValidate}>
                <Field name="firstName"/>
            </Form>
        );

        fireEvent.click(screen.getByRole('button', {name: 'Odeslat'}));

        await waitFor(() => {
            expect(screen.getByTestId('field-error')).toHaveTextContent('Jméno je povinné');
            expect(screen.getByTestId('error-input')).toHaveClass('error-input');
        });
    });

    it('should display form-level errors for orphan validation errors', async () => {
        mockValidate.mockReturnValue({
            firstName: 'Jméno je povinné',
            general: 'Obecná chyba formuláře'
        });

        render(
            <Form value={{firstName: ''}} onSubmit={mockSubmit} validate={mockValidate}>
                <Field name="firstName"/>
            </Form>
        );

        fireEvent.click(screen.getByRole('button', {name: 'Odeslat'}));

        await waitFor(() => {
            expect(screen.getByText('Obecná chyba formuláře')).toBeInTheDocument();
        });
    });

    it('should handle nested field values', () => {
        const formValue = {
            address: {
                city: 'Praha',
                street: 'Hlavní 123'
            }
        };

        render(
            <Form value={formValue} onSubmit={mockSubmit}>
                <Field name="address.city"/>
                <Field name="address.street"/>
            </Form>
        );

        expect(screen.getByRole('textbox', {name: 'address.city'})).toHaveAttribute('value', 'Praha');
        expect(screen.getByRole('textbox', {name: 'address.street'})).toHaveAttribute('value', 'Hlavní 123');
    });

    it('should clear field errors when field value changes', async () => {
        mockValidate.mockReturnValue({firstName: 'Jméno je povinné'});

        render(
            <Form value={{firstName: ''}} onSubmit={mockSubmit} validate={mockValidate}>
                <Field name="firstName"/>
            </Form>
        );

        // Vyvolat chybu
        fireEvent.click(screen.getByRole('button', {name: 'Odeslat'}));
        await waitFor(() => {
            expect(screen.getByTestId('clearing-error')).toBeInTheDocument();
        });

        // Změnit hodnotu pole
        fireEvent.change(screen.getByTestId('clearing-input'), {target: {value: 'Nová hodnota'}});

        // Chyba by měla zmizet
        await waitFor(() => {
            expect(screen.queryByTestId('clearing-error')).not.toBeInTheDocument();
        });
    });

    it('should validate field-level validations when submitting form', async () => {
        const fieldValidate = jest.fn().mockReturnValue('Field validation error');

        render(
            <Form value={{firstName: 'test'}} onSubmit={mockSubmit}>
                <Field name="firstName" validate={fieldValidate}/>
            </Form>
        );

        fireEvent.click(screen.getByRole('button', {name: 'Odeslat'}));

        await waitFor(() => {
            expect(fieldValidate).toHaveBeenCalledWith('test');
            expect(screen.getByTestId('error-message')).toHaveTextContent('Field validation error');
            expect(mockSubmit).not.toHaveBeenCalled();
        });
    });

    it('should submit successfully when no validation errors', async () => {
        mockValidate.mockReturnValue(null);

        render(
            <Form value={{firstName: 'Jan'}} onSubmit={mockSubmit} validate={mockValidate}>
                <Field name="firstName"/>
            </Form>
        );

        fireEvent.click(screen.getByRole('button', {name: 'Odeslat'}));

        await waitFor(() => {
            expect(mockValidate).toHaveBeenCalledWith({firstName: 'Jan'});
            expect(mockSubmit).toHaveBeenCalledWith({firstName: 'Jan'});
        });
    });
});
