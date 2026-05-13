import type {ReactElement} from 'react';
import {Button, Card} from '../UI';
import {HalRouteProvider} from '../../contexts/HalRouteContext.tsx';
import {MemberNameWithRegNumber} from '../members/MemberNameWithRegNumber.tsx';
import {formatDate} from '../../utils/dateUtils.ts';
import {labels} from '../../localization';
import {UserMinus} from 'lucide-react';
import type {HalResourceLinks} from '../../api';

export interface GroupMemberRow {
    memberId: string;
    joinedAt: string;
    memberLink: HalResourceLinks | undefined;
    removeAriaLabel?: string;
    onRemove?: () => void;
}

interface GroupMembersTableProps {
    members: GroupMemberRow[];
    emptyMessage: string;
}

export const GroupMembersTable = ({members, emptyMessage}: GroupMembersTableProps): ReactElement => {
    if (members.length === 0) {
        return <p className="text-sm text-text-tertiary">{emptyMessage}</p>;
    }

    return (
        <Card className="p-0 overflow-hidden">
            <table className="w-full text-sm">
                <thead>
                <tr className="border-b border-border bg-slate-50 dark:bg-zinc-800">
                    <th className="text-left px-4 py-3 font-medium text-text-secondary">
                        {labels.fields.memberId}
                    </th>
                    <th className="text-left px-4 py-3 font-medium text-text-secondary">
                        {labels.tables.joinedAt}
                    </th>
                    <th className="px-4 py-3"/>
                </tr>
                </thead>
                <tbody>
                {members.map((member) => (
                    <tr key={member.memberId}
                        className="border-b border-border last:border-0 hover:bg-slate-50 dark:hover:bg-zinc-800/50">
                        <td className="px-4 py-3">
                            {member.memberLink && (
                                <HalRouteProvider routeLink={member.memberLink}>
                                    <MemberNameWithRegNumber/>
                                </HalRouteProvider>
                            )}
                        </td>
                        <td className="px-4 py-3 text-text-secondary">{formatDate(member.joinedAt)}</td>
                        <td className="px-4 py-3 text-right">
                            {member.onRemove && (
                                <Button
                                    variant="ghost"
                                    size="sm"
                                    className="text-red-600"
                                    aria-label={member.removeAriaLabel}
                                    onClick={member.onRemove}
                                >
                                    <UserMinus className="w-4 h-4"/>
                                </Button>
                            )}
                        </td>
                    </tr>
                ))}
                </tbody>
            </table>
        </Card>
    );
};
