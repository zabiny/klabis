import {type ReactElement, useEffect} from 'react'
import {useField} from 'formik'
import {Check} from 'lucide-react'
import {useHalCollection} from '../../../../hooks/useHalCollection.ts'
import {describeError, labels} from '../../../../localization'
import {Alert} from '../../../UI'
import type {HalFormsInputProps} from '../types.ts'
import {fieldLabel} from './fieldLabel.ts'

interface MembershipFeeGroupOption {
    id: string;
    name: string;
    yearlyFeeAmount?: number;
}

interface MembershipFeeGroupRadioCardsProps {
    groups: MembershipFeeGroupOption[];
    value: string;
    onChange: (groupId: string) => void;
    ariaLabel?: string;
}

const useMembershipFeeGroupOptions = (optionsHref?: string) => {
    const {items, isLoading, error} = useHalCollection<MembershipFeeGroupOption>(optionsHref)
    return {groups: items, isLoading, error, unavailable: !optionsHref}
}

const RadioCard = ({option, selected, onSelect}: {
    option: MembershipFeeGroupOption;
    selected: boolean;
    onSelect: (id: string) => void;
}): ReactElement => (
    <button
        type="button"
        role="radio"
        aria-checked={selected}
        onClick={() => onSelect(option.id)}
        className="w-full flex items-center gap-3 px-4 h-14 rounded-lg border transition-colors cursor-pointer text-left"
        style={{
            background: selected ? 'var(--color-primary-subtle)' : 'var(--color-bg-elevated)',
            borderWidth: selected ? '1.5px' : '1px',
            borderColor: selected ? 'var(--color-primary)' : 'var(--color-border-subtle)',
        }}
    >
        <div
            className="flex-shrink-0 flex items-center justify-center w-[18px] h-[18px] rounded-full"
            style={{
                background: selected ? 'var(--color-primary)' : 'var(--color-bg-elevated)',
                border: selected ? 'none' : '1.5px solid var(--color-border-default)',
            }}
        >
            {selected && <Check className="w-3 h-3 text-white" strokeWidth={3}/>}
        </div>
        <span className="flex-1 font-bold text-sm text-text-primary">{option.name}</span>
        {option.yearlyFeeAmount !== undefined && (
            <span className="text-sm text-text-secondary">
                {option.yearlyFeeAmount} {labels.finance.currency} / rok
            </span>
        )}
    </button>
)

const MembershipFeeGroupRadioCards = ({
    groups,
    value,
    onChange,
    ariaLabel,
}: MembershipFeeGroupRadioCardsProps): ReactElement => (
    <div role="radiogroup" aria-label={ariaLabel} className="flex flex-col gap-3">
        {groups.map(group => (
            <RadioCard key={group.id} option={group} selected={value === group.id} onSelect={onChange}/>
        ))}
    </div>
)

export const HalFormsMembershipFeeGroupChoice = ({prop, errorText}: HalFormsInputProps): ReactElement => {
    const [field, , helpers] = useField<string | undefined>(prop.name)
    const label = fieldLabel(prop)
    const {groups, isLoading, error, unavailable} = useMembershipFeeGroupOptions(prop.options?.link?.href)
    const value = field.value ?? ''

    // A prefilled value (e.g. the recommended type) may not be offered to this member; submitting it would be rejected.
    useEffect(() => {
        if (!isLoading && value && !groups.some(g => g.id === value)) {
            void helpers.setValue('')
        }
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [isLoading, groups, value])

    return (
        <div className="flex flex-col gap-2">
            <p className="text-sm font-medium text-text-secondary">{label}</p>
            {isLoading && <p className="text-sm text-text-tertiary">{labels.ui.loading}</p>}
            {(unavailable || error) && (
                <Alert severity="error">{error ? describeError(error) : labels.memberFee.optionsUnavailable}</Alert>
            )}
            <MembershipFeeGroupRadioCards
                groups={groups}
                value={value}
                onChange={id => helpers.setValue(id)}
                ariaLabel={label}
            />
            {errorText && <p className="text-sm text-error font-medium">{errorText}</p>}
        </div>
    )
}

HalFormsMembershipFeeGroupChoice.displayName = 'HalFormsMembershipFeeGroupChoice'
