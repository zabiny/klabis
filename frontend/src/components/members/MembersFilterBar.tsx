import { type ReactElement } from 'react';
import { labels } from '../../localization';
import { FulltextSearchInput, PillGroup } from '../UI';

export type MemberStatusFilter = 'ACTIVE' | 'INACTIVE' | 'ALL';

export const DEFAULT_MEMBER_STATUS: MemberStatusFilter = 'ACTIVE';

export type MembersFilterValue = {
    q: string;
    status: MemberStatusFilter;
};

export interface MembersFilterBarProps {
    value: MembersFilterValue;
    onChange: (next: MembersFilterValue) => void;
    hasManageAuthority: boolean;
}

const STATUS_OPTIONS: { value: MemberStatusFilter; label: string }[] = [
    { value: 'ACTIVE', label: labels.membersFilter.statusActive },
    { value: 'INACTIVE', label: labels.membersFilter.statusInactive },
    { value: 'ALL', label: labels.membersFilter.statusAll },
];

export function MembersFilterBar({
    value,
    onChange,
    hasManageAuthority,
}: MembersFilterBarProps): ReactElement {
    return (
        <div className="flex flex-wrap items-center gap-3 p-3 bg-surface-raised rounded-md border border-border">
            <FulltextSearchInput
                value={value.q}
                onChange={(q) => onChange({ ...value, q })}
                placeholder={labels.membersFilter.searchPlaceholder}
                ariaLabel={labels.membersFilter.search}
            />

            {hasManageAuthority && (
                <PillGroup<MemberStatusFilter>
                    options={STATUS_OPTIONS}
                    selectedValue={value.status}
                    onChange={(status) => onChange({ ...value, status })}
                    ariaLabel={labels.membersFilter.statusLabel}
                />
            )}
        </div>
    );
}
