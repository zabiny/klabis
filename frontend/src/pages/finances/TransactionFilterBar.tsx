import {type ReactElement} from 'react';
import {labels} from '../../localization';
import {PillGroup} from '../../components/UI';

export type TransactionTypeFilter = 'DEPOSIT' | 'OTHER' | '';

export type TransactionFilterValue = {
    type: TransactionTypeFilter | string;
    dateFrom: string;
    dateTo: string;
};

export interface TransactionFilterBarProps {
    value: TransactionFilterValue;
    onChange: (next: TransactionFilterValue) => void;
}

const TYPE_OPTIONS: {value: TransactionTypeFilter; label: string}[] = [
    {value: '', label: labels.finance.filterTypeAll},
    {value: 'DEPOSIT', label: labels.finance.typeDeposit},
    {value: 'OTHER', label: labels.finance.typeOther},
];

export function TransactionFilterBar({value, onChange}: TransactionFilterBarProps): ReactElement {
    return (
        <div className="flex flex-wrap items-end gap-4 p-3 bg-surface-raised rounded-md border border-border">
            <div className="flex flex-col gap-1">
                <label className="text-xs font-semibold text-text-secondary uppercase tracking-wide">
                    {labels.finance.filterDateFrom}
                </label>
                <input
                    type="date"
                    value={value.dateFrom}
                    onChange={(e) => onChange({...value, dateFrom: e.target.value})}
                    className="px-3 py-1.5 rounded border border-border bg-surface text-sm text-text-primary focus:outline-none focus:ring-2 focus:ring-blue-500"
                />
            </div>

            <div className="flex flex-col gap-1">
                <label className="text-xs font-semibold text-text-secondary uppercase tracking-wide">
                    {labels.finance.filterDateTo}
                </label>
                <input
                    type="date"
                    value={value.dateTo}
                    onChange={(e) => onChange({...value, dateTo: e.target.value})}
                    className="px-3 py-1.5 rounded border border-border bg-surface text-sm text-text-primary focus:outline-none focus:ring-2 focus:ring-blue-500"
                />
            </div>

            <div className="flex flex-col gap-1">
                <span className="text-xs font-semibold text-text-secondary uppercase tracking-wide">
                    {labels.finance.filterType}
                </span>
                <PillGroup<string>
                    options={TYPE_OPTIONS}
                    selectedValue={value.type}
                    onChange={(type) => onChange({...value, type: type as TransactionTypeFilter})}
                    ariaLabel={labels.finance.filterType}
                />
            </div>
        </div>
    );
}
