import type {ReactElement} from 'react';
import {render} from '@testing-library/react';
import {MemoryRouter} from 'react-router-dom';
import {QueryClientProvider} from '@tanstack/react-query';
import {vi} from 'vitest';
import {useHalPageData} from '../hooks/useHalPageData';
import {ToastProvider} from '../contexts/ToastContext.tsx';
import {createTestQueryClient, LocationProbe, ToastProbe} from './halDialogTest.tsx';
import {mockHalFormsTemplate} from './halData';
import type {HalFormsTemplate, HalResponse} from '../api';

export const MEMBER_OPTIONS = [
    {value: 'member-1', prompt: 'Petr Svoboda'},
    {value: 'member-2', prompt: 'Jana Nováková'},
    {value: 'member-3', prompt: 'Karel Dvořák'},
];

export const memberOptionsResponse = (url: string): unknown | undefined =>
    url.includes('/members/options') ? MEMBER_OPTIONS : undefined;

export const memberPickerTemplate = (overrides?: Partial<HalFormsTemplate>): HalFormsTemplate => mockHalFormsTemplate({
    properties: [{
        name: 'memberId',
        prompt: 'Člen',
        type: 'MemberId',
        required: true,
        options: {link: {href: '/api/members/options'}},
    }],
    ...overrides,
});

export const confirmTemplate = (overrides?: Partial<HalFormsTemplate>): HalFormsTemplate => mockHalFormsTemplate({
    method: 'DELETE',
    target: undefined,
    properties: [],
    ...overrides,
});

export const renderDetailPage = (
    page: ReactElement,
    resourceData: HalResponse | null,
    options: {pathname: string; refetch?: () => Promise<void>; isLoading?: boolean; error?: Error | null},
) => {
    vi.mocked(useHalPageData).mockReturnValue({
        resourceData,
        isLoading: options.isLoading ?? false,
        error: options.error ?? null,
        navigateToResource: vi.fn(),
        refetch: options.refetch ?? (async () => {}),
        getResourceLink: vi.fn(),
    } as unknown as ReturnType<typeof useHalPageData>);
    const queryClient = createTestQueryClient();
    return render(
        <QueryClientProvider client={queryClient}>
            <MemoryRouter initialEntries={[options.pathname]}>
                <ToastProvider>
                    {page}
                    <ToastProbe/>
                    <LocationProbe/>
                </ToastProvider>
            </MemoryRouter>
        </QueryClientProvider>
    );
};
