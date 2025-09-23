import React from 'react';
import {fireEvent, render, screen} from '@testing-library/react';
import {Field} from '../Field';

const expectFieldError = function (expectedError: string) {
    expect(screen.getByTestId('error-message')).toHaveTextContent(expectedError);
    expect(screen.getByText(expectedError)).toHaveClass('error-message');
    const input = screen.getByTestId('field-input');
    expect(input).toHaveClass('error');
}

describe('Field Component', () => {
    it('should render without errors', () => {
        render(<Field name={"firstName"}/>);
        expect(screen.getByTestId('field-input')).toBeInTheDocument();
    });

    it('should display the provided value', () => {
        render(<Field name={"firstName"} value="test value"/>);
        const input = screen.getByTestId('field-input') as HTMLInputElement;
        expect(input.value).toBe('test value');
    });

    it('should display initial error message if provided', () => {
        render(<Field name={"firstName"} value="test value" initialErrorMessage={"Chybne jmeno"}/>);
        expectFieldError('Chybne jmeno');
    });

    it('should display field label text', () => {
        render(<Field name={"firstName"} value="test value"/>);
        const legend = screen.getByText('firstName') as HTMLLegendElement;
        expect(legend).toBeVisible();
    });

    it('should call onChange when input changes', () => {
        const mockOnChange = jest.fn();
        render(<Field name={"firstName"} onChange={mockOnChange}/>);

        const input = screen.getByTestId('field-input');
        fireEvent.change(input, {target: {value: 'new value'}});

        expect(mockOnChange).toHaveBeenCalledWith('new value');
    });

    it('should mark field as errored after value is changed to value violating given validate function', () => {
        render(<Field name={"firstName"} value="David" validate={() => "This field is required"}/>);

        const input = screen.getByTestId('field-input');
        fireEvent.change(input, {target: {value: 'Something else'}})

        expectFieldError('This field is required');
    });

    it('should set hasError to false when no errorMessage', () => {
        render(<Field name={"firstName"}/>);

        const input = screen.getByTestId('field-input');
        expect(input).not.toHaveClass('error');
    });

    it('should handle undefined/null values gracefully', () => {
        render(<Field name={"firstName"} value={null}/>);
        const input = screen.getByTestId('field-input') as HTMLInputElement;
        expect(input.value).toBe('');
    });
});
