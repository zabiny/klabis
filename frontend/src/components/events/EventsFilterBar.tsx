import {type ReactElement, type ReactNode} from 'react';
import {labels} from '../../localization';
import {FulltextSearchInput, PillGroup} from '../UI';
import {getYearRange, isCurrentYear, type TimeWindow} from './eventsFilterUtils';
import type {EventTypeCatalogItem} from '../../hooks/useEventTypes';

export type EventsFilterValue = {
    q: string;
    timeWindow: TimeWindow;
    registeredByMe: boolean;
    eventTypeIds: string[];
    selectedYear: number | null;
    coordinator: string;
};

export type CoordinatorOption = {value: string; prompt: string};

export interface EventsFilterBarProps {
    value: EventsFilterValue;
    onChange: (next: EventsFilterValue) => void;
    showRegisteredByMeToggle: boolean;
    eventTypes?: EventTypeCatalogItem[];
    coordinatorOptions?: CoordinatorOption[];
}

const TIME_WINDOW_OPTIONS: { value: TimeWindow; label: string }[] = [
    { value: 'budouci', label: labels.eventsFilter.budouci },
    { value: 'probehle', label: labels.eventsFilter.probehle },
    { value: 'vse', label: labels.eventsFilter.vse },
];

const ALL_EVENT_TYPES = '';
const YEAR_OPTIONS = getYearRange();

interface FilterSelectProps {
    id: string;
    label: string;
    value: string | number;
    onChange: (value: string) => void;
    children: ReactNode;
}

function FilterSelect({id, label, value, onChange, children}: FilterSelectProps): ReactElement {
    return (
        <div className="flex items-center gap-1.5">
            <label htmlFor={id} className="text-sm text-text-primary whitespace-nowrap">
                {label}
            </label>
            <select
                id={id}
                aria-label={label}
                value={value}
                onChange={(e) => onChange(e.target.value)}
                className="form-select text-sm border border-border rounded pl-2 py-1 bg-surface text-text-primary focus:outline-none focus:ring-1 focus:ring-primary"
            >
                {children}
            </select>
        </div>
    );
}

export function EventsFilterBar({
    value,
    onChange,
    showRegisteredByMeToggle,
    eventTypes,
    coordinatorOptions,
}: EventsFilterBarProps): ReactElement {
    const eventTypeOptions = eventTypes
        ? [
              { value: ALL_EVENT_TYPES, label: labels.eventsFilter.eventTypeAll },
              ...eventTypes.map((et) => ({ value: et.id, label: et.name })),
          ]
        : [];

    const selectedEventType = value.eventTypeIds[0] ?? ALL_EVENT_TYPES;

    const timeWindowDisabled: TimeWindow[] =
        value.selectedYear !== null && !isCurrentYear(value.selectedYear)
            ? ['budouci', 'probehle']
            : [];

    const handleYearChange = (yearStr: string) => {
        if (yearStr === '') {
            onChange({ ...value, selectedYear: null });
        } else {
            const newYear = parseInt(yearStr, 10);
            const needsCoercion = !isCurrentYear(newYear) && value.timeWindow !== 'vse';
            onChange({
                ...value,
                selectedYear: newYear,
                timeWindow: needsCoercion ? 'vse' : value.timeWindow,
            });
        }
    };

    const handleTimeWindowChange = (timeWindow: TimeWindow) => {
        onChange({ ...value, timeWindow });
    };

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
                onChange={handleTimeWindowChange}
                ariaLabel={labels.eventsFilter.timeWindowLabel}
                disabledValues={timeWindowDisabled}
                disabledTooltip={labels.eventsFilter.timeWindowDisabledTooltip}
            />

            <FilterSelect
                id="events-year-filter"
                label={labels.eventsFilter.eventsFilterYear}
                value={value.selectedYear ?? ''}
                onChange={handleYearChange}
            >
                <option value="">{labels.eventsFilter.noYear}</option>
                {YEAR_OPTIONS.map((y) => (
                    <option key={y} value={y}>{y}</option>
                ))}
            </FilterSelect>

            {coordinatorOptions && (
                <FilterSelect
                    id="events-coordinator-filter"
                    label={labels.eventsFilter.coordinator}
                    value={value.coordinator}
                    onChange={(coordinator) => onChange({ ...value, coordinator })}
                >
                    <option value="">{labels.eventsFilter.coordinatorAll}</option>
                    {coordinatorOptions.map((o) => (
                        <option key={o.value} value={o.value}>{o.prompt}</option>
                    ))}
                </FilterSelect>
            )}

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
