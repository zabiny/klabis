import { type ReactElement } from 'react';

export interface PillGroupOption<T> {
    value: T;
    label: string;
}

export interface PillGroupProps<T> {
    options: PillGroupOption<T>[];
    selectedValue: T;
    onChange: (value: T) => void;
    ariaLabel: string;
    className?: string;
    disabledValues?: T[];
    disabledTooltip?: string;
}

export function PillGroup<T>({
    options,
    selectedValue,
    onChange,
    ariaLabel,
    className,
    disabledValues,
    disabledTooltip,
}: PillGroupProps<T>): ReactElement {
    return (
        <div
            className={`inline-flex rounded-md border border-border overflow-hidden${className ? ` ${className}` : ''}`}
            role="group"
            aria-label={ariaLabel}
        >
            {options.map(({ value, label }) => {
                const isDisabled = disabledValues?.includes(value) ?? false;
                return (
                    <button
                        key={String(value)}
                        type="button"
                        aria-pressed={selectedValue === value}
                        aria-disabled={isDisabled || undefined}
                        title={isDisabled ? disabledTooltip : undefined}
                        onClick={isDisabled ? undefined : () => onChange(value)}
                        className={`px-3 py-1.5 text-sm font-medium transition-colors focus:outline-none focus:ring-2 focus:ring-accent focus:ring-inset ${
                            isDisabled
                                ? 'bg-surface text-text-disabled cursor-not-allowed opacity-50'
                                : selectedValue === value
                                  ? 'bg-primary text-white'
                                  : 'bg-surface text-text-primary hover:bg-surface-hover'
                        }`}
                    >
                        {label}
                    </button>
                );
            })}
        </div>
    );
}
