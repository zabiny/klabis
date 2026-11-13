import {type ReactElement} from 'react';
import {Link, useParams} from 'react-router-dom';
import {useHalPageData} from '../../hooks/useHalPageData.ts';
import {Skeleton} from '../../components/UI';
import {KlabisTable, TableCell} from '../../components/KlabisTable';
import {ErrorPage} from '../ErrorPage.tsx';
import {toHref} from '../../api/hateoas.ts';
import {toRoutePath} from '../../api/apiPath.ts';
import {getEnumLabel, labels} from '../../localization';
import type {GetFeeHistoryResource} from '../../api';
import {MemberFeeSection} from './MemberFeeSection.tsx';

const l = labels.memberFee;

type FeeAssignment = NonNullable<GetFeeHistoryResource['assignments']>[number];

export const MemberFeeHistoryPage = (): ReactElement => {
    const {resourceData, isLoading, error} = useHalPageData<GetFeeHistoryResource>();
    const {memberId = ''} = useParams();

    if (isLoading) return <Skeleton/>;
    if (error) return <ErrorPage error={error}/>;
    if (!resourceData) return <Skeleton/>;

    const memberLink = resourceData._links?.member;
    const feeSummaryLink = resourceData._links?.feeSummary;
    const assignments = resourceData.assignments ?? [];

    return (
        <div className="flex flex-col gap-6">
            {memberLink && (
                <nav className="text-sm text-text-secondary">
                    <Link to={toRoutePath(toHref(memberLink))} className="hover:text-text-primary transition-colors">
                        {l.backToMember}
                    </Link>
                </nav>
            )}

            <h1 className="text-3xl font-bold text-text-primary">{l.pageTitle}</h1>

            {feeSummaryLink && (
                <MemberFeeSection feeSummaryHref={toHref(feeSummaryLink)} memberId={memberId}/>
            )}

            <div className="flex flex-col gap-3">
                <h2 className="text-xl font-bold text-text-primary">{l.historyTitle}</h2>
                <KlabisTable<FeeAssignment>
                    data={assignments}
                    emptyMessage={l.noHistory}
                >
                    <TableCell column="year">{l.historyYear}</TableCell>
                    <TableCell column="groupName">{l.historyLevel}</TableCell>
                    <TableCell
                        column="source"
                        dataRender={({value}) => getEnumLabel('feeMemberSource', String(value))}
                    >
                        {l.historySource}
                    </TableCell>
                </KlabisTable>
            </div>
        </div>
    );
};
