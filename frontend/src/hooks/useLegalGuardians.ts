import type {components} from '../api/klabisApi';
import type {HalResourceLinks} from '../api';
import {toHref} from '../api/hateoas.ts';
import {useAuthorizedQuery} from './useAuthorizedFetch.ts';

export type LegalGuardianRow = components['schemas']['EntityModelLegalGuardianGroupGuardianResponse'];

type GuardiansResponse = {_embedded?: {legalGuardianGroupGuardianResponseList?: LegalGuardianRow[]}};

/** Loads guardians behind a `legalGuardians` link; without the link there is nothing to load. */
export function useLegalGuardians(guardiansLink?: HalResourceLinks): {guardians: LegalGuardianRow[]; isLoading: boolean} {
    const href = guardiansLink ? toHref(guardiansLink) : undefined;
    const {data, isLoading} = useAuthorizedQuery<GuardiansResponse>(href);
    return {
        guardians: data?._embedded?.legalGuardianGroupGuardianResponseList ?? [],
        isLoading: href !== undefined && isLoading,
    };
}
