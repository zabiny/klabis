import { type ReactElement } from 'react';
import { useSearchParams } from 'react-router-dom';
import { labels } from '../../localization';
import { FulltextSearchInput } from '../UI/FulltextSearchInput';

export type MemberStatusFilter = 'ACTIVE' | 'INACTIVE' | 'ALL';

export interface MembersFilterBarProps {
    hasManageAuthority: boolean;
}

const STATUS_OPTIONS: { value: MemberStatusFilter; label: string }[] = [
    { value: 'ACTIVE', label: labels.membersFilter.statusActive },
    { value: 'INACTIVE', label: labels.membersFilter.statusInactive },
    { value: 'ALL', label: labels.membersFilter.statusAll },
];

export function MembersFilterBar({ hasManageAuthority }: MembersFilterBarProps): ReactElement {
    const [searchParams, setSearchParams] = useSearchParams();
    const currentStatus = (searchParams.get('status') ?? 'ACTIVE') as MemberStatusFilter;

    const handleStatusChange = (status: MemberStatusFilter) => {
        setSearchParams((prev) => {
            const next = new URLSearchParams(prev);
            next.set('status', status);
            return next;
        });
    };

    return (
        <div className="flex flex-wrap items-center gap-3 p-3 bg-surface-raised rounded-md border border-border">
            <FulltextSearchInput
                paramName="q"
                placeholder={labels.membersFilter.searchPlaceholder}
                ariaLabel={labels.membersFilter.search}
            />

            {hasManageAuthority && (
                <div
                    className="inline-flex rounded-md border border-border overflow-hidden"
                    role="group"
                    aria-label={labels.membersFilter.statusLabel}
                >
                    {STATUS_OPTIONS.map(({ value, label }) => (
                        <button
                            key={value}
                            type="button"
                            aria-pressed={currentStatus === value}
                            onClick={() => handleStatusChange(value)}
                            className={`px-3 py-1.5 text-sm font-medium transition-colors focus:outline-none focus:ring-2 focus:ring-accent focus:ring-inset ${
                                currentStatus === value
                                    ? 'bg-primary text-white'
                                    : 'bg-surface text-text-primary hover:bg-surface-hover'
                            }`}
                        >
                            {label}
                        </button>
                    ))}
                </div>
            )}
        </div>
    );
}
