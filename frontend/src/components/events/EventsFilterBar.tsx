import {type ReactElement} from 'react';
import {labels} from '../../localization';
import {FulltextSearchInput, PillGroup} from '../UI';
import type {TimeWindow} from './eventsFilterUtils';
import type {EventTypeCatalogItem} from '../../hooks/useEventTypes';

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

const ALL_EVENT_TYPES = '';

export function EventsFilterBar({
    value,
    onChange,
    showRegisteredByMeToggle,
    eventTypes,
}: EventsFilterBarProps): ReactElement {
    const eventTypeOptions = eventTypes
        ? [
              { value: ALL_EVENT_TYPES, label: labels.eventsFilter.eventTypeAll },
              ...eventTypes.map((et) => ({ value: et.id, label: et.name })),
          ]
        : [];

    const selectedEventType = value.eventTypeIds[0] ?? ALL_EVENT_TYPES;

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
                <PillGroup<string>
                    options={eventTypeOptions}
                    selectedValue={selectedEventType}
                    onChange={(id) =>
                        onChange({
                            ...value,
                            eventTypeIds: id === ALL_EVENT_TYPES ? [] : [id],
                        })
                    }
                    ariaLabel={labels.eventsFilter.eventTypeFilter}
                />
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
