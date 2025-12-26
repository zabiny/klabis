import '@testing-library/jest-dom';
import React from 'react';
import {render, screen} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {SwitchField} from './SwitchField';

describe('SwitchField', () => {
    it('should call onChange when clicked', async () => {
        const handleChange = jest.fn();
        render(
            <SwitchField
                name="test"
                label="Test Switch"
                checked={false}
                onChange={handleChange}
            />
        );

        const switchElement = screen.getByRole('switch');
        await userEvent.click(switchElement);

        // Should be called with !checked (which is !false = true)
        expect(handleChange).toHaveBeenCalledWith(true);
    });
});
