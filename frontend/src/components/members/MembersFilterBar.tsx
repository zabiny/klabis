import { type ReactElement } from 'react';
import { useSearchParams } from 'react-router-dom';
import { labels } from '../../localization';
import { FulltextSearchInput, PillGroup } from '../UI';

export type MemberStatusFilter = 'ACTIVE' | 'INACTIVE' | 'ALL';

export const DEFAULT_MEMBER_STATUS: MemberStatusFilter = 'ACTIVE';

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
        if (status === currentStatus) return;
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
                <PillGroup<MemberStatusFilter>
                    options={STATUS_OPTIONS}
                    selectedValue={currentStatus}
                    onChange={handleStatusChange}
                    ariaLabel={labels.membersFilter.statusLabel}
                />
            )}
        </div>
    );
}
