import '@testing-library/jest-dom';
import {render, screen, waitFor} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {Form, Formik} from 'formik';
import {HalFormsBoolean} from './HalFormsBoolean.tsx';
import type {HalFormsProperty} from '../../../../api';
import type {HalFormsInputProps} from '../types.ts';
import {vi} from 'vitest';

describe('HalFormsBoolean', () => {
    const createHalFormsProperty = (overrides = {}): HalFormsProperty => ({
        name: 'isActive',
        prompt: 'Is Active',
        type: 'checkbox',
        required: false,
        readOnly: false,
        value: undefined,
        ...overrides,
    });

    const renderWithFormik = (
        prop: HalFormsProperty,
        initialValue: boolean = false,
        onSubmit = vi.fn(),
        errorText?: string
    ) => {
        const fieldProps: HalFormsInputProps = {
            prop,
            errorText,
            subElementProps: vi.fn(),
        };

        return render(
            <Formik initialValues={{[prop.name]: initialValue}} onSubmit={onSubmit}>
                <Form>
                    <HalFormsBoolean {...fieldProps} />
                    <button type="submit">Submit</button>
                </Form>
            </Formik>
        );
    };

    it('should render switch with correct label', () => {
        const prop = createHalFormsProperty();
        renderWithFormik(prop);
        expect(screen.getByText('Is Active')).toBeInTheDocument();
    });

    it('should be unchecked initially when value is false', () => {
        const prop = createHalFormsProperty();
        renderWithFormik(prop, false);
        const switchElement = screen.getByRole('switch');
        expect(switchElement).toHaveAttribute('aria-checked', 'false');
    });

    it('should be checked initially when value is true', () => {
        const prop = createHalFormsProperty();
        renderWithFormik(prop, true);
        const switchElement = screen.getByRole('switch');
        expect(switchElement).toHaveAttribute('aria-checked', 'true');
    });

    it('should toggle value when clicked', async () => {
        const prop = createHalFormsProperty();
        const onSubmit = vi.fn();
        renderWithFormik(prop, false, onSubmit);

        const switchElement = screen.getByRole('switch');

        // Initially unchecked
        expect(switchElement).toHaveAttribute('aria-checked', 'false');

        // Click to toggle
        await userEvent.click(switchElement);

        // Should be checked now
        await waitFor(() => {
            expect(switchElement).toHaveAttribute('aria-checked', 'true');
        });

        // Click to toggle back
        await userEvent.click(switchElement);

        // Should be unchecked again
        await waitFor(() => {
            expect(switchElement).toHaveAttribute('aria-checked', 'false');
        });
    });

    it('should submit correct value when form is submitted', async () => {
        const prop = createHalFormsProperty();
        const onSubmit = vi.fn();
        renderWithFormik(prop, false, onSubmit);

        const switchElement = screen.getByRole('switch');
        const submitButton = screen.getByText('Submit');

        // Toggle the switch
        await userEvent.click(switchElement);

        // Wait for the switch to update
        await waitFor(() => {
            expect(switchElement).toHaveAttribute('aria-checked', 'true');
        });

        // Submit the form
        await userEvent.click(submitButton);

        // Check submitted value
        await waitFor(() => {
            expect(onSubmit).toHaveBeenCalledWith(
                {isActive: true},
                expect.anything()
            );
        });
    });

    it('should handle multiple rapid clicks correctly', async () => {
        const prop = createHalFormsProperty();
        const onSubmit = vi.fn();
        renderWithFormik(prop, false, onSubmit);

        const switchElement = screen.getByRole('switch');

        // Click multiple times rapidly
        await userEvent.click(switchElement);
        await userEvent.click(switchElement);
        await userEvent.click(switchElement);

        // Should end up checked (odd number of clicks)
        await waitFor(() => {
            expect(switchElement).toHaveAttribute('aria-checked', 'true');
        });
    });

    it('should be disabled when readOnly is true', () => {
        const prop = createHalFormsProperty({readOnly: true});
        renderWithFormik(prop, false);

        const switchElement = screen.getByRole('switch');
        expect(switchElement).toHaveAttribute('aria-disabled', 'true');
    });

    it('should display error text when provided', () => {
        const prop = createHalFormsProperty();
        renderWithFormik(prop, false, vi.fn(), 'This field is required');

        expect(screen.getByText('This field is required')).toBeInTheDocument();
    });

    it('should use property name as label when prompt is not provided', () => {
        const prop = createHalFormsProperty({prompt: undefined});
        renderWithFormik(prop, false);

        expect(screen.getByText('isActive')).toBeInTheDocument();
    });
});
