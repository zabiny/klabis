import {type ReactElement} from 'react';
import {Link, useNavigate} from 'react-router-dom';
import {useHalPageData} from '../../hooks/useHalPageData.ts';
import {useAuthorizedQuery} from '../../hooks/useAuthorizedFetch.ts';
import {Alert, Card, Skeleton} from '../../components/UI';
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
        <div className="flex flex-col gap-8">
            <div>
                <Link to="/fee-year-publications" className="text-sm text-primary hover:text-primary-light">
                    {labels.ui.backToList}
                </Link>
            </div>

            <h1 className="text-3xl font-bold text-text-primary">{resourceData.year}</h1>

            <Card className="p-6">
                <dl className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                    <div>
                        <dt className="text-xs uppercase font-semibold text-text-secondary">{labels.fields.votingDeadline}</dt>
                        <dd className="mt-1 text-text-primary font-medium">{resourceData.votingDeadline ? formatDate(resourceData.votingDeadline) : '—'}</dd>
                    </div>
                </dl>
            </Card>

            <div className="flex flex-col gap-4">
                <h2 className="text-xl font-bold text-text-primary">{labels.sections.membershipFeeGroups}</h2>
                {groups.length === 0 ? (
                    <p className="text-text-secondary text-sm">Žádné skupiny.</p>
                ) : (
                    <Card className="p-0 overflow-hidden">
                        <table className="w-full text-sm">
                            <thead>
                                <tr className="border-b border-border bg-surface-secondary">
                                    <th className="px-4 py-3 text-left font-semibold text-text-secondary">{labels.fields.name}</th>
                                    <th className="px-4 py-3 text-right font-semibold text-text-secondary">{labels.fields.memberCount}</th>
                                    <th className="px-4 py-3 text-left font-semibold text-text-secondary">{labels.fields.groupStatus}</th>
                                </tr>
                            </thead>
                            <tbody>
                                {groups.map((group) => (
                                    <tr
                                        key={group.id}
                                        className="border-b border-border last:border-0 hover:bg-surface-secondary cursor-pointer"
                                        onClick={() => handleGroupClick(group)}
                                    >
                                        <td className="px-4 py-3 text-text-primary font-medium">
                                            <Link
                                                to={extractNavigationPath(group._links.self.href)}
                                                className="text-primary hover:text-primary-light"
                                                onClick={(e) => e.stopPropagation()}
                                            >
                                                {group.name}
                                            </Link>
                                        </td>
                                        <td className="px-4 py-3 text-right text-text-primary">{group.memberCount}</td>
                                        <td className="px-4 py-3 text-text-primary">
                                            <span className={`inline-flex items-center px-2 py-0.5 rounded text-xs font-medium ${
                                                group.status === 'EDITABLE'
                                                    ? 'bg-green-100 text-green-800 dark:bg-green-900 dark:text-green-200'
                                                    : 'bg-zinc-100 text-zinc-700 dark:bg-zinc-800 dark:text-zinc-300'
                                            }`}>
                                                {getEnumLabel('feeGroupStatus', group.status)}
                                            </span>
                                        </td>
                                    </tr>
                                ))}
                            </tbody>
                        </table>
                    </Card>
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
