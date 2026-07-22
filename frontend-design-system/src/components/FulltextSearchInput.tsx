import { type ChangeEvent, type ReactElement, useCallback, useEffect, useRef, useState } from 'react';

export interface FulltextSearchInputProps {
    value: string;
    onChange: (value: string) => void;
    placeholder: string;
    ariaLabel: string;
    className?: string;
    minChars?: number;
    debounceMs?: number;
}

export function FulltextSearchInput({
    value,
    onChange,
    placeholder,
    ariaLabel,
    className,
    minChars = 2,
    debounceMs = 250,
}: FulltextSearchInputProps): ReactElement {
    const [inputValue, setInputValue] = useState(value);
    const debounceRef = useRef<ReturnType<typeof setTimeout> | null>(null);
    const lastEmittedRef = useRef<string | null>(null);

    useEffect(
        () => () => {
            if (debounceRef.current) clearTimeout(debounceRef.current);
        },
        [],
    );

    useEffect(() => {
        setInputValue(value);
    }, [value]);

    const handleChange = useCallback(
        (e: ChangeEvent<HTMLInputElement>) => {
            const raw = e.target.value;
            setInputValue(raw);

            if (debounceRef.current) clearTimeout(debounceRef.current);
            debounceRef.current = setTimeout(() => {
                const trimmed = raw.trim();
                const effectiveValue = trimmed.length >= minChars ? trimmed : '';
                if (effectiveValue === lastEmittedRef.current) return;
                lastEmittedRef.current = effectiveValue;
                onChange(effectiveValue);
            }, debounceMs);
        },
        [onChange, minChars, debounceMs],
    );

    return (
        <input
            type="text"
            value={inputValue}
            onChange={handleChange}
            placeholder={placeholder}
            aria-label={ariaLabel}
            className={
                className ??
                'flex-1 min-w-40 px-3 py-1.5 text-sm rounded-md border border-border bg-surface text-text-primary placeholder:text-text-tertiary focus:outline-none focus:ring-2 focus:ring-accent'
            }
        />
    );
}
