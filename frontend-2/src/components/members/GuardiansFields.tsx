import type React from 'react'
import {useCallback} from 'react'
import {Button} from '../UI'
import {TextField} from '../FormFields/TextField'
import ContactFields from './ContactFields'
import type {MemberRegistrationForm} from '../../api'

type Guardian = NonNullable<MemberRegistrationForm['guardians']>[number]

export interface GuardiansFieldsProps {
    value: Guardian[]
    onChange: (guardians: Guardian[]) => void
    disabled?: boolean
}

interface GuardianFieldProps {
    value: Guardian
    onChange: (g: Guardian) => void
    onRemove: () => void
    disabled?: boolean
}

const GuardianField: React.FC<GuardianFieldProps> = ({value, onChange, onRemove, disabled}) => {
    // Přímé změny hodnot
    const handleChange = (path: string, v: unknown) => {
        const keys = path.split('.')
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        let obj: any = {...value}
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        let ref: any = obj
        for (let k = 0; k < keys.length - 1; k++) {
            if (!(keys[k] in ref)) ref[keys[k]] = {}
            ref = ref[keys[k]]
        }
        ref[keys[keys.length - 1]] = v
        onChange(obj)
    }

    return (
        <div
            className="relative border border-gray-300 dark:border-gray-600 rounded-lg p-4 bg-gray-50 dark:bg-gray-800">
            <div className="grid grid-cols-1 md:grid-cols-12 gap-4">
                <div className="col-span-1 md:col-span-6">
                    <TextField
                        label="Jméno"
                        value={value.firstName ?? ''}
                        onChange={(e) => handleChange('firstName', e.target.value)}
                        required
                    />
                </div>
                <div className="col-span-1 md:col-span-6">
                    <TextField
                        label="Příjmení"
                        value={value.lastName ?? ''}
                        onChange={(e) => handleChange('lastName', e.target.value)}
                        required
                    />
                </div>
                <div className="col-span-1 md:col-span-12">
                    <ContactFields
                        value={{
                            email: value.contact?.email || '',
                            phone: value.contact?.phone || '',
                            note: value.contact?.note || ''
                        }}
                        onChange={(c) => handleChange('contact', c)}
                        showNote={false}
                    />
                </div>
                <div className="col-span-1 md:col-span-12">
                    <TextField
                        label="Poznámka k zástupci"
                        value={value.note || ''}
                        onChange={(e) => handleChange('note', e.target.value)}
                    />
                </div>
            </div>
            <Button
                variant="secondary"
                size="sm"
                disabled={disabled}
                onClick={onRemove}
                className="absolute top-4 right-4"
            >
                Odebrat
            </Button>
        </div>
    )
}

const GuardiansFields: React.FC<GuardiansFieldsProps> = ({value, onChange, disabled}) => {
    const handleAddGuardian = () => {
        onChange([
            ...value,
            {firstName: '', lastName: '', contact: {email: '', phone: '', note: ''}, note: ''}
        ])
    }

    const handleRemoveGuardian = useCallback((idx: number) => {
        onChange(value.filter((_, i) => i !== idx))
    }, [value, onChange])

    const handleGuardianChange = useCallback(
        (idx: number, updatedG: Guardian) => {
            onChange(value.map((current, i) => (i === idx ? updatedG : current)))
        },
        [value, onChange]
    )

    return (
        <div>
            <h2 className="text-lg font-semibold mb-4 text-gray-900 dark:text-white">Zákonní zástupci</h2>
            <div className="space-y-4">
                {(value || []).map((g, idx) => (
                    <GuardianField
                        key={idx}
                        value={g}
                        onChange={(updatedG) => handleGuardianChange(idx, updatedG)}
                        onRemove={() => handleRemoveGuardian(idx)}
                        disabled={disabled}
                    />
                ))}
                <Button variant="secondary" onClick={handleAddGuardian} disabled={disabled}>
                    Přidat zástupce
                </Button>
            </div>
        </div>
    )
}

export default GuardiansFields
