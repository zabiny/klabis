import '@testing-library/jest-dom';
import {render, screen} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {Form, Formik} from 'formik';
import type {HalFormsProperty} from '../../../api';
import {HalFormsDateTime} from './HalFormsDateTime';
import type {HalFormsInputProps} from '../types';
import {vi} from 'vitest';

describe('HalFormsDateTime Component', () => {
    const createHalFormsProperty = (overrides = {}): HalFormsProperty => ({
        name: 'eventDateTime',
        prompt: 'Event Date and Time',
        type: 'datetime',
        required: false,
        readOnly: false,
        value: undefined,
        ...overrides,
    });

    const renderWithFormik = (
        prop: HalFormsProperty,
        initialValue: unknown = ''
    ) => {
        const fieldProps: HalFormsInputProps = {
            prop,
            errorText: undefined,
            subElementProps: vi.fn(),
        };

        return render(
            <Formik
                initialValues={{[prop.name]: initialValue}}
                onSubmit={vi.fn()}
            >
                <Form>
                    <HalFormsDateTime {...fieldProps} />
                </Form>
            </Formik>
        );
    };

    describe('Rendering', () => {
        it('should render a datetime-local input field', () => {
            const prop = createHalFormsProperty();
            renderWithFormik(prop);

            const input = document.querySelector('input[type="datetime-local"]') as HTMLInputElement;
            expect(input).toBeInTheDocument();
            expect(input.type).toBe('datetime-local');
        });

        it('should display the label from prop.prompt', () => {
            const prop = createHalFormsProperty({prompt: 'Meeting Time'});
            renderWithFormik(prop);

            const label = screen.getByText('Meeting Time');
            expect(label).toBeInTheDocument();
        });

        it('should use prop.name as label when prompt is not provided', () => {
            const prop = createHalFormsProperty({prompt: undefined});
            renderWithFormik(prop);

            const label = screen.getByText('eventDateTime');
            expect(label).toBeInTheDocument();
        });

        it('should display the field as disabled when readOnly is true', () => {
            const prop = createHalFormsProperty({readOnly: true});
            renderWithFormik(prop);

            const input = document.querySelector('input[type="datetime-local"]') as HTMLInputElement;
            expect(input).toBeDisabled();
        });

        it('should display the field as enabled when readOnly is false', () => {
            const prop = createHalFormsProperty({readOnly: false});
            renderWithFormik(prop);

            const input = document.querySelector('input[type="datetime-local"]') as HTMLInputElement;
            expect(input).not.toBeDisabled();
        });
    });

    describe('Value Handling', () => {
        it('should display empty value when no initial value is provided', () => {
            const prop = createHalFormsProperty();
            renderWithFormik(prop, '');

            const input = document.querySelector('input[type="datetime-local"]') as HTMLInputElement;
            expect(input.value).toBe('');
        });

        it('should display simple datetime format without timezone', () => {
            const prop = createHalFormsProperty();
            const isoDateTime = '2025-12-05T14:30';
            renderWithFormik(prop, isoDateTime);

            const input = document.querySelector('input[type="datetime-local"]') as HTMLInputElement;
            expect(input.value).toBe(isoDateTime);
        });

        it('should convert ISO datetime with timezone offset to datetime-local format', () => {
            const prop = createHalFormsProperty();
            const isoDateTimeWithTz = '2025-12-05T00:00:00+01:00';
            const expectedValue = '2025-12-05T00:00';
            renderWithFormik(prop, isoDateTimeWithTz);

            const input = document.querySelector('input[type="datetime-local"]') as HTMLInputElement;
            expect(input.value).toBe(expectedValue);
        });

        it('should convert ISO datetime with negative timezone offset', () => {
            const prop = createHalFormsProperty();
            const isoDateTimeWithTz = '2025-12-05T00:00:00-05:00';
            const expectedValue = '2025-12-05T00:00';
            renderWithFormik(prop, isoDateTimeWithTz);

            const input = document.querySelector('input[type="datetime-local"]') as HTMLInputElement;
            expect(input.value).toBe(expectedValue);
        });

        it('should handle ISO datetime with Z timezone indicator', () => {
            const prop = createHalFormsProperty();
            const isoDateTimeWithZ = '2025-12-05T14:30:00Z';
            const expectedValue = '2025-12-05T14:30';
            renderWithFormik(prop, isoDateTimeWithZ);

            const input = document.querySelector('input[type="datetime-local"]') as HTMLInputElement;
            expect(input.value).toBe(expectedValue);
        });

        it('should handle ISO datetime with seconds', () => {
            const prop = createHalFormsProperty();
            const isoDateTimeWithSeconds = '2025-12-05T14:30:45+01:00';
            const expectedValue = '2025-12-05T14:30';
            renderWithFormik(prop, isoDateTimeWithSeconds);

            const input = document.querySelector('input[type="datetime-local"]') as HTMLInputElement;
            expect(input.value).toBe(expectedValue);
        });

        it('should handle ISO datetime with milliseconds', () => {
            const prop = createHalFormsProperty();
            const isoDateTimeWithMs = '2025-12-05T14:30:45.123+01:00';
            const expectedValue = '2025-12-05T14:30';
            renderWithFormik(prop, isoDateTimeWithMs);

            const input = document.querySelector('input[type="datetime-local"]') as HTMLInputElement;
            expect(input.value).toBe(expectedValue);
        });
    });

    describe('Attributes', () => {
        it('should set required attribute when prop.required is true', () => {
            const prop = createHalFormsProperty({required: true});
            renderWithFormik(prop);

            const input = document.querySelector('input[type="datetime-local"]') as HTMLInputElement;
            expect(input).toBeRequired();
        });

        it('should not set required attribute when prop.required is false', () => {
            const prop = createHalFormsProperty({required: false});
            renderWithFormik(prop);

            const input = document.querySelector('input[type="datetime-local"]') as HTMLInputElement;
            expect(input).not.toBeRequired();
        });
    });

    describe('Error Handling', () => {
        it('should display error text when provided', () => {
            const prop = createHalFormsProperty();
            const fieldProps: HalFormsInputProps = {
                prop,
                errorText: 'Invalid datetime format',
                subElementProps: vi.fn(),
            };

            render(
                <Formik
                    initialValues={{[prop.name]: ''}}
                    onSubmit={vi.fn()}
                >
                    <Form>
                        <HalFormsDateTime {...fieldProps} />
                    </Form>
                </Formik>
            );

            expect(screen.getByText('Invalid datetime format')).toBeInTheDocument();
        });
    });

    describe('Submission Format', () => {
        it('should convert datetime-local value to ISO format with timezone on change', async () => {
            const prop = createHalFormsProperty();
            const onSubmit = vi.fn();
            const fieldProps: HalFormsInputProps = {
                prop,
                errorText: undefined,
                subElementProps: vi.fn(),
            };

            const user = userEvent.setup();

            render(
                <Formik
                    initialValues={{[prop.name]: ''}}
                    onSubmit={onSubmit}
                >
                    <Form>
                        <HalFormsDateTime {...fieldProps} />
                        <button type="submit">Submit</button>
                    </Form>
                </Formik>
            );

            const input = document.querySelector('input[type="datetime-local"]') as HTMLInputElement;
            await user.type(input, '2025-12-12T14:30');

            const submitButton = screen.getByText('Submit');
            await user.click(submitButton);

            // Verify that the submitted value is in ISO format with timezone
            expect(onSubmit).toHaveBeenCalledWith(
                expect.objectContaining({
                    eventDateTime: expect.stringMatching(/^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:00[+\-]\d{2}:\d{2}$/)
                }),
                expect.anything()
            );
        });

        it('should preserve ISO format value if already in correct format', async () => {
            const prop = createHalFormsProperty();
            const onSubmit = vi.fn();
            const fieldProps: HalFormsInputProps = {
                prop,
                errorText: undefined,
                subElementProps: vi.fn(),
            };

            const isoValue = '2025-12-12T14:30:00+01:00';
            const user = userEvent.setup();

            render(
                <Formik
                    initialValues={{[prop.name]: isoValue}}
                    onSubmit={onSubmit}
                >
                    <Form>
                        <HalFormsDateTime {...fieldProps} />
                        <button type="submit">Submit</button>
                    </Form>
                </Formik>
            );

            const submitButton = screen.getByText('Submit');
            await user.click(submitButton);

            // Value should be unchanged when submitting without modification
            expect(onSubmit).toHaveBeenCalledWith(
                expect.objectContaining({
                    eventDateTime: isoValue
                }),
                expect.anything()
            );
        });

        it('should convert modified value back to ISO format with timezone', async () => {
            const prop = createHalFormsProperty();
            const onSubmit = vi.fn();
            const fieldProps: HalFormsInputProps = {
                prop,
                errorText: undefined,
                subElementProps: vi.fn(),
            };

            const initialIsoValue = '2025-12-05T10:00:00+01:00';
            const user = userEvent.setup();

            render(
                <Formik
                    initialValues={{[prop.name]: initialIsoValue}}
                    onSubmit={onSubmit}
                >
                    <Form>
                        <HalFormsDateTime {...fieldProps} />
                        <button type="submit">Submit</button>
                    </Form>
                </Formik>
            );

            const input = document.querySelector('input[type="datetime-local"]') as HTMLInputElement;
            // Clear and set new value
            await user.clear(input);
            await user.type(input, '2025-12-12T14:30');

            const submitButton = screen.getByText('Submit');
            await user.click(submitButton);

            // Verify submitted value is in ISO format with timezone
            expect(onSubmit).toHaveBeenCalledWith(
                expect.objectContaining({
                    eventDateTime: expect.stringMatching(/^2025-12-12T14:30:00[+\-]\d{2}:\d{2}$/)
                }),
                expect.anything()
            );
        });
    });
});
