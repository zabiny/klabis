import {type ReactElement} from 'react';

export const CoParticipationRuleTypeBadge = ({ruleType}: {ruleType: 'PERCENTAGE' | 'FIXED_AMOUNT'}): ReactElement => {
    if (ruleType === 'PERCENTAGE') {
        return (
            <span className="inline-flex items-center px-2 py-0.5 rounded text-xs font-medium bg-blue-50 text-blue-700">
                Procento
            </span>
        );
    }
    return (
        <span className="inline-flex items-center px-2 py-0.5 rounded text-xs font-medium bg-green-50 text-green-700">
            Fixní částka
        </span>
    );
};
