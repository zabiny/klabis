import {type ReactElement} from 'react';
import {Link} from 'react-router-dom';
import {Section} from '../members/MemberSection';
import {labels} from '../../localization';
import {useAuthorizedQuery} from '../../hooks/useAuthorizedFetch';
import type {GetFeeSummaryResource} from '../../api';

const l = labels.memberFee;

interface MemberFeeSectionProps {
    feeSummaryHref: string;
    memberId: string;
}

export const MemberFeeSection = ({feeSummaryHref, memberId}: MemberFeeSectionProps): ReactElement => {
    const {data: summary, isLoading} = useAuthorizedQuery<GetFeeSummaryResource>(feeSummaryHref);

    return (
        <Section title={l.sectionTitle}>
            {isLoading && (
                <p className="text-sm text-text-tertiary">{labels.ui.loading}</p>
            )}

            {!isLoading && summary && renderSummary(summary, memberId)}
        </Section>
    );
};

function renderSummary(summary: GetFeeSummaryResource, memberId: string): ReactElement {
    const feeChoicePath = `/members/${memberId}/fee-choice/${summary.year}`;
    const canChoose = Boolean(summary._templates?.chooseFeeType);

    return (
        <div className="flex flex-col gap-3">
            {summary.votingOpen && canChoose && !summary.currentGroup && (
                <p className="text-sm text-text-secondary">{l.votingOpenNoChoice}</p>
            )}

            {summary.votingOpen && canChoose && summary.currentGroup && (
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

            {summary.votingOpen && canChoose && !summary.currentGroup && (
                <Link
                    to={feeChoicePath}
                    className="text-sm text-primary hover:text-primary-light underline self-start"
                >
                    {l.chooseTier}
                </Link>
            )}

            {summary.votingOpen && canChoose && summary.currentGroup && (
                <Link
                    to={feeChoicePath}
                    className="text-sm text-primary hover:text-primary-light underline self-start"
                >
                    {l.changeTier}
                </Link>
            )}

        </div>
    );
}
