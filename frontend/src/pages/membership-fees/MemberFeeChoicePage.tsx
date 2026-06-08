import {type ReactElement, useState} from 'react';
import {Link, useNavigate, useParams} from 'react-router-dom';
import {Alert, Button, Skeleton} from '../../components/UI';
import {labels} from '../../localization';
import {useAuthorizedMutation, useAuthorizedQuery} from '../../hooks/useAuthorizedFetch';
import type {HalFormsTemplate} from '../../api';
import type {FeeSummaryData as FeeSummaryBase} from './memberFeeTypes';

const l = labels.memberFee;

interface FeeSummaryData extends FeeSummaryBase {
    _templates?: {
        chooseLevel?: HalFormsTemplate;
        cancelChoice?: HalFormsTemplate;
    };
}

type ChooseLevelOption = {
    value: string;
    prompt: string;
};

const currentYear = new Date().getFullYear();

export const MemberFeeChoicePage = (): ReactElement => {
    const {memberId = '', year: yearParam} = useParams<{memberId: string; year?: string}>();
    const year = yearParam ? parseInt(yearParam, 10) : currentYear;
    const navigate = useNavigate();

    const feeSummaryUrl = `/api/members/${memberId}/fee-summary/${year}`;

    const {data: summary, isLoading, error, refetch} = useAuthorizedQuery<FeeSummaryData>(feeSummaryUrl);

    const {mutate: submitChoice, isPending: isChoosing} = useAuthorizedMutation({method: 'POST'});
    const {mutate: cancelChoice, isPending: isCancelling} = useAuthorizedMutation({method: 'DELETE'});

    const [chosenGroupId, setChosenGroupId] = useState<string>('');

    if (isLoading) {
        return <Skeleton/>;
    }

    if (error) {
        return <Alert severity="error">{(error as Error).message}</Alert>;
    }

    const chooseLevelTemplate = summary?._templates?.chooseLevel;
    const cancelChoiceTemplate = summary?._templates?.cancelChoice;

    const groupOptions: ChooseLevelOption[] =
        chooseLevelTemplate?.properties?.[0]?.options?.inline?.flatMap((o) => {
            if (typeof o === 'object' && o !== null && 'value' in o) {
                const item = o as { value: string | number; prompt?: string };
                return [{ value: String(item.value), prompt: item.prompt ?? String(item.value) }];
            }
            return [];
        }) ?? [];

    const effectiveGroupId = chosenGroupId || summary?.recommendedLevelId || groupOptions[0]?.value || '';

    const handleChooseLevel = () => {
        const url = chooseLevelTemplate?.target ?? `/api/members/${memberId}/fee-choice/${year}`;
        submitChoice(
            {url, data: {membershipFeeGroupId: effectiveGroupId}},
            {
                onSuccess: async () => {
                    await refetch();
                },
            },
        );
    };

    const handleCancelChoice = () => {
        const url = cancelChoiceTemplate?.target ?? `/api/members/${memberId}/fee-choice/${year}`;
        cancelChoice(
            {url},
            {
                onSuccess: async () => {
                    await refetch();
                },
            },
        );
    };

    return (
        <div className="flex flex-col gap-8">
            <div>
                <Link
                    to={`/members/${memberId}`}
                    className="text-sm text-primary hover:text-primary-light"
                >
                    {labels.ui.backToList}
                </Link>
            </div>

            <h1 className="text-3xl font-bold text-text-primary">
                {l.choicePageTitle(year)}
            </h1>

            {!summary?.votingOpen && !summary?.currentGroup && (
                <Alert severity="warning">{l.deadlineExpired}</Alert>
            )}

            {!summary?.votingOpen && summary?.currentGroup && (
                <div className="flex flex-col gap-4 max-w-lg">
                    <p className="text-sm text-text-secondary">{l.choiceLockedInfo}</p>
                    <div className="bg-surface-raised rounded-md border border-border p-4">
                        <p className="text-sm font-medium text-text-secondary mb-1">{l.currentLevel}</p>
                        <p className="text-lg font-semibold text-text-primary">{summary.currentGroup.name}</p>
                        <p className="text-sm text-text-secondary mt-1">
                            {summary.currentGroup.yearlyFee} {labels.finance.currency} / rok
                        </p>
                    </div>
                </div>
            )}

            {summary?.votingOpen && (
                <div className="flex flex-col gap-6 max-w-lg">
                    {summary.currentGroup && (
                        <div className="flex flex-col gap-2">
                            <p className="text-sm font-medium text-text-secondary">{l.currentLevel}</p>
                            <div className="bg-surface-raised rounded-md border border-border p-4">
                                <p className="text-lg font-semibold text-text-primary">{summary.currentGroup.name}</p>
                                <p className="text-sm text-text-secondary mt-1">
                                    {summary.currentGroup.yearlyFee} {labels.finance.currency} / rok
                                </p>
                            </div>
                        </div>
                    )}

                    {chooseLevelTemplate && groupOptions.length > 0 && (
                        <div className="flex flex-col gap-3">
                            <label
                                htmlFor="group-select"
                                className="text-sm font-medium text-text-secondary"
                            >
                                {l.choiceLevel}
                            </label>
                            <select
                                id="group-select"
                                value={effectiveGroupId}
                                onChange={(e) => setChosenGroupId(e.target.value)}
                                className="block w-full rounded-md border border-border bg-surface-raised px-3 py-2 text-text-primary text-sm focus:outline-none focus:ring-2 focus:ring-primary"
                            >
                                {groupOptions.map((opt) => (
                                    <option key={opt.value} value={opt.value}>
                                        {opt.prompt}
                                    </option>
                                ))}
                            </select>

                            <div className="flex gap-3">
                                <Button
                                    variant="primary"
                                    onClick={handleChooseLevel}
                                    disabled={!effectiveGroupId || isChoosing}
                                    loading={isChoosing}
                                >
                                    {summary.currentGroup ? l.changeLevel : l.chooseLevel}
                                </Button>

                                {cancelChoiceTemplate && summary.currentGroup && (
                                    <Button
                                        variant="secondary"
                                        onClick={handleCancelChoice}
                                        disabled={isCancelling}
                                        loading={isCancelling}
                                    >
                                        {l.cancelChoice}
                                    </Button>
                                )}
                            </div>
                        </div>
                    )}

                    {!chooseLevelTemplate && !summary.currentGroup && (
                        <p className="text-sm text-text-secondary">{l.noChoiceMade}</p>
                    )}
                </div>
            )}

            <div className="flex gap-4">
                <Button variant="secondary" onClick={() => navigate(`/members/${memberId}`)}>
                    {labels.buttons.cancel}
                </Button>
            </div>
        </div>
    );
};
