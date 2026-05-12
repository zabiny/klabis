import { type ReactElement } from 'react';
import { labels } from '../../localization';
import { FulltextSearchInput, PillGroup } from '../UI';
import type { TimeWindow } from './eventsFilterUtils';
import type { EventTypeCatalogItem } from '../../hooks/useEventTypes';

export type EventsFilterValue = {
    q: string;
    timeWindow: TimeWindow;
    registeredByMe: boolean;
    eventTypeIds: string[];
};

export interface EventsFilterBarProps {
    value: EventsFilterValue;
    onChange: (next: EventsFilterValue) => void;
    showRegisteredByMeToggle: boolean;
    eventTypes?: EventTypeCatalogItem[];
}

const TIME_WINDOW_OPTIONS: { value: TimeWindow; label: string }[] = [
    { value: 'budouci', label: labels.eventsFilter.budouci },
    { value: 'probehle', label: labels.eventsFilter.probehle },
    { value: 'vse', label: labels.eventsFilter.vse },
];

export function EventsFilterBar({
    value,
    onChange,
    showRegisteredByMeToggle,
    eventTypes,
}: EventsFilterBarProps): ReactElement {
    const handleEventTypeChange = (e: React.ChangeEvent<HTMLSelectElement>) => {
        const selected = Array.from(e.target.selectedOptions)
            .map((o) => o.value)
            .filter((v) => v !== '');
        onChange({ ...value, eventTypeIds: selected });
    };

    const selectId = 'events-filter-event-type';

    return (
        <div className="flex flex-wrap items-center gap-3 p-3 bg-surface-raised rounded-md border border-border">
            <FulltextSearchInput
                value={value.q}
                onChange={(q) => onChange({ ...value, q })}
                placeholder={labels.eventsFilter.searchPlaceholder}
                ariaLabel={labels.eventsFilter.search}
            />

            <PillGroup<TimeWindow>
                options={TIME_WINDOW_OPTIONS}
                selectedValue={value.timeWindow}
                onChange={(timeWindow) => onChange({ ...value, timeWindow })}
                ariaLabel={labels.eventsFilter.timeWindowLabel}
            />

            {eventTypes && eventTypes.length > 0 && (
                <div className="flex items-center gap-1.5">
                    <label htmlFor={selectId} className="text-sm text-text-primary">
                        {labels.eventsFilter.eventTypeFilter}
                    </label>
                    <select
                        id={selectId}
                        multiple
                        value={value.eventTypeIds}
                        onChange={handleEventTypeChange}
                        className="text-sm border border-border rounded bg-surface text-text-primary px-2 py-1 min-w-[8rem] max-h-24"
                        aria-label={labels.eventsFilter.eventTypeFilter}
                    >
                        <option value="">{labels.eventsFilter.eventTypeSelectPlaceholder}</option>
                        {eventTypes.map((et) => (
                            <option key={et.id} value={et.id}>
                                {et.name}
                            </option>
                        ))}
                    </select>
                </div>
            )}

            {showRegisteredByMeToggle && (
                <label className="inline-flex items-center gap-2 text-sm text-text-primary cursor-pointer select-none">
                    <input
                        type="checkbox"
                        checked={value.registeredByMe}
                        onChange={(e) => onChange({ ...value, registeredByMe: e.target.checked })}
                        aria-label={labels.eventsFilter.mojePřihlaskyLabel}
                        className="w-4 h-4 accent-primary"
                    />
                    {labels.eventsFilter.mojePřihlaskyLabel}
                </label>
            )}
        </div>
    );
}
