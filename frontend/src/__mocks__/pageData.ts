import {vi} from 'vitest';
import type {HalResponse} from '../api';
import type {UseHalPageDataReturn} from '../hooks/useHalPageData';

export const makePageData = (
    overrides: Partial<UseHalPageDataReturn<HalResponse>> = {},
): UseHalPageDataReturn<HalResponse> => ({
    resourceData: null,
    isLoading: false,
    error: null,
    queryState: 'success',
    navigateToResource: vi.fn(),
    refetch: vi.fn(),
    getResourceLink: vi.fn(),
    ...overrides,
});
