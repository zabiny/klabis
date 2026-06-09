import {type ReactElement, useState} from 'react';
import {Link, useNavigate, useParams} from 'react-router-dom';
import {Alert, Button, Skeleton} from '../../components/UI';
import {labels} from '../../localization';
import {useAuthorizedMutation, useAuthorizedQuery} from '../../hooks/useAuthorizedFetch';
import type {HalFormsTemplate} from '../../api';
import type {FeeSummaryData as FeeSummaryBase} from './memberFeeTypes';
import {Check} from 'lucide-react';

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

const RadioCard = ({
    option,
    selected,
    onSelect,
}: {
    option: ChooseLevelOption;
    selected: boolean;
    onSelect: (value: string) => void;
}): ReactElement => (
    <button
        type="button"
        role="radio"
        aria-checked={selected}
        onClick={() => onSelect(option.value)}
        className="w-full flex items-center gap-3 px-4 rounded-lg border transition-colors cursor-pointer text-left"
        style={{
            height: '56px',
            background: selected ? '#EFF6FF' : '#FFFFFF',
            borderWidth: selected ? '1.5px' : '1px',
            borderColor: selected ? '#2563EB' : '#E4E4E7',
            borderRadius: '8px',
        }}
    >
        {/* Radio indicator */}
        <div
            className="flex-shrink-0 flex items-center justify-center"
            style={{
                width: '18px',
                height: '18px',
                borderRadius: '4px',
                background: selected ? '#2563EB' : '#FFFFFF',
                border: selected ? 'none' : '1.5px solid #D1D5DB',
            }}
        >
            {selected && <Check className="w-3 h-3 text-white" strokeWidth={3}/>}
        </div>

        {/* Option name */}
        <span
            className="flex-1 font-bold"
            style={{fontSize: '14px', color: '#18181B'}}
        >
            {option.prompt}
        </span>
    </button>
);

const LockedChoiceCard = ({name, yearlyFee}: {name: string; yearlyFee: number}): ReactElement => (
    <div
        className="p-4 rounded-lg border"
        style={{background: '#F8FAFC', borderColor: '#E4E4E7', borderRadius: '8px'}}
    >
        <p className="text-sm font-medium mb-1" style={{color: '#71717A'}}>{l.currentLevel}</p>
        <p className="text-lg font-semibold" style={{color: '#18181B'}}>{name}</p>
        <p className="text-sm mt-1" style={{color: '#71717A'}}>
            {yearlyFee} {labels.finance.currency} / rok
        </p>
    </div>
);

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
            {/* Back link */}
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

            {/* Voting closed, no choice made */}
            {!summary?.votingOpen && !summary?.currentGroup && (
                <Alert severity="warning">{l.deadlineExpired}</Alert>
            )}

            {/* Voting closed, choice locked */}
            {!summary?.votingOpen && summary?.currentGroup && (
                <div className="flex flex-col gap-4 max-w-lg">
                    <p className="text-sm" style={{color: '#71717A'}}>{l.choiceLockedInfo}</p>
                    <LockedChoiceCard
                        name={summary.currentGroup.name}
                        yearlyFee={summary.currentGroup.yearlyFee}
                    />
                </div>
            )}

            {/* Voting open */}
            {summary?.votingOpen && (
                <div className="flex flex-col gap-6 max-w-lg">
                    {/* Current choice info */}
                    {summary.currentGroup && (
                        <div className="flex flex-col gap-2">
                            <p className="text-sm font-medium" style={{color: '#71717A'}}>{l.currentLevel}</p>
                            <LockedChoiceCard
                                name={summary.currentGroup.name}
                                yearlyFee={summary.currentGroup.yearlyFee}
                            />
                        </div>
                    )}

                    {/* Radio card selection */}
                    {chooseLevelTemplate && groupOptions.length > 0 && (
                        <div className="flex flex-col gap-3">
                            <p className="text-sm font-medium" style={{color: '#71717A'}}>
                                {l.choiceLevel}
                            </p>

                            <div
                                role="radiogroup"
                                aria-label={l.choiceLevel}
                                className="flex flex-col"
                                style={{gap: '12px'}}
                            >
                                {groupOptions.map((opt) => (
                                    <RadioCard
                                        key={opt.value}
                                        option={opt}
                                        selected={effectiveGroupId === opt.value}
                                        onSelect={setChosenGroupId}
                                    />
                                ))}
                            </div>

                            <div className="flex gap-3 mt-2">
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
                        <p className="text-sm" style={{color: '#71717A'}}>{l.noChoiceMade}</p>
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
