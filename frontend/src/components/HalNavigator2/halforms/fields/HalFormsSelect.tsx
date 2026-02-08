import type {ReactElement} from 'react'
import type {FieldProps} from 'formik'
import {Field} from 'formik'
import {SelectField} from '../../../UI/forms'
import {useHalFormOptions} from '../../../../hooks/useHalFormOptions.ts'
import type {HalFormsInputProps} from '../types.ts'

/**
 * HalFormsSelect component - dropdown selection for HAL+Forms
 *
 * Uses Formik Field and FormFields SelectField abstraction.
 * Options are fetched via useHalFormOptions which handles both inline
 * and link-based options with automatic React Query caching.
 */
export const HalFormsSelect = ({prop, errorText}: HalFormsInputProps): ReactElement => {
    const {options, isLoading} = useHalFormOptions(prop.options)

    return (
        <Field
            name={prop.name}
            validate={() => undefined}
            render={(fieldProps: FieldProps<unknown>) => {
                const fieldValue = fieldProps.field.value as string | number | undefined;
                return (
                    <SelectField
                        {...fieldProps.field}
                        value={fieldValue}
                        label={prop.prompt || prop.name}
                        placeholder={isLoading ? 'Načítání...' : 'Vyberte možnost'}
                        disabled={prop.readOnly || isLoading || false}
                        required={prop.required}
                        error={errorText}
                        options={options}
                        className="w-full"
                    />
                );
            }}
        />
    )
}

HalFormsSelect.displayName = 'HalFormsSelect'
