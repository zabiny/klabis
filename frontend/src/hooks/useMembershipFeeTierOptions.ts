import type {components} from '../api/klabisApi';
import {useHalCollection} from './useHalCollection.ts';

type TierSummary = components['schemas']['EntityModelMembershipFeeTierSummaryResponse'];

export interface TierOption {
    value: string;
    prompt: string;
}

const toTierOptions = (tiers: TierSummary[]): TierOption[] =>
    tiers
        .filter((t): t is TierSummary & {id: string; name: string} => !!t.id && !!t.name)
        .map((t) => ({value: t.id, prompt: t.name}));

export function useMembershipFeeTierOptions(): TierOption[] {
    const {items} = useHalCollection<TierSummary, TierOption>('/api/membership-fee-tiers', {
        staleTime: 5 * 60 * 1000,
        select: toTierOptions,
    });

    return items;
}
