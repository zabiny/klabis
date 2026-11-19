import {type ReactElement} from 'react';
import {Link, useParams} from 'react-router-dom';
import {KlabisTable, TableCell} from '../../components/KlabisTable';
import {HalPageGate} from '../HalPageGate.tsx';
import {toHref} from '../../api/hateoas.ts';
import {toRoutePath} from '../../api/apiPath.ts';
import {getEnumLabel, labels} from '../../localization';
import type {GetFeeHistoryResource} from '../../api';
import {MemberFeeSection} from './MemberFeeSection.tsx';

const l = labels.memberFee;

type FeeAssignment = NonNullable<GetFeeHistoryResource['assignments']>[number];

const MemberFeeHistoryContent = ({resourceData}: {resourceData: GetFeeHistoryResource}): ReactElement => {
    const {memberId = ''} = useParams();

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
                        column="yearlyFee"
                        dataRender={({value}) => `${value} ${labels.finance.currency}`}
                    >
                        {l.historyFee}
                    </TableCell>
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

export const MemberFeeHistoryPage = (): ReactElement => (
    <HalPageGate<GetFeeHistoryResource>>
        {(resourceData) => <MemberFeeHistoryContent resourceData={resourceData}/>}
    </HalPageGate>
);
