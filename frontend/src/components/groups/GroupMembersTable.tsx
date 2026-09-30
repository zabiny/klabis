import type {ReactElement} from 'react';
import {Link} from 'react-router-dom';
import {extractNavigationPath} from '../../utils/navigationPath.ts';
import {Button, Card} from '../UI';
import {HalRouteProvider} from '../../contexts/HalRouteContext.tsx';
import {MemberNameWithRegNumber} from '../members/MemberNameWithRegNumber.tsx';
import {formatDate} from '../../utils/dateUtils.ts';
import {labels} from '../../localization';
import {UserMinus} from 'lucide-react';
import type {HalResourceLinks} from '../../api';
import {toHref} from '../../api/hateoas.ts';

export interface GroupMemberRow {
    memberId: string;
    joinedAt: string;
    memberLink: HalResourceLinks | undefined;
    /** Display name already known to the caller; skips loading the member resource for the row. */
    memberName?: string;
    removeAriaLabel?: string;
    onRemove?: () => void;
}

interface GroupMembersTableProps {
    members: GroupMemberRow[];
    emptyMessage: string;
    linkMembers?: boolean;
}

export const GroupMembersTable = ({members, emptyMessage, linkMembers = false}: GroupMembersTableProps): ReactElement => {
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
                {members.map((member) => {
                    const href = member.memberLink ? toHref(member.memberLink) : undefined;
                    const cell = (name: ReactElement) => linkMembers && href
                        ? <Link to={extractNavigationPath(href)} className="hover:text-primary hover:underline">{name}</Link>
                        : name;
                    return (
                        <tr key={member.memberId}
                            className="border-b border-border last:border-0 hover:bg-slate-50 dark:hover:bg-zinc-800/50">
                            <td className="px-4 py-3">
                                {member.memberName !== undefined ? (
                                    cell(<span className="text-text-primary">{member.memberName}</span>)
                                ) : member.memberLink && (
                                    <HalRouteProvider routeLink={member.memberLink}>
                                        {cell(<MemberNameWithRegNumber/>)}
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
                    );
                })}
                </tbody>
            </table>
        </Card>
    );
};
