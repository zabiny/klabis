import '@testing-library/jest-dom';
import {render, screen, within} from '@testing-library/react';
import {MemoryRouter, Route, Routes} from 'react-router-dom';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {vi} from 'vitest';
import {useHalPageData} from '../../hooks/useHalPageData';
import {useAuthorizedQuery} from '../../hooks/useAuthorizedFetch';
import {MemberFeeHistoryPage} from './MemberFeeHistoryPage';
import type {GetFeeHistoryResource} from '../../api';

vi.mock('../../hooks/useHalPageData', () => ({
    useHalPageData: vi.fn(),
}));

vi.mock('../../hooks/useAuthorizedFetch', () => ({
    useAuthorizedQuery: vi.fn(),
    useAuthorizedMutation: vi.fn().mockReturnValue({mutate: vi.fn(), isPending: false, error: null}),
}));

const memberId = '123e4567-e89b-12d3-a456-426614174000';

const buildHistory = (overrides: Partial<GetFeeHistoryResource> = {}): GetFeeHistoryResource => ({
    assignments: [
        {year: 2026, groupId: 'g2', groupName: 'Základní', joinedAt: '2026-01-10', source: 'MEMBER_CHOICE'},
        {year: 2025, groupId: 'g1', groupName: 'Snížený', joinedAt: '2025-01-05', source: 'ADMIN_ASSIGNMENT'},
    ],
    _links: {
        self: {href: `/api/members/${memberId}/fee-history`},
        member: {href: `/api/members/${memberId}`},
    },
    ...overrides,
} as GetFeeHistoryResource);

const renderPage = (resourceData: GetFeeHistoryResource | null, extra: Record<string, unknown> = {}) => {
    vi.mocked(useHalPageData).mockReturnValue({
        resourceData,
        isLoading: false,
        error: null,
        navigateToResource: vi.fn(),
        refetch: async () => {},
        getResourceLink: vi.fn(),
        ...extra,
    } as unknown as ReturnType<typeof useHalPageData>);
    const queryClient = new QueryClient({defaultOptions: {queries: {retry: false, gcTime: 0}}});
    return render(
        <QueryClientProvider client={queryClient}>
            <MemoryRouter initialEntries={[`/members/${memberId}/fee-history`]}>
                <Routes>
                    <Route path="/members/:memberId/fee-history" element={<MemberFeeHistoryPage/>}/>
                </Routes>
            </MemoryRouter>
        </QueryClientProvider>
    );
};

describe('MemberFeeHistoryPage', () => {
    beforeEach(() => {
        vi.clearAllMocks();
        vi.mocked(useAuthorizedQuery).mockReturnValue({
            data: {currentGroup: null, votingOpen: false, _links: {self: {href: `/api/members/${memberId}/fee-summary/2026`}}},
            isLoading: false,
            error: null,
            status: 'success',
        } as ReturnType<typeof useAuthorizedQuery>);
    });

    it('renders page title', () => {
        renderPage(buildHistory());
        expect(screen.getByRole('heading', {level: 1, name: 'Členské příspěvky'})).toBeInTheDocument();
    });

    it('lists history rows newest first with year, level and source label', () => {
        renderPage(buildHistory());

        const rows = screen.getAllByRole('row').slice(1);
        expect(rows).toHaveLength(2);
        expect(within(rows[0]).getByText('2026')).toBeInTheDocument();
        expect(within(rows[0]).getByText('Základní')).toBeInTheDocument();
        expect(within(rows[0]).getByText('Vlastní volba')).toBeInTheDocument();
        expect(within(rows[1]).getByText('2025')).toBeInTheDocument();
        expect(within(rows[1]).getByText('Snížený')).toBeInTheDocument();
        expect(within(rows[1]).getByText('Přiřazeno adminem')).toBeInTheDocument();
    });

    it('shows empty-state message when there are no assignments', () => {
        renderPage(buildHistory({assignments: []}));
        expect(screen.getByText(/žádnou historii/i)).toBeInTheDocument();
    });

    it('back link targets the member taken from the member link', () => {
        renderPage(buildHistory());
        expect(screen.getByRole('link', {name: /zpět na detail člena/i})).toHaveAttribute('href', `/members/${memberId}`);
    });

    it('shows current level section when the feeSummary link is present', () => {
        renderPage(buildHistory({
            _links: {
                self: {href: `/api/members/${memberId}/fee-history`},
                member: {href: `/api/members/${memberId}`},
                feeSummary: {href: `/api/members/${memberId}/fee-summary/2026`},
            },
        }));
        expect(screen.getByRole('heading', {name: /Členský příspěvek/i})).toBeInTheDocument();
        expect(useAuthorizedQuery).toHaveBeenCalledWith(`/api/members/${memberId}/fee-summary/2026`);
    });

    it('does not show current level section without the feeSummary link', () => {
        renderPage(buildHistory());
        expect(screen.queryByRole('heading', {name: /Členský příspěvek/i})).not.toBeInTheDocument();
    });
});
