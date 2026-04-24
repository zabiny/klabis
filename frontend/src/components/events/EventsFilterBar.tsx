import { type ReactElement } from 'react';
import { useAuth } from '../../contexts/AuthContext2';
import { labels } from '../../localization';
import { FulltextSearchInput } from '../UI/FulltextSearchInput';
import type { TimeWindow } from './eventsFilterUtils';

export interface EventsFilterBarProps {
    timeWindow: TimeWindow;
    onTimeWindowChange: (window: TimeWindow) => void;
    registeredByMe: boolean;
    onRegisteredByMeChange: (checked: boolean) => void;
}

const TIME_WINDOW_OPTIONS: { value: TimeWindow; label: string }[] = [
    { value: 'budouci', label: labels.eventsFilter.budouci },
    { value: 'probehle', label: labels.eventsFilter.probehle },
    { value: 'vse', label: labels.eventsFilter.vse },
];

export function EventsFilterBar({
    timeWindow,
    onTimeWindowChange,
    registeredByMe,
    onRegisteredByMeChange,
}: EventsFilterBarProps): ReactElement {
    const { getUser } = useAuth();
    const user = getUser();
    const hasMemberProfile = Boolean(user?.memberId);

    return (
        <div className="flex flex-wrap items-center gap-3 p-3 bg-surface-raised rounded-md border border-border">
            <FulltextSearchInput
                paramName="q"
                placeholder={labels.eventsFilter.searchPlaceholder}
                ariaLabel={labels.eventsFilter.search}
            />

            <div
                className="inline-flex rounded-md border border-border overflow-hidden"
                role="group"
                aria-label={labels.eventsFilter.timeWindowLabel}
            >
                {TIME_WINDOW_OPTIONS.map(({ value, label }) => (
                    <button
                        key={value}
                        type="button"
                        aria-pressed={timeWindow === value}
                        onClick={() => onTimeWindowChange(value)}
                        className={`px-3 py-1.5 text-sm font-medium transition-colors focus:outline-none focus:ring-2 focus:ring-accent focus:ring-inset ${
                            timeWindow === value
                                ? 'bg-primary text-white'
                                : 'bg-surface text-text-primary hover:bg-surface-hover'
                        }`}
                    >
                        {label}
                    </button>
                ))}
            </div>

            {hasMemberProfile && (
                <label className="inline-flex items-center gap-2 text-sm text-text-primary cursor-pointer select-none">
                    <input
                        type="checkbox"
                        checked={registeredByMe}
                        onChange={(e) => onRegisteredByMeChange(e.target.checked)}
                        aria-label={labels.eventsFilter.mojePřihlaskyLabel}
                        className="w-4 h-4 accent-primary"
                    />
                    {labels.eventsFilter.mojePřihlaskyLabel}
                </label>
            )}
        </div>
    );
}
