import {type ReactElement} from 'react';
import {Link} from 'react-router-dom';
import {Section} from './MemberSection';
import {labels} from '../../localization';
import {useAuthorizedQuery} from '../../hooks/useAuthorizedFetch';

const l = labels.memberFee;

interface FeeSummaryGroup {
    id: string;
    name: string;
    yearlyFee: number;
}

interface FeeSummaryLinks {
    self: {href: string};
    feeHistory?: {href: string};
}

interface FeeSummaryData {
    currentGroup: FeeSummaryGroup | null;
    votingOpen: boolean;
    recommendedLevelId: string | null;
    _links: FeeSummaryLinks;
}

interface MemberFeeSectionProps {
    feeSummaryHref: string;
    memberId: string;
}

export const MemberFeeSection = ({feeSummaryHref, memberId}: MemberFeeSectionProps): ReactElement => {
    const {data: summary, isLoading} = useAuthorizedQuery<FeeSummaryData>(feeSummaryHref);

    const feeChoicePath = `/members/${memberId}/fee-choice`;

    return (
        <Section title={l.sectionTitle}>
            {isLoading && (
                <p className="text-sm text-text-tertiary">{labels.ui.loading}</p>
            )}

            {!isLoading && summary && (
                <div className="flex flex-col gap-3">
                    {summary.votingOpen && !summary.currentGroup && (
                        <p className="text-sm text-text-secondary">{l.votingOpenNoChoice}</p>
                    )}

                    {summary.votingOpen && summary.currentGroup && (
                        <p className="text-sm text-text-secondary">{l.votingOpenChoiceExists}</p>
                    )}

                    {!summary.votingOpen && !summary.currentGroup && (
                        <p className="text-sm text-warning">{l.deadlineExpired}</p>
                    )}

                    {!summary.votingOpen && summary.currentGroup && (
                        <p className="text-sm text-text-tertiary">{l.choiceLockedInfo}</p>
                    )}

                    {summary.currentGroup && (
                        <div className="flex flex-col gap-1">
                            <p className="text-sm font-medium text-text-primary">{summary.currentGroup.name}</p>
                            <p className="text-sm text-text-secondary">{summary.currentGroup.yearlyFee} {labels.finance.currency}</p>
                        </div>
                    )}

                    {summary.votingOpen && !summary.currentGroup && (
                        <Link
                            to={feeChoicePath}
                            className="text-sm text-primary hover:text-primary-light underline self-start"
                        >
                            {l.chooseLevel}
                        </Link>
                    )}

                    {summary.votingOpen && summary.currentGroup && (
                        <Link
                            to={feeChoicePath}
                            className="text-sm text-primary hover:text-primary-light underline self-start"
                        >
                            {l.changeLevel}
                        </Link>
                    )}

                    {summary._links.feeHistory && (
                        <Link
                            to={`/members/${memberId}/fee-history`}
                            className="text-sm text-text-secondary hover:text-text-primary underline self-start"
                        >
                            {l.viewHistory}
                        </Link>
                    )}
                </div>
            )}
        </Section>
    );
};
