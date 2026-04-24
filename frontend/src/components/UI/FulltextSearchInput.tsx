import { type ChangeEvent, type ReactElement, useCallback, useEffect, useRef, useState } from 'react';
import { useSearchParams } from 'react-router-dom';

export interface FulltextSearchInputProps {
    paramName?: string;
    placeholder: string;
    ariaLabel: string;
    className?: string;
    minChars?: number;
    debounceMs?: number;
}

export function FulltextSearchInput({
    paramName = 'q',
    placeholder,
    ariaLabel,
    className,
    minChars = 2,
    debounceMs = 250,
}: FulltextSearchInputProps): ReactElement {
    const [searchParams, setSearchParams] = useSearchParams();
    const urlValue = searchParams.get(paramName) ?? '';

    const [inputValue, setInputValue] = useState(urlValue);
    const debounceRef = useRef<ReturnType<typeof setTimeout> | null>(null);

    // Cleanup debounce timer on unmount
    useEffect(
        () => () => {
            if (debounceRef.current) clearTimeout(debounceRef.current);
        },
        [],
    );

    // Sync input when URL param changes externally (back/forward navigation)
    const prevUrlValue = useRef(urlValue);
    useEffect(() => {
        if (prevUrlValue.current !== urlValue && inputValue !== urlValue) {
            setInputValue(urlValue);
        }
        prevUrlValue.current = urlValue;
    }, [urlValue, inputValue]);

    const handleChange = useCallback(
        (e: ChangeEvent<HTMLInputElement>) => {
            const value = e.target.value;
            setInputValue(value);

            if (debounceRef.current) clearTimeout(debounceRef.current);
            debounceRef.current = setTimeout(() => {
                const trimmed = value.trim();
                const effectiveValue = trimmed.length >= minChars ? trimmed : '';
                setSearchParams((prev) => {
                    const currentValue = prev.get(paramName) ?? '';
                    if (effectiveValue === currentValue) return prev;
                    const next = new URLSearchParams(prev);
                    if (effectiveValue) {
                        next.set(paramName, effectiveValue);
                    } else {
                        next.delete(paramName);
                    }
                    return next;
                });
            }, debounceMs);
        },
        [setSearchParams, paramName, minChars, debounceMs],
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
