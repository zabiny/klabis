import {type KeyboardEvent, type ReactElement, useEffect, useMemo, useState} from 'react'
import {X} from 'lucide-react'
import {useField} from 'formik'
import {FieldWrapper} from '../../../UI/forms'
import type {SelectOption} from '../../../UI/forms'
import {useHalFormOptions} from '../../../../hooks/useHalFormOptions.ts'
import type {HalFormsInputProps} from '../types.ts'
import {getFieldLabel} from '../../../../localization'
import {ReadOnlyDisplay} from '../HalFormsForm.tsx'
import {normalizeArrayValue} from './normalizeArrayValue.ts'
import {needsNormalization, toSubmitValue} from './multiValueSubmit.ts'

/**
 * Generic multi-value HAL-FORMS widget (design.md D7): a searchable dropdown with the
 * current selection shown as removable chips. Used for any `multi` property backed by an
 * option list (inline or remote link) that has no dedicated per-row custom widget — e.g.
 * event type `disciplineIds`. Replaces a plain checkbox group for option lists that can be
 * long/unbounded; `HalFormsCheckboxGroup` stays the right choice for short, always-visible
 * enumerations (e.g. `MembershipFeeTierMultiSelect`, which renders it directly regardless of
 * this factory's dispatch).
 */
export const HalFormsMultiSelect = ({prop, errorText, renderMode = 'field'}: HalFormsInputProps): ReactElement => {
    const {options, isLoading} = useHalFormOptions(prop.options, prop)
    const [field, , helpers] = useField<unknown>(prop.name)
    const [query, setQuery] = useState('')
    const [isOpen, setIsOpen] = useState(false)
    const [activeIndex, setActiveIndex] = useState(-1)

    const rawValue = field.value
    const valueArray = Array.isArray(rawValue) ? rawValue : []

    useEffect(() => {
        // Normalize stale HAL objects (e.g. {memberId, _links}) or numbers in a non-number
        // field, same as HalFormsCheckboxGroup — intentionally only on mount.
        if (needsNormalization(valueArray, prop.type)) {
            helpers.setValue(valueArray.map(normalizeArrayValue));
        }
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [prop.name]);

    const normalizedValue = valueArray.map(normalizeArrayValue)

    const optionByValue = useMemo(() => {
        const map = new Map<string, SelectOption>();
        options.forEach(o => map.set(String(o.value), o));
        return map;
    }, [options]);

    const selectedOptions = normalizedValue
        .map(v => optionByValue.get(v))
        .filter((o): o is SelectOption => o !== undefined);

    const availableOptions = options.filter(o => !normalizedValue.includes(String(o.value)));
    const filteredOptions = query
        ? availableOptions.filter(o => o.label.toLowerCase().includes(query.toLowerCase()))
        : availableOptions;

    const commit = (values: string[]) => {
        helpers.setValue(values.map((v) => toSubmitValue(v, prop.type)));
    };

    const handleSelect = (optionValue: string | number) => {
        commit([...normalizedValue, String(optionValue)]);
        setQuery('');
        setIsOpen(false);
    };

    const handleRemove = (optionValue: string) => {
        commit(normalizedValue.filter(v => v !== optionValue));
    };

    const listboxId = `multiselect-listbox-${prop.name}`;
    const optionId = (index: number) => `${listboxId}-option-${index}`;

    const handleKeyDown = (e: KeyboardEvent<HTMLInputElement>) => {
        switch (e.key) {
            case 'ArrowDown':
                e.preventDefault();
                setIsOpen(true);
                setActiveIndex(prev => filteredOptions.length === 0 ? -1 : (prev + 1) % filteredOptions.length);
                break;
            case 'ArrowUp':
                e.preventDefault();
                setIsOpen(true);
                setActiveIndex(prev => filteredOptions.length === 0 ? -1 : (prev - 1 + filteredOptions.length) % filteredOptions.length);
                break;
            case 'Enter':
                if (isOpen && activeIndex >= 0 && activeIndex < filteredOptions.length) {
                    e.preventDefault();
                    handleSelect(filteredOptions[activeIndex].value);
                }
                break;
            case 'Escape':
                setIsOpen(false);
                break;
            case 'Backspace':
                if (query === '' && selectedOptions.length > 0) {
                    handleRemove(String(selectedOptions[selectedOptions.length - 1].value));
                }
                break;
        }
    };

    if (prop.readOnly) {
        const display = selectedOptions.length > 0
            ? selectedOptions.map(o => o.label).join(', ')
            : (isLoading ? 'Načítání...' : '—');
        return <ReadOnlyDisplay label={prop.prompt || getFieldLabel(prop.name)} value={display} renderMode={renderMode} />;
    }

    const label = renderMode === 'field' ? (prop.prompt || getFieldLabel(prop.name)) : undefined;

    return (
        <FieldWrapper label={label} error={errorText} required={prop.required}>
            <div className="flex flex-col gap-2">
                {selectedOptions.length > 0 && (
                    <div className="flex flex-wrap gap-1.5" data-testid={`multiselect-chips-${prop.name}`}>
                        {selectedOptions.map((option) => (
                            <span
                                key={String(option.value)}
                                className="inline-flex items-center gap-1 rounded-full bg-primary/10 text-primary px-2.5 py-1 text-sm"
                            >
                                {option.label}
                                <button
                                    type="button"
                                    onClick={() => handleRemove(String(option.value))}
                                    aria-label={`Odebrat ${option.label}`}
                                    className="hover:text-error"
                                >
                                    <X className="w-3.5 h-3.5" />
                                </button>
                            </span>
                        ))}
                    </div>
                )}

                <div className="relative">
                    <input
                        type="text"
                        role="combobox"
                        aria-expanded={isOpen}
                        aria-controls={listboxId}
                        aria-activedescendant={isOpen && activeIndex >= 0 ? optionId(activeIndex) : undefined}
                        aria-label={renderMode === 'input' ? (prop.prompt || getFieldLabel(prop.name)) : undefined}
                        value={query}
                        disabled={isLoading}
                        placeholder={isLoading ? 'Načítání...' : 'Vyhledat a přidat...'}
                        onChange={(e) => {
                            setQuery(e.target.value);
                            setIsOpen(true);
                            setActiveIndex(0);
                        }}
                        onFocus={() => {
                            setIsOpen(true);
                            setActiveIndex(0);
                        }}
                        onKeyDown={handleKeyDown}
                        onBlur={() => setIsOpen(false)}
                        className="w-full px-3 py-1.5 border rounded-md font-normal text-sm text-text-primary bg-surface-raised border-border placeholder-text-tertiary focus:outline-none focus:ring-2 focus:ring-primary focus:ring-offset-0 disabled:opacity-50"
                    />
                    {isOpen && filteredOptions.length > 0 && (
                        <ul
                            id={listboxId}
                            role="listbox"
                            className="absolute z-10 mt-1 w-full max-h-56 overflow-auto rounded-md border border-border bg-surface-raised shadow-lg"
                        >
                            {filteredOptions.map((option, index) => (
                                <li
                                    key={String(option.value)}
                                    id={optionId(index)}
                                    role="option"
                                    aria-selected={index === activeIndex}
                                    // onMouseDown (not onClick) fires before the input's onBlur,
                                    // so the option is still in the DOM when the click resolves.
                                    onMouseDown={(e) => {
                                        e.preventDefault();
                                        handleSelect(option.value);
                                    }}
                                    className={`px-3 py-1.5 text-sm text-text-primary cursor-pointer hover:bg-primary/10 ${index === activeIndex ? 'bg-primary/10' : ''}`}
                                >
                                    {option.label}
                                </li>
                            ))}
                        </ul>
                    )}
                </div>
            </div>
        </FieldWrapper>
    )
}

HalFormsMultiSelect.displayName = 'HalFormsMultiSelect'
