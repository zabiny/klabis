import React from 'react';
import {fireEvent, render, screen} from '@testing-library/react';
import {Field} from '../Field';

describe('Field Component', () => {
    const defaultProps = {
        name: 'testField',
        children: ({value, onChange, hasError, errorMessage}: any) => (
            <div>
                <input
                    data-testid="field-input"
                    value={value || ''}
                    onChange={(e) => onChange(e.target.value)}
                    className={hasError ? 'error' : ''}
                />
                {errorMessage && <span data-testid="error-message">{errorMessage}</span>}
            </div>
        )
    };

    it('should render without errors', () => {
        render(<Field {...defaultProps} />);
        expect(screen.getByTestId('field-input')).toBeInTheDocument();
    });

    it('should display the provided value', () => {
        render(<Field {...defaultProps} value="test value"/>);
        const input = screen.getByTestId('field-input') as HTMLInputElement;
        expect(input.value).toBe('test value');
    });

    it('should call onChange when input changes', () => {
        const mockOnChange = jest.fn();
        render(<Field {...defaultProps} onChange={mockOnChange}/>);

        const input = screen.getByTestId('field-input');
        fireEvent.change(input, {target: {value: 'new value'}});

        expect(mockOnChange).toHaveBeenCalledWith('new value');
    });

    it('should display error message when provided', () => {
        render(<Field {...defaultProps} errorMessage="This field is required"/>);

        expect(screen.getByTestId('error-message')).toHaveTextContent('This field is required');
        expect(screen.getByText('This field is required')).toHaveClass('error-message');
    });

    it('should set hasError to true when errorMessage is provided', () => {
        render(<Field {...defaultProps} errorMessage="Error occurred"/>);

        const input = screen.getByTestId('field-input');
        expect(input).toHaveClass('error');
    });

    it('should set hasError to false when no errorMessage', () => {
        render(<Field {...defaultProps} />);

        const input = screen.getByTestId('field-input');
        expect(input).not.toHaveClass('error');
    });

    it('should handle undefined/null values gracefully', () => {
        render(<Field {...defaultProps} value={null}/>);
        const input = screen.getByTestId('field-input') as HTMLInputElement;
        expect(input.value).toBe('');
    });
});
