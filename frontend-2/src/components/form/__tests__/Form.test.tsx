import React from 'react';
import {fireEvent, render, screen, waitFor} from '@testing-library/react';
import {Form} from '../Form';
import {Field} from '../Field';

describe('Form Component', () => {
    const mockSubmit = jest.fn();
    const mockValidate = jest.fn();

    beforeEach(() => {
        jest.clearAllMocks();
    });

    const TestInput = ({value, onChange, hasError}: any) => (
        <input
            data-testid="test-input"
            value={value || ''}
            onChange={(e) => onChange(e.target.value)}
            className={hasError ? 'error-input' : 'normal-input'}
        />
    );

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
                <Field name="firstName">
                    {({value}) => <span data-testid="field-value">{value}</span>}
                </Field>
            </Form>
        );

        expect(screen.getByTestId('field-value')).toHaveTextContent('Jan');
    });

    it('should handle field value changes', async () => {
        const formValue = {firstName: ''};

        render(
            <Form value={formValue} onSubmit={mockSubmit}>
                <Field name="firstName">
                    {TestInput}
                </Field>
            </Form>
        );

        const input = screen.getByTestId('test-input');
        fireEvent.change(input, {target: {value: 'Nové jméno'}});

        await waitFor(() => {
            expect((input as HTMLInputElement).value).toBe('Nové jméno');
        });
    });

    it('should call onSubmit with form data when validation passes', async () => {
        const formValue = {firstName: 'Jan'};

        render(
            <Form value={formValue} onSubmit={mockSubmit}>
                <Field name="firstName">{TestInput}</Field>
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
                <Field name="firstName">{TestInput}</Field>
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
                <Field name="firstName">
                    {({value, onChange, hasError, errorMessage}) => (
                        <div>
                            <input
                                data-testid="error-input"
                                value={value || ''}
                                onChange={(e) => onChange(e.target.value)}
                                className={hasError ? 'error-input' : 'normal-input'}
                            />
                            {errorMessage && <span data-testid="field-error">{errorMessage}</span>}
                        </div>
                    )}
                </Field>
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
                <Field name="firstName">{TestInput}</Field>
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
                <Field name="address.city">
                    {({value}) => <span data-testid="city-value">{value}</span>}
                </Field>
                <Field name="address.street">
                    {({value}) => <span data-testid="street-value">{value}</span>}
                </Field>
            </Form>
        );

        expect(screen.getByTestId('city-value')).toHaveTextContent('Praha');
        expect(screen.getByTestId('street-value')).toHaveTextContent('Hlavní 123');
    });

    it('should clear field errors when field value changes', async () => {
        mockValidate.mockReturnValue({firstName: 'Jméno je povinné'});

        render(
            <Form value={{firstName: ''}} onSubmit={mockSubmit} validate={mockValidate}>
                <Field name="firstName">
                    {({value, onChange, errorMessage}) => (
                        <div>
                            <input
                                data-testid="clearing-input"
                                value={value || ''}
                                onChange={(e) => onChange(e.target.value)}
                            />
                            {errorMessage && <span data-testid="clearing-error">{errorMessage}</span>}
                        </div>
                    )}
                </Field>
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

    it('should handle field-level validation', async () => {
        const fieldValidate = jest.fn().mockReturnValue('Field validation error');

        render(
            <Form value={{firstName: 'test'}} onSubmit={mockSubmit}>
                <Field name="firstName" validate={fieldValidate}>
                    {({value, onChange, errorMessage}) => (
                        <div>
                            <input
                                data-testid="field-validated-input"
                                value={value || ''}
                                onChange={(e) => onChange(e.target.value)}
                            />
                            {errorMessage && <span data-testid="field-validation-error">{errorMessage}</span>}
                        </div>
                    )}
                </Field>
            </Form>
        );

        fireEvent.click(screen.getByRole('button', {name: 'Odeslat'}));

        await waitFor(() => {
            expect(fieldValidate).toHaveBeenCalledWith('test');
            expect(screen.getByTestId('field-validation-error')).toHaveTextContent('Field validation error');
            expect(mockSubmit).not.toHaveBeenCalled();
        });
    });

    it('should submit successfully when no validation errors', async () => {
        mockValidate.mockReturnValue(null);

        render(
            <Form value={{firstName: 'Jan'}} onSubmit={mockSubmit} validate={mockValidate}>
                <Field name="firstName">{TestInput}</Field>
            </Form>
        );

        fireEvent.click(screen.getByRole('button', {name: 'Odeslat'}));

        await waitFor(() => {
            expect(mockValidate).toHaveBeenCalledWith({firstName: 'Jan'});
            expect(mockSubmit).toHaveBeenCalledWith({firstName: 'Jan'});
        });
    });
});
