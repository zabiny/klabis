import {useQuery} from '@tanstack/react-query';
import {authorizedFetch} from '../api/authorizedFetch';
import {normalizeKlabisApiPath} from '../utils/halFormsUtils';

const STALE_TIME = 10 * 60 * 1000;
const GC_TIME = 10 * 60 * 1000;

export interface UseHalFormGetAvailabilityReturn {
    /**
     * True if the Allow response header on the target URL advertises GET.
     * Undefined while loading or when the probe has not been enabled.
     * Undefined on error — caller decides how to surface failures.
     */
    isGetAllowed: boolean | undefined;
    isLoading: boolean;
    error: Error | null;
}

/**
 * Probes whether a target URL supports GET by sending an OPTIONS request and
 * parsing the Allow response header. Results are cached for 10 minutes so that
 * repeated form opens against the same target do not re-probe.
 *
 * @param targetUrl - The URL to probe. Must be non-empty for the probe to fire.
 * @param enabled   - When false the probe is skipped entirely (e.g. no target differs from current resource).
 */
export function useHalFormGetAvailability(
    targetUrl: string | undefined,
    enabled: boolean
): UseHalFormGetAvailabilityReturn {
    const normalizedUrl = targetUrl ? normalizeKlabisApiPath(targetUrl) : undefined;
    const shouldProbe = enabled && !!normalizedUrl;

    const {data, isLoading, error} = useQuery<boolean>({
        queryKey: ['hal-options', normalizedUrl],
        queryFn: async () => {
            const response = await authorizedFetch(normalizedUrl!, {method: 'OPTIONS'});
            const allowHeader = response.headers.get('Allow');
            if (!allowHeader) {
                // Conservative: no Allow header on a 2xx response is treated as GET not supported
                // rather than optimistically assuming it is, to avoid unexpected GETs on write-only endpoints.
                return false;
            }
            return allowHeader
                .split(',')
                .map((m) => m.trim().toUpperCase())
                .includes('GET');
        },
        enabled: shouldProbe,
        staleTime: STALE_TIME,
        gcTime: GC_TIME,
        retry: false,
    });

    return {
        isGetAllowed: data,
        isLoading,
        error: error as Error | null,
    };
}
