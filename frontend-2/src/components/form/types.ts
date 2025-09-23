// Types used by the form components.
import React from 'react';

/**
 * Props for a single field component.
 */
export interface FieldProps<T> {
    /** Unique name of the field, used for nested value lookup. */
    name: string;
    /** Current value of the field. */
    value?: T | null;
    /** Change handler for the field. */
    onChange?: (value: T) => void;
    /** Validation function for the field. */
    validate?: (value: T) => string | null;
    initialErrorMessage?: string;
    required?: boolean;
}

export interface FieldRenderProps<T> {
    value: T | null | undefined;
    onChange: (v: T) => void;
    hasError: boolean;
    errorMessage?: string;
}

/**
 * Validation errors map where the key is the field name.
 */
export type ValidationErrors = Record<string, string>;

/**
 * Generic props for the Form component.
 *
 * @template T - The shape of the form's value object.
 */
export interface FormProps<T> {
    /** Initial form value. */
    value: T;
    /** Callback invoked when the form is successfully submitted. */
    onSubmit: (value: T) => void;
    /** Optional form‑level validation function. */
    validate?: (value: T) => ValidationErrors | undefined;
    /** Form children – typically Field components. */
    children: React.ReactNode;
    /** Optional ARIA label for accessibility. */
    ariaLabel?: string;
}