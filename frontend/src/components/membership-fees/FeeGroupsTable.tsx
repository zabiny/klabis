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
        return <p className="text-sm text-[#71717A]">Žádné skupiny.</p>;
    }

    return (
        <div className="bg-white border border-[#E4E4E7] rounded-xl overflow-hidden">
            <table className="w-full text-sm">
                <thead>
                    <tr className="bg-[#F8FAFC] border-b border-[#E4E4E7]" style={{height: '44px'}}>
                        <th className="px-5 text-left text-xs font-semibold text-[#71717A]">Název</th>
                        <th className="px-5 text-right text-xs font-semibold text-[#71717A]">Počet členů</th>
                        <th className="px-5 text-left text-xs font-semibold text-[#71717A]">Stav</th>
                        <th className="px-5 w-12"></th>
                    </tr>
                </thead>
                <tbody>
                    {groups.map((group) => (
                        <tr
                            key={group.id}
                            className="border-b border-[#E4E4E7] last:border-0 hover:bg-[#F8FAFC]"
                            style={{height: '60px'}}
                        >
                            <td className="px-5 text-[#18181B] font-medium">
                                <Link
                                    to={extractNavigationPath(group._links.self.href)}
                                    className="hover:text-[#2563EB]"
                                >
                                    {group.name}
                                </Link>
                            </td>
                            <td className="px-5 text-right text-[#18181B]">{group.memberCount}</td>
                            <td className="px-5">
                                <Badge variant={group.status === 'EDITABLE' ? 'success' : 'default'} size="sm">
                                    {getEnumLabel('feeGroupStatus', group.status)}
                                </Badge>
                            </td>
                            <td className="px-5">
                                <Link
                                    to={extractNavigationPath(group._links.self.href)}
                                    className="flex items-center justify-center w-8 h-8 rounded-[6px] bg-[#F8FAFC] text-[#71717A] hover:bg-[#F1F5F9]"
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
