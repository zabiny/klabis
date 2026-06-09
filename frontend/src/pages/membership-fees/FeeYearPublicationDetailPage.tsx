import {type ReactElement} from 'react';
import {Link, useNavigate} from 'react-router-dom';
import {ArrowRight} from 'lucide-react';
import {useHalPageData} from '../../hooks/useHalPageData.ts';
import {useAuthorizedQuery} from '../../hooks/useAuthorizedFetch.ts';
import {Alert, Skeleton} from '../../components/UI';
import type {HalResponse} from '../../api';
import {labels, getEnumLabel} from '../../localization';
import {formatDate} from '../../utils/dateUtils.ts';
import {extractNavigationPath} from '../../utils/navigationPath.ts';

interface FeeGroupSummary {
    id: string;
    name: string;
    memberCount: number;
    status: 'EDITABLE' | 'FROZEN';
    _links: {
        self: {href: string};
    };
}

interface FeeGroupsCollection {
    _embedded?: {
        membershipFeeGroupResponseList?: FeeGroupSummary[];
    };
}

interface FeeYearPublicationDetail extends HalResponse {
    id: string;
    year: number;
    votingDeadline: string;
    _links: {
        self: {href: string};
        levels?: {href: string};
    };
}

const GroupStatusBadge = ({status}: {status: FeeGroupSummary['status']}): ReactElement => {
    const colorClass = status === 'EDITABLE'
        ? 'bg-[#DCFCE7] text-[#15803D]'
        : 'bg-[#F4F4F5] text-[#71717A]';
    return (
        <span className={`inline-flex items-center px-2 py-0.5 rounded text-xs font-medium ${colorClass}`}>
            {getEnumLabel('feeGroupStatus', status)}
        </span>
    );
};

const FeeYearPublicationDetailContent = ({resourceData}: {resourceData: FeeYearPublicationDetail}): ReactElement => {
    const navigate = useNavigate();
    const levelsHref = resourceData._links?.levels?.href ?? '';
    const {data: groupsData} = useAuthorizedQuery<FeeGroupsCollection>(
        levelsHref ? extractNavigationPath(levelsHref) : '',
        {enabled: !!levelsHref}
    );
    const groups = groupsData?._embedded?.membershipFeeGroupResponseList ?? [];

    const handleGroupClick = (group: FeeGroupSummary) => {
        navigate(extractNavigationPath(group._links.self.href));
    };

    return (
        <div className="flex flex-col gap-6">
            <div className="flex items-center gap-2 text-sm text-[#71717A]">
                <Link to="/fee-year-publications" className="hover:text-[#18181B]">
                    {labels.ui.backToList}
                </Link>
                <span>/</span>
                <span className="text-[#18181B] font-medium">{resourceData.year}</span>
            </div>

            <h1 className="text-[28px] font-bold text-[#18181B]">{resourceData.year}</h1>

            <div className="bg-white border border-[#E4E4E7] rounded-xl p-6">
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-6">
                    <div>
                        <p className="text-xs font-semibold text-[#71717A] uppercase tracking-wide">Rok</p>
                        <p className="mt-1 text-[#18181B] font-medium">{resourceData.year}</p>
                    </div>
                    <div>
                        <p className="text-xs font-semibold text-[#71717A] uppercase tracking-wide">{labels.fields.votingDeadline}</p>
                        <p className="mt-1 text-[#18181B] font-medium">
                            {resourceData.votingDeadline ? formatDate(resourceData.votingDeadline) : '—'}
                        </p>
                    </div>
                </div>
            </div>

            <div className="flex flex-col gap-4">
                <h2 className="text-xl font-bold text-[#18181B]">{labels.sections.membershipFeeGroups}</h2>
                {groups.length === 0 ? (
                    <p className="text-sm text-[#71717A]">Žádné skupiny.</p>
                ) : (
                    <div className="bg-white border border-[#E4E4E7] rounded-xl overflow-hidden">
                        <table className="w-full text-sm">
                            <thead>
                                <tr className="bg-[#F8FAFC] border-b border-[#E4E4E7]" style={{height: '44px'}}>
                                    <th className="px-5 text-left text-xs font-semibold text-[#71717A]">{labels.fields.name}</th>
                                    <th className="px-5 text-right text-xs font-semibold text-[#71717A]">{labels.fields.memberCount}</th>
                                    <th className="px-5 text-left text-xs font-semibold text-[#71717A]">{labels.fields.groupStatus}</th>
                                    <th className="px-5 w-12"></th>
                                </tr>
                            </thead>
                            <tbody>
                                {groups.map((group) => (
                                    <tr
                                        key={group.id}
                                        className="border-b border-[#E4E4E7] last:border-0 hover:bg-[#F8FAFC] cursor-pointer"
                                        style={{height: '60px'}}
                                        onClick={() => handleGroupClick(group)}
                                    >
                                        <td className="px-5 text-[#18181B] font-medium">
                                            <Link
                                                to={extractNavigationPath(group._links.self.href)}
                                                className="hover:text-[#2563EB]"
                                                onClick={(e) => e.stopPropagation()}
                                            >
                                                {group.name}
                                            </Link>
                                        </td>
                                        <td className="px-5 text-right text-[#18181B]">{group.memberCount}</td>
                                        <td className="px-5">
                                            <GroupStatusBadge status={group.status}/>
                                        </td>
                                        <td className="px-5">
                                            <button
                                                type="button"
                                                className="flex items-center justify-center w-8 h-8 rounded-[6px] bg-[#F8FAFC] text-[#71717A] hover:bg-[#F1F5F9]"
                                                aria-label="Otevřít detail skupiny"
                                                onClick={(e) => {
                                                    e.stopPropagation();
                                                    handleGroupClick(group);
                                                }}
                                            >
                                                <ArrowRight size={16}/>
                                            </button>
                                        </td>
                                    </tr>
                                ))}
                            </tbody>
                        </table>
                    </div>
                )}
            </div>
        </div>
    );
};

export const FeeYearPublicationDetailPage = (): ReactElement => {
    const {resourceData, isLoading, error} = useHalPageData<FeeYearPublicationDetail>();

    if (isLoading) {
        return <Skeleton/>;
    }

    if (error) {
        return <Alert severity="error">{error.message}</Alert>;
    }

    if (!resourceData) {
        return <Skeleton/>;
    }

    return <FeeYearPublicationDetailContent resourceData={resourceData}/>;
};
