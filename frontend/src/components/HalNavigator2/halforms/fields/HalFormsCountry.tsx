import {type ReactElement, useState} from 'react'
import type {FieldProps} from 'formik'
import {Field} from 'formik'
import {SelectField, TextField} from '../../../UI/forms'
import {countryName} from '../../../../utils/countryName.ts'
import type {HalFormsInputProps} from '../types.ts'
import {fieldLabel} from './fieldLabel.ts'

const PRESET_CODES = ['CZ', 'SK', 'AT', 'PL', 'DE', 'SE', 'NO', 'DK']
const OTHER = '__other__'

const PRESET_OPTIONS = [
    ...PRESET_CODES.map(code => ({value: code, label: countryName(code) ?? code})),
    {value: OTHER, label: 'Jiný stát…'},
]

interface CountryPickerProps {
    value: string
    onChange: (code: string) => void
    onBlur?: () => void
    name: string
    label?: string
    ariaLabel?: string
    codeLabel?: string
    disabled?: boolean
    required?: boolean
    error?: string
}

export const CountryPicker = ({
                                  value, onChange, onBlur, name, label, ariaLabel, codeLabel, disabled, required, error
                              }: CountryPickerProps): ReactElement => {
    const [otherChosen, setOtherChosen] = useState(false)
    const isOther = otherChosen || (value !== '' && !PRESET_CODES.includes(value))
    const resolvedName = countryName(value)

    return (
        <div className="flex flex-col gap-2">
            <SelectField
                name={name}
                value={isOther ? OTHER : value}
                label={label}
                aria-label={ariaLabel}
                options={PRESET_OPTIONS}
                placeholder="Vyberte stát"
                disabled={disabled}
                required={required}
                error={isOther ? undefined : error}
                onBlur={onBlur}
                onChange={(e) => {
                    const selected = e.target.value
                    if (selected === OTHER) {
                        setOtherChosen(true)
                        onChange('')
                    } else {
                        setOtherChosen(false)
                        onChange(selected)
                    }
                }}
                className="w-full"
            />
            {isOther && (
                <div className="flex items-center gap-3">
                    <TextField
                        name={`${name}-code`}
                        aria-label={codeLabel ? `Kód státu (${codeLabel})` : 'Kód státu'}
                        value={value}
                        maxLength={2}
                        disabled={disabled}
                        error={error}
                        onBlur={onBlur}
                        onChange={(e) => {
                            setOtherChosen(true)
                            onChange(e.target.value.toUpperCase())
                        }}
                        className="w-20"
                    />
                    {value.length === 2 && (
                        <span className={resolvedName ? 'text-sm text-text-secondary' : 'text-sm text-error'}>
                            {resolvedName ?? 'Neznámý kód státu'}
                        </span>
                    )}
                </div>
            )}
        </div>
    )
}

export const HalFormsCountry = ({prop, errorText, renderMode = 'field'}: HalFormsInputProps): ReactElement => (
    <Field name={prop.name} validate={() => undefined}>
        {({field, form}: FieldProps<unknown>) => (
            <CountryPicker
                name={field.name}
                value={typeof field.value === 'string' ? field.value : ''}
                onChange={(code) => form.setFieldValue(field.name, code)}
                onBlur={() => form.setFieldTouched(field.name, true)}
                label={renderMode === 'field' ? fieldLabel(prop) : undefined}
                ariaLabel={renderMode === 'input' ? fieldLabel(prop) : undefined}
                codeLabel={fieldLabel(prop)}
                disabled={prop.readOnly || false}
                required={prop.required}
                error={errorText}
            />
        )}
    </Field>
)

HalFormsCountry.displayName = 'HalFormsCountry'
