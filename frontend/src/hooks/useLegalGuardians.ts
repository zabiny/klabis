import type {components} from '../api/klabisApi';
import type {HalResourceLinks} from '../api';
import {toHref} from '../api/hateoas.ts';
import {normalizeKlabisApiPath} from '../utils/halFormsUtils.ts';
import {useAuthorizedQuery} from './useAuthorizedFetch.ts';

export type LegalGuardianRow = components['schemas']['EntityModelLegalGuardianGroupGuardianResponse'];

type GuardiansResponse = {_embedded?: {legalGuardianGroupGuardianResponseList?: LegalGuardianRow[]}};

/** Loads guardians behind a `legalGuardians` link; without the link there is nothing to load. */
export function useLegalGuardians(guardiansLink?: HalResourceLinks): {guardians: LegalGuardianRow[]; isLoading: boolean} {
    const href = guardiansLink ? toHref(guardiansLink).replace(/\{[^}]*\}/g, '') : '';
    const {data, isLoading} = useAuthorizedQuery<GuardiansResponse>(normalizeKlabisApiPath(href), {
        enabled: href !== '',
    });
    return {
        guardians: data?._embedded?.legalGuardianGroupGuardianResponseList ?? [],
        isLoading: href !== '' && isLoading,
    };
}
