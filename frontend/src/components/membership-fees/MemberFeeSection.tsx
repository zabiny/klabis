import {type ReactElement} from 'react';
import {Section} from '../../pages/members/MemberSection';
import {Alert, Button} from '../UI';
import {HalFormDisplay} from '../HalNavigator2/HalFormDisplay';
import {describeError, labels} from '../../localization';
import {useAuthorizedMutation, useAuthorizedQuery} from '../../hooks/useAuthorizedFetch';
import {halResource} from '../../api/halResource';
import type {GetFeeSummaryResource} from '../../api';

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

    const {mutate: cancelChoice, isPending: isCancelling, error: cancelError} = useAuthorizedMutation({method: 'DELETE'});

    const canChoose = Boolean(summary.votingOpen && chooseAction);

    const handleCancel = () => {
        if (!removeAction?.href) return;
        cancelChoice({url: removeAction.href});
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

            {summary.currentGroup && !canChoose && (
                <CurrentChoiceCard name={summary.currentGroup.name} yearlyFee={summary.currentGroup.yearlyFee}/>
            )}

            {canChoose && chooseAction && (
                <>
                    {cancelError && <Alert severity="error">{describeError(cancelError)}</Alert>}

                    <HalFormDisplay
                        key={summary.currentGroup?.id ?? 'none'}
                        template={chooseAction.template}
                        templateName={chooseAction.name}
                        href={chooseAction.href}
                        resourceData={{...summary, membershipFeeGroupId: summary.currentGroup?.id ?? summary.recommendedGroupId ?? ''}}
                        onClose={() => undefined}
                        navigateOnSuccess={false}
                        submitButtonLabel={summary.currentGroup ? l.changeTier : l.chooseTier}
                        customLayout={({renderField}) => (
                            <>
                                {renderField('membershipFeeGroupId')}
                                <div className="flex gap-3">
                                    {renderField('submit')}
                                    {removeAction && summary.currentGroup && (
                                        <Button
                                            type="button"
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
                    />
                </>
            )}
        </div>
    );
};
