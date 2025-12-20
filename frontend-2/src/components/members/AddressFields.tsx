import type React from 'react'
import {TextField} from '../FormFields/TextField'
import {SelectField} from '../FormFields/SelectField'
import type {SelectOption} from '../FormFields'

type Address = {
  streetAndNumber: string
  city: string
  postalCode: string
  country: string
}

type AddressFieldsProps = {
  value: Address
  onChange: (val: Address) => void
  countryOptions?: SelectOption[]
}

const DEFAULT_COUNTRIES: SelectOption[] = [
  {value: '', label: ''},
  {value: 'CZ', label: 'Česká republika'},
  {value: 'SK', label: 'Slovensko'},
  {value: 'DE', label: 'Německo'},
  {value: 'AT', label: 'Rakousko'},
  {value: 'PL', label: 'Polsko'},
  {value: 'AS', label: 'Americká Samoa'},
  {value: 'IO', label: 'Britské indickooceánské území'}
]

export const AddressFields: React.FC<AddressFieldsProps> = ({
                                                              value,
                                                              onChange,
                                                              countryOptions = DEFAULT_COUNTRIES
                                                            }) => {
  const handleFieldChange = (field: keyof Address) => (e: React.ChangeEvent<HTMLInputElement>) => {
    onChange({...value, [field]: e.target.value})
  }

  const handleCountryChange = (e: React.ChangeEvent<HTMLSelectElement>) => {
    onChange({...value, country: e.target.value})
  }

  return (
      <div className="grid grid-cols-1 md:grid-cols-12 gap-4">
        <div className="col-span-1 md:col-span-4">
          <TextField
              label="Ulice a číslo"
              value={value.streetAndNumber}
              onChange={handleFieldChange('streetAndNumber')}
              required
          />
        </div>
        <div className="col-span-1 md:col-span-3">
          <TextField
              label="Město"
              value={value.city}
              onChange={handleFieldChange('city')}
              required
          />
        </div>
        <div className="col-span-1 md:col-span-2">
          <TextField
              label="PSČ"
              value={value.postalCode}
              onChange={handleFieldChange('postalCode')}
          />
        </div>
        <div className="col-span-1 md:col-span-3">
          <SelectField
              label="Stát"
              value={value.country}
              onChange={handleCountryChange}
              options={countryOptions}
              required
          />
        </div>
      </div>
  )
}

export default AddressFields
