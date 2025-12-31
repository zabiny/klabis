import type {InputHTMLAttributes, ReactNode, SelectHTMLAttributes, TextareaHTMLAttributes} from 'react'

/**
 * Common props for all form field wrappers
 */
export interface FieldWrapperProps {
    label?: string
    error?: string
    required?: boolean
    helpText?: string
    disabled?: boolean
    className?: string
}

/**
 * Props for text-based input fields
 */
export interface TextFieldProps extends FieldWrapperProps, Omit<InputHTMLAttributes<HTMLInputElement>, 'className' | 'disabled'> {
    type?: 'text' | 'email' | 'password' | 'number' | 'date' | 'datetime-local' | 'url' | 'tel'
    placeholder?: string
}

/**
 * Props for textarea fields
 */
export interface TextAreaFieldProps extends FieldWrapperProps, Omit<TextareaHTMLAttributes<HTMLTextAreaElement>, 'className' | 'disabled'> {
    placeholder?: string
    rows?: number
}

/**
 * Option item for select, radio, and checkbox groups
 */
export interface SelectOption {
    value: string | number
    label: string
    disabled?: boolean
}

/**
 * Props for select dropdown fields
 */
export interface SelectFieldProps extends FieldWrapperProps, Omit<SelectHTMLAttributes<HTMLSelectElement>, 'className' | 'disabled'> {
    options: SelectOption[]
    placeholder?: string
    multiple?: boolean
}

/**
 * Props for radio group fields
 */
export interface RadioGroupProps extends FieldWrapperProps {
    name: string
    options: SelectOption[]
    value?: string | number
    onChange?: (value: string | number) => void
    direction?: 'horizontal' | 'vertical'
}

/**
 * Props for checkbox group fields
 */
export interface CheckboxGroupProps extends FieldWrapperProps {
    name: string
    options: SelectOption[]
    value?: (string | number)[]
    onChange?: (value: (string | number)[]) => void
    direction?: 'horizontal' | 'vertical'
}

/**
 * Props for single checkbox field
 */
export interface CheckboxFieldProps extends FieldWrapperProps, Omit<InputHTMLAttributes<HTMLInputElement>, 'className' | 'disabled' | 'type' | 'onChange'> {
    checked: boolean
    onChange?: (checked: boolean) => void
}

/**
 * Props for switch/toggle field
 */
export interface SwitchFieldProps extends FieldWrapperProps {
    name: string
    checked: boolean
    onChange?: (checked: boolean) => void
}

/**
 * Props for form control group (groups related fields)
 */
export interface FormControlProps {
    children: ReactNode
    className?: string
}

/**
 * Props for form helper text (error or help text)
 */
export interface FormHelperTextProps {
    error?: boolean
    className?: string
    children: ReactNode
}
