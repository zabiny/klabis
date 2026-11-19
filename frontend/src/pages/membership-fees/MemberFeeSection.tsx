import {type ReactElement, useState} from 'react';
import {Section} from '../members/MemberSection';
import {Alert, Button} from '../../components/UI';
import {describeError, labels} from '../../localization';
import {useAuthorizedMutation, useAuthorizedQuery} from '../../hooks/useAuthorizedFetch';
import {halResource} from '../../api/halResource';
import type {GetFeeSummaryResource} from '../../api';
import {MembershipFeeGroupRadioCards, useMembershipFeeGroupOptions} from '../../components/HalNavigator2/halforms/fields';

const l = labels.memberFee;

interface MemberFeeSectionProps {
    feeSummaryHref: string;
}

export const MemberFeeSection = ({feeSummaryHref}: MemberFeeSectionProps): ReactElement => {
    const {data: summary, isLoading} = useAuthorizedQuery<GetFeeSummaryResource>(feeSummaryHref);

    return (
        <Section title={l.sectionTitle}>
            {isLoading && (
                <p className="text-sm text-text-tertiary">{labels.ui.loading}</p>
            )}

            {!isLoading && summary && <FeeSummary summary={summary}/>}
        </Section>
    );
};

const CurrentChoiceCard = ({name, yearlyFee}: {name?: string; yearlyFee?: number}): ReactElement => (
    <div className="p-4 rounded-lg border border-border-subtle bg-bg-subtle">
        <p className="text-sm font-medium mb-1 text-text-secondary">{l.currentTier}</p>
        <p className="text-lg font-semibold text-text-primary">{name}</p>
        {yearlyFee !== undefined && (
            <p className="text-sm mt-1 text-text-secondary">
                {yearlyFee} {labels.finance.currency} / rok
            </p>
        )}
    </div>
);

const FeeSummary = ({summary}: {summary: GetFeeSummaryResource}): ReactElement => {
    const actions = halResource(summary);
    const chooseAction = actions.template('chooseFeeType');
    const removeAction = actions.template('removeChoice');
    const optionsHref = chooseAction?.template.properties?.find(p => p.name === 'membershipFeeGroupId')?.options?.link?.href;

    const {mutate: submitChoice, isPending: isChoosing, error: chooseError} = useAuthorizedMutation({method: 'POST'});
    const {mutate: cancelChoice, isPending: isCancelling, error: cancelError} = useAuthorizedMutation({method: 'DELETE'});
    const {groups, isLoading: optionsLoading, error: optionsError, unavailable} = useMembershipFeeGroupOptions(optionsHref);
    const [chosenGroupId, setChosenGroupId] = useState<string>('');

    const isOffered = (id: string | null | undefined): id is string => !!id && groups.some(g => g.id === id);
    const selectedGroupId = [chosenGroupId, summary.recommendedGroupId].find(isOffered) ?? '';
    const canChoose = Boolean(summary.votingOpen && chooseAction);
    const mutationError = chooseError ?? cancelError;

    const handleChoose = () => {
        if (!chooseAction?.href) return;
        submitChoice(
            {url: chooseAction.href, data: {membershipFeeGroupId: selectedGroupId}},
            {onSuccess: () => setChosenGroupId('')},
        );
    };

    const handleCancel = () => {
        if (!removeAction?.href) return;
        cancelChoice({url: removeAction.href}, {onSuccess: () => setChosenGroupId('')});
    };

    return (
        <div className="flex flex-col gap-3 max-w-lg">
            {canChoose && !summary.currentGroup && (
                <p className="text-sm text-text-secondary">{l.votingOpenNoChoice}</p>
            )}

            {canChoose && summary.currentGroup && (
                <p className="text-sm text-text-secondary">{l.votingOpenChoiceExists}</p>
            )}

            {!summary.votingOpen && !summary.currentGroup && (
                <p className="text-sm text-warning">{l.deadlineExpired}</p>
            )}

            {!summary.votingOpen && summary.currentGroup && (
                <p className="text-sm text-text-tertiary">{l.choiceLockedInfo}</p>
            )}

            {summary.currentGroup && (
                <CurrentChoiceCard name={summary.currentGroup.name} yearlyFee={summary.currentGroup.yearlyFee}/>
            )}

            {canChoose && (
                <>
                    {optionsLoading && <p className="text-sm text-text-tertiary">{labels.ui.loading}</p>}
                    {(unavailable || optionsError) && (
                        <Alert severity="error">{optionsError ? describeError(optionsError) : l.optionsUnavailable}</Alert>
                    )}

                    <MembershipFeeGroupRadioCards
                        groups={groups}
                        value={selectedGroupId}
                        onChange={setChosenGroupId}
                        ariaLabel={l.choiceTier}
                    />

                    {mutationError && <Alert severity="error">{describeError(mutationError)}</Alert>}

                    <div className="flex gap-3">
                        <Button
                            variant="primary"
                            onClick={handleChoose}
                            disabled={!selectedGroupId || isChoosing}
                            loading={isChoosing}
                        >
                            {summary.currentGroup ? l.changeTier : l.chooseTier}
                        </Button>

                        {removeAction && summary.currentGroup && (
                            <Button
                                variant="secondary"
                                onClick={handleCancel}
                                disabled={isCancelling}
                                loading={isCancelling}
                            >
                                {l.cancelChoice}
                            </Button>
                        )}
                    </div>
                </>
            )}
        </div>
    );
};
