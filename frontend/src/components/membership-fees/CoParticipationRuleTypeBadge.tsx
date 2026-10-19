import {type ReactElement} from 'react';

export const CoParticipationRuleTypeBadge = ({ruleType}: {ruleType: 'PERCENTAGE' | 'FIXED_AMOUNT'}): ReactElement => {
    if (ruleType === 'PERCENTAGE') {
        return (
            <span className="inline-flex items-center px-2 py-0.5 rounded text-xs font-medium bg-primary-subtle text-primary">
                Procento
            </span>
        );
    }
    return (
        <span className="inline-flex items-center px-2 py-0.5 rounded text-xs font-medium bg-success-bg text-success">
            Fixní částka
        </span>
    );
};
