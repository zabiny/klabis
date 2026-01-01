import type React from 'react'
import {TextField} from '../UI/forms/TextField.tsx'

type Contact = {
    email: string
    phone: string
    note: string
}

type ContactFieldsProps = {
    value: Contact
    onChange: (val: Contact) => void
    showNote?: boolean
}

export const ContactFields: React.FC<ContactFieldsProps> = ({
                                                                value,
                                                                onChange,
                                                                showNote = true
                                                            }) => {
    const handleFieldChange = (field: keyof Contact) => (
        e: React.ChangeEvent<HTMLInputElement>
    ) => {
        onChange({...value, [field]: e.target.value})
    }

    return (
        <div className="grid grid-cols-1 md:grid-cols-12 gap-4">
            <div className="col-span-1 md:col-span-5">
                <TextField
                    label="E-mail"
                    value={value.email}
                    onChange={handleFieldChange('email')}
                    type="email"
                />
            </div>
            <div className="col-span-1 md:col-span-4">
                <TextField
                    label="Telefon"
                    value={value.phone}
                    onChange={handleFieldChange('phone')}
                />
            </div>
            {showNote && (
                <div className="col-span-1 md:col-span-3">
                    <TextField
                        label="Poznámka"
                        value={value.note}
                        onChange={handleFieldChange('note')}
                    />
                </div>
            )}
        </div>
    )
}

export default ContactFields
