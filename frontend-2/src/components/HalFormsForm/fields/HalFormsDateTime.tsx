import type {ReactElement} from 'react'
import type {FieldProps} from 'formik'
import {Field} from 'formik'
import {TextField} from '../../FormFields'
import type {HalFormsInputProps} from '../types'

/**
 * Gets the local timezone offset in the format +HH:mm or -HH:mm
 */
const getLocalTimezoneOffset = (): string => {
    const offset = new Date().getTimezoneOffset();
    const hours = Math.floor(Math.abs(offset) / 60);
    const minutes = Math.abs(offset) % 60;
    const sign = offset <= 0 ? '+' : '-';
    return `${sign}${String(hours).padStart(2, '0')}:${String(minutes).padStart(2, '0')}`;
};

/**
 * Converts ISO datetime string to datetime-local format
 * Handles ISO strings with timezone information (e.g., '2025-12-05T00:00:00+01:00')
 * Returns format suitable for datetime-local input (e.g., '2025-12-05T00:00')
 */
const convertToDatetimeLocalFormat = (value: string | undefined): string => {
    if (!value || typeof value !== 'string') {
        return '';
    }

    // Match ISO datetime formats with optional timezone
    // Matches: YYYY-MM-DDTHH:mm or YYYY-MM-DDTHH:mm:ss or YYYY-MM-DDTHH:mm:ss.sss
    // With optional timezone: Z, +HH:mm, -HH:mm, +HHMM, -HHMM, +HH, -HH
    const isoDateTimeRegex = /^(\d{4}-\d{2}-\d{2}T\d{2}:\d{2})(?::\d{2})?(?:\.\d+)?(?:Z|[+\-]\d{2}:?\d{2})?$/;

    if (isoDateTimeRegex.test(value)) {
        // Extract just the date and time part (YYYY-MM-DDTHH:mm)
        const match = value.match(/^(\d{4}-\d{2}-\d{2}T\d{2}:\d{2})/);
        if (match) {
            return match[1];
        }
    }

    // Return as-is if it doesn't match ISO format (already in datetime-local format)
    return value;
};

/**
 * Converts datetime-local format to ISO datetime with timezone offset
 * Input format: YYYY-MM-DDTHH:mm
 * Output format: YYYY-MM-DDTHH:mm:00+HH:mm (with local timezone offset)
 */
const convertToISODatetimeWithTimezone = (value: string | undefined): string => {
    if (!value || typeof value !== 'string') {
        return '';
    }

    // Check if already in ISO format with timezone
    const hasTimezone = /(?:Z|[+\-]\d{2}:?\d{2})$/.test(value);
    if (hasTimezone) {
        return value;
    }

    // Check if it's in datetime-local format (YYYY-MM-DDTHH:mm)
    const datetimeLocalRegex = /^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}$/;
    if (datetimeLocalRegex.test(value)) {
        const timezone = getLocalTimezoneOffset();
        return `${value}:00${timezone}`;
    }

    // Return as-is if format is not recognized
    return value;
};

/**
 * HalFormsDateTime component - datetime input for HAL+Forms
 * Allows user to select both date and time
 * Converts ISO datetime strings with timezone to datetime-local format for display
 * Converts datetime-local input back to ISO format with timezone for submission
 * Uses Formik Field and FormFields TextField abstraction
 */
export const HalFormsDateTime = ({
                                     prop,
                                     errorText,
                                 }: HalFormsInputProps): ReactElement => {
    return (
        <Field
            name={prop.name}
            validate={() => undefined}
            render={(fieldProps: FieldProps<unknown>) => {
                const fieldValue = fieldProps.field.value as string | undefined;
                const convertedValue = convertToDatetimeLocalFormat(fieldValue);

                const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
                    const datetimeLocalValue = e.target.value;
                    // Convert back to ISO format with timezone before storing in Formik
                    const isoValue = convertToISODatetimeWithTimezone(datetimeLocalValue);
                    fieldProps.field.onChange({
                        target: {name: fieldProps.field.name, value: isoValue}
                    });
                };

                return (
                    <TextField
                        {...fieldProps.field}
                        value={convertedValue}
                        onChange={handleChange}
                        type="datetime-local"
                        label={prop.prompt || prop.name}
                        disabled={prop.readOnly || false}
                        required={prop.required}
                        error={errorText}
                        className="w-full"
                    />
                );
            }}
        />
    )
}

HalFormsDateTime.displayName = 'HalFormsDateTime'
