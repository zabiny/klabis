import { type ReactElement } from 'react';
import { useAuth } from '../../contexts/AuthContext2';
import { labels } from '../../localization';
import { FulltextSearchInput, PillGroup } from '../UI';
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

            <PillGroup<TimeWindow>
                options={TIME_WINDOW_OPTIONS}
                selectedValue={timeWindow}
                onChange={onTimeWindowChange}
                ariaLabel={labels.eventsFilter.timeWindowLabel}
            />

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
