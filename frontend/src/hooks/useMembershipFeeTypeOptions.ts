import type {components} from '../api/klabisApi';
import {useHalCollection} from './useHalCollection.ts';

type TypeSummary = components['schemas']['EntityModelMembershipFeeTypeSummaryResponse'];

export interface TypeOption {
    value: string;
    prompt: string;
}

const toTypeOptions = (types: TypeSummary[]): TypeOption[] =>
    types
        .filter((t): t is TypeSummary & {id: string; name: string} => !!t.id && !!t.name)
        .map((t) => ({value: t.id, prompt: t.name}));

export function useMembershipFeeTypeOptions(): TypeOption[] {
    const {items} = useHalCollection<TypeSummary, TypeOption>('/api/membership-fee-types', {
        staleTime: 5 * 60 * 1000,
        select: toTypeOptions,
    });

    return items;
}
