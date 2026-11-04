/* eslint-disable react-refresh/only-export-components */
import type {ReactElement} from 'react';
import {render} from '@testing-library/react';
import {MemoryRouter, useLocation} from 'react-router-dom';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {vi} from 'vitest';
import {useHalPageData} from '../hooks/useHalPageData';
import {ToastProvider} from '../contexts/ToastContext.tsx';
import {useToast} from '../contexts/toastContext.ts';
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

const ToastProbe = () => {
    const {toasts} = useToast();
    return <ul>{toasts.map(t => <li key={t.id} data-testid="toast">{t.message}</li>)}</ul>;
};

const LocationProbe = () => <span data-testid="location">{useLocation().pathname}</span>;

export const renderDetailPage = (
    page: ReactElement,
    resourceData: HalResponse | null,
    options: {pathname: string; refetch?: () => Promise<void>; isLoading?: boolean; error?: Error | null},
) => {
    vi.mocked(useHalPageData).mockReturnValue({
        resourceData,
        isLoading: options.isLoading ?? false,
        error: options.error ?? null,
        isAdmin: false,
        route: {
            pathname: options.pathname,
            navigateToResource: vi.fn(),
            refetch: options.refetch ?? (async () => {}),
            queryState: 'success' as const,
            getResourceLink: vi.fn(),
        },
        actions: {handleNavigateToItem: vi.fn()},
        getLinks: vi.fn(() => undefined),
        getTemplates: vi.fn(() => undefined),
        hasEmbedded: vi.fn(() => false),
        getEmbeddedItems: vi.fn(() => []),
        isCollection: vi.fn(() => false),
        hasLink: vi.fn(() => false),
        hasTemplate: vi.fn(() => false),
        hasForms: vi.fn(() => false),
        getPageMetadata: vi.fn(() => undefined),
    } as unknown as ReturnType<typeof useHalPageData>);
    const queryClient = new QueryClient({defaultOptions: {queries: {retry: false, gcTime: 0}}});
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
