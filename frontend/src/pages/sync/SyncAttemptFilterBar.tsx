import {type ReactElement} from 'react';
import {getEnumLabel, labels} from '../../localization';
import type {SyncAttemptFilters} from './useSyncAttemptFilters.ts';

const ENTITY_TYPES = Object.keys(labels.enums.syncEntityType);
const OUTCOMES = Object.keys(labels.enums.syncOutcome);

const labelClass = 'text-xs font-semibold text-text-secondary uppercase tracking-wide';
const controlClass = 'px-3 py-1.5 rounded border border-border bg-surface text-sm text-text-primary focus:outline-none focus:ring-2 focus:ring-blue-500';

interface SyncAttemptFilterBarProps {
    value: SyncAttemptFilters;
    onChange: (next: SyncAttemptFilters) => void;
}

export function SyncAttemptFilterBar({value, onChange}: SyncAttemptFilterBarProps): ReactElement {
    const f = labels.syncOverview.filters;

    return (
        <div className="flex flex-wrap items-end gap-4 p-3 bg-surface-raised rounded-md border border-border">
            <div className="flex flex-col gap-1">
                <label htmlFor="sync-filter-entity-type" className={labelClass}>{f.entityType}</label>
                <select id="sync-filter-entity-type" className={controlClass} value={value.entityType}
                        onChange={(e) => onChange({...value, entityType: e.target.value})}>
                    <option value="">{f.all}</option>
                    {ENTITY_TYPES.map(t => <option key={t} value={t}>{getEnumLabel('syncEntityType', t)}</option>)}
                </select>
            </div>

            <div className="flex flex-col gap-1">
                <label htmlFor="sync-filter-outcome" className={labelClass}>{f.outcome}</label>
                <select id="sync-filter-outcome" className={controlClass} value={value.outcome}
                        onChange={(e) => onChange({...value, outcome: e.target.value})}>
                    <option value="">{f.all}</option>
                    {OUTCOMES.map(o => <option key={o} value={o}>{getEnumLabel('syncOutcome', o)}</option>)}
                </select>
            </div>

            <div className="flex flex-col gap-1">
                <label htmlFor="sync-filter-date-from" className={labelClass}>{f.dateFrom}</label>
                <input id="sync-filter-date-from" type="date" className={controlClass} value={value.dateFrom}
                       onChange={(e) => onChange({...value, dateFrom: e.target.value})}/>
            </div>

            <div className="flex flex-col gap-1">
                <label htmlFor="sync-filter-date-to" className={labelClass}>{f.dateTo}</label>
                <input id="sync-filter-date-to" type="date" className={controlClass} value={value.dateTo}
                       onChange={(e) => onChange({...value, dateTo: e.target.value})}/>
            </div>

            <label className="flex items-center gap-2 text-sm text-text-primary pb-2">
                <input type="checkbox" checked={value.includeUnchanged}
                       onChange={(e) => onChange({...value, includeUnchanged: e.target.checked})}/>
                {f.includeUnchanged}
            </label>
        </div>
    );
}
