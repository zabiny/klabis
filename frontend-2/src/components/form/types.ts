import {ReactNode} from 'react';

export interface FormProps {
    value: Record<string, any>;
    onSubmit: (value: Record<string, any>) => void;
    validate?: (value: Record<string, any>) => Record<string, string> | null | undefined;
    children: ReactNode;
    ariaLabel?: string
}

export interface FieldProps {
    name: string;
    value?: any;
    onChange?: (value: any) => void;
    validate?: (value: any) => string | null | undefined;
    errorMessage?: string;
    children: (props: {
        value: any;
        onChange: (value: any) => void;
        hasError: boolean;
        errorMessage?: string;
    }) => ReactNode;
}

export type ValidationErrors = Record<string, string>;
