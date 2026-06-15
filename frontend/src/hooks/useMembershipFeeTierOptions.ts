import {useAuthorizedQuery} from './useAuthorizedFetch.ts';
import type {components} from '../api/klabisApi';

type TierSummary = components['schemas']['EntityModelMembershipFeeTierSummaryResponse'];

interface TierOptionsResponse {
    _embedded?: {
        membershipFeeTierSummaryResponseList?: TierSummary[];
    };
}

export interface TierOption {
    value: string;
    prompt: string;
}

export function useMembershipFeeTierOptions(): TierOption[] {
    const {data} = useAuthorizedQuery<TierOptionsResponse>('/api/membership-fee-tiers', {
        staleTime: 5 * 60 * 1000,
    });

    const tiers = data?._embedded?.membershipFeeTierSummaryResponseList ?? [];

    return tiers
        .filter((t): t is TierSummary & {id: string; name: string} => !!t.id && !!t.name)
        .map((t) => ({value: t.id, prompt: t.name}));
}
