import type {ReactElement} from 'react'
import {useEffect, useState} from 'react'
import type {FieldProps} from 'formik'
import {Field} from 'formik'
import type {SelectOption} from '../../FormFields'
import {SelectField} from '../../FormFields'
import {klabisAuthUserManager} from '../../../api/klabisUserManager'
import type {HalFormsInputProps} from '../types'
import type {HalFormsOption, HalFormsOptionType} from '../../../api'

/**
 * Helper hook to fetch options from link or use inline options
 */
const useOptionItems = (def: HalFormsOption | undefined): { isLoading: boolean; options: SelectOption[] } => {
    const [isLoading, setIsLoading] = useState<boolean>(true)
    const [options, setOptions] = useState<SelectOption[]>([])

    const fetchData = async (url: string): Promise<void> => {
        setIsLoading(true)

        try {
            const user = await klabisAuthUserManager.getUser()
            const res = await fetch(url, {
                headers: {
                    Accept: 'application/json',
                    Authorization: `Bearer ${user?.access_token}`,
                },
            })
            if (!res.ok) throw new Error(`HTTP ${res.status}`)

            const data = await res.json()
            setOptions(convertToSelectOptions(data))
        } finally {
            setIsLoading(false)
        }
    }

    useEffect(() => {
        if (def?.link && def.link.href) {
            fetchData(def.link.href)
        }
    }, [def])

    if (def?.inline) {
        return {isLoading: false, options: convertToSelectOptions(def.inline)}
    } else if (def?.link) {
        return {isLoading: isLoading, options: options}
    } else {
        return {isLoading: false, options: []}
    }
}

/**
 * Convert HAL+Forms options to SelectOption format
 */
const convertToSelectOptions = (halOptions: HalFormsOptionType[]): SelectOption[] => {
    if (!halOptions) return []

    return halOptions.map((item) => {
        const value = getValue(item)
        const label = getLabel(item)
        return {value, label}
    })
}

function isOptionItem(item: any): item is { value: any; prompt?: string } {
    return item !== undefined && item !== null && item.value !== undefined
}

function isNumber(item: any): item is number {
    return typeof item === 'number'
}

function optionValueToString(value: any): string {
    if (isNumber(value)) {
        return `${value}`
    } else {
        return value
    }
}

function getValue(item: HalFormsOptionType): string {
    if (isOptionItem(item)) {
        return getValue(item.value)
    } else {
        return optionValueToString(item)
    }
}

function getLabel(item: HalFormsOptionType): string {
    if (isOptionItem(item)) {
        return item.prompt || getLabel(item.value)
    } else if (isNumber(item)) {
        return `${item}`
    } else {
        return item
    }
}

/**
 * HalFormsSelect component - dropdown selection for HAL+Forms
 * Uses Formik Field and FormFields SelectField abstraction
 */
export const HalFormsSelect = ({prop, errorText}: HalFormsInputProps): ReactElement => {
    const {options, isLoading} = useOptionItems(prop.options)

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
