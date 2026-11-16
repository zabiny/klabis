import type {components} from '../api/klabisApi';
import type {HalResourceLinks} from '../api';
import {toHref} from '../api/hateoas.ts';
import {useHalCollection} from './useHalCollection.ts';

export type LegalGuardianRow = components['schemas']['EntityModelLegalGuardianGroupGuardianResponse'];

/** Loads guardians behind a `legalGuardians` link; without the link there is nothing to load. */
export function useLegalGuardians(guardiansLink?: HalResourceLinks): {guardians: LegalGuardianRow[]; isLoading: boolean} {
    const href = guardiansLink ? toHref(guardiansLink) : undefined;
    const {items, isLoading} = useHalCollection<LegalGuardianRow>(href);
    return {guardians: items, isLoading};
}
