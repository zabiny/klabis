import clsx from 'clsx'
import type {FormControlProps, FormHelperTextProps} from './types.ts'

/**
 * FormControl component for grouping form fields
 * Replaces MUI FormControl
 */
export const FormControl = ({children, className}: FormControlProps) => {
    return (
        <div className={clsx('w-full space-y-2', className)}>
            {children}
        </div>
    )
}

FormControl.displayName = 'FormControl'

/**
 * FormHelperText component for error and help text
 * Used by other form components internally, but can be used standalone if needed
 */
export const FormHelperText = ({
                                   error,
                                   className,
                                   children,
                               }: FormHelperTextProps) => {
    return (
        <p
            className={clsx(
                'text-sm font-medium',
                error
                    ? 'text-red-600 dark:text-red-400'
                    : 'text-gray-500 dark:text-gray-400',
                className
            )}
        >
            {children}
        </p>
    )
}

FormHelperText.displayName = 'FormHelperText'
