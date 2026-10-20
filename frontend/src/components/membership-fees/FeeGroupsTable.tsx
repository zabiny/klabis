import {type ReactElement} from 'react';
import {Link} from 'react-router-dom';
import {ArrowRight} from 'lucide-react';
import {getEnumLabel} from '../../localization';
import {extractNavigationPath} from '../../utils/navigationPath.ts';
import {Badge} from '../UI/Badge.tsx';

export interface FeeGroupSummary {
    id: string;
    name: string;
    memberCount: number;
    status: 'EDITABLE' | 'FROZEN';
    _links: {
        self: {href: string};
    };
}

interface FeeGroupsTableProps {
    groups: FeeGroupSummary[];
}

export const FeeGroupsTable = ({groups}: FeeGroupsTableProps): ReactElement => {
    if (groups.length === 0) {
        return <p className="text-sm text-text-secondary">Žádné skupiny.</p>;
    }

    return (
        <div className="bg-surface border border-border-subtle rounded-xl overflow-hidden">
            <table className="w-full text-sm">
                <thead>
                    <tr className="bg-bg-subtle border-b border-border-subtle" style={{height: '44px'}}>
                        <th className="px-5 text-left text-xs font-semibold text-text-secondary">Název</th>
                        <th className="px-5 text-right text-xs font-semibold text-text-secondary">Počet členů</th>
                        <th className="px-5 text-left text-xs font-semibold text-text-secondary">Stav</th>
                        <th className="px-5 w-12"></th>
                    </tr>
                </thead>
                <tbody>
                    {groups.map((group) => (
                        <tr
                            key={group.id}
                            className="border-b border-border-subtle last:border-0 hover:bg-bg-subtle"
                            style={{height: '60px'}}
                        >
                            <td className="px-5 text-text-primary font-medium">
                                <Link
                                    to={extractNavigationPath(group._links.self.href)}
                                    className="hover:text-primary"
                                >
                                    {group.name}
                                </Link>
                            </td>
                            <td className="px-5 text-right text-text-primary">{group.memberCount}</td>
                            <td className="px-5">
                                <Badge variant={group.status === 'EDITABLE' ? 'success' : 'default'} size="sm">
                                    {getEnumLabel('feeGroupStatus', group.status)}
                                </Badge>
                            </td>
                            <td className="px-5">
                                <Link
                                    to={extractNavigationPath(group._links.self.href)}
                                    className="flex items-center justify-center w-8 h-8 rounded-[6px] bg-bg-subtle text-text-secondary hover:bg-bg-subtle"
                                    aria-label="Otevřít detail skupiny"
                                >
                                    <ArrowRight size={16}/>
                                </Link>
                            </td>
                        </tr>
                    ))}
                </tbody>
            </table>
        </div>
    );
};
