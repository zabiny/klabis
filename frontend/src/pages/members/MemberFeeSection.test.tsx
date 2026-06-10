import '@testing-library/jest-dom';
import {render, screen} from '@testing-library/react';
import {MemoryRouter} from 'react-router-dom';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {vi} from 'vitest';
import {MemberFeeSection} from './MemberFeeSection';
import {useAuthorizedQuery} from '../../hooks/useAuthorizedFetch';

vi.mock('../../hooks/useAuthorizedFetch', () => ({
    useAuthorizedQuery: vi.fn(),
    useAuthorizedMutation: vi.fn().mockReturnValue({
        mutate: vi.fn(),
        isPending: false,
        error: null,
    }),
}));

const renderSection = (feeSummaryHref = '/api/members/member-1/fee-summary/2026') => {
    const queryClient = new QueryClient({defaultOptions: {queries: {retry: false, gcTime: 0}}});
    return render(
        <QueryClientProvider client={queryClient}>
            <MemoryRouter initialEntries={['/members/member-1']}>
                <MemberFeeSection feeSummaryHref={feeSummaryHref} memberId="member-1"/>
            </MemoryRouter>
        </QueryClientProvider>
    );
};

const mockFeeSummary = (data: unknown) => {
    vi.mocked(useAuthorizedQuery).mockReturnValue({
        data,
        isLoading: false,
        error: null,
        status: 'success',
    } as ReturnType<typeof useAuthorizedQuery>);
};

describe('MemberFeeSection', () => {
    beforeEach(() => {
        vi.clearAllMocks();
    });

    describe('loading state', () => {
        it('shows loading indicator while fetching', () => {
            vi.mocked(useAuthorizedQuery).mockReturnValue({
                data: undefined,
                isLoading: true,
                error: null,
                status: 'pending',
            } as ReturnType<typeof useAuthorizedQuery>);

            renderSection();

            expect(screen.getByText(/Načítání/i)).toBeInTheDocument();
        });
    });

    describe('section heading', () => {
        it('renders section heading "Členský příspěvek"', () => {
            mockFeeSummary(null);
            renderSection();
            expect(screen.getByRole('heading', {name: /Členský příspěvek/i})).toBeInTheDocument();
        });
    });

    describe('votingOpen is true, no current choice', () => {
        it('shows call-to-action message when voting open and no choice made', () => {
            mockFeeSummary({
                currentGroup: null,
                votingOpen: true,
                recommendedLevelId: null,
                _links: {self: {href: '/api/members/member-1/fee-summary/2026'}},
            });

            renderSection();

            expect(screen.getByText(/Probíhá volba/i)).toBeInTheDocument();
        });

        it('renders link to fee choice page when voting is open and no choice made', () => {
            mockFeeSummary({
                currentGroup: null,
                votingOpen: true,
                recommendedLevelId: null,
                _links: {self: {href: '/api/members/member-1/fee-summary/2026'}},
            });

            renderSection();

            const link = screen.getByRole('link', {name: /Zvolit tier/i});
            expect(link).toBeInTheDocument();
            expect(link).toHaveAttribute('href', expect.stringContaining('/members/member-1/fee-choice'));
        });
    });

    describe('votingOpen is true, choice exists', () => {
        it('shows current choice name when voting open and choice exists', () => {
            mockFeeSummary({
                currentGroup: {id: 'group-1', name: 'Základ', yearlyFee: 500},
                votingOpen: true,
                recommendedLevelId: null,
                _links: {self: {href: '/api/members/member-1/fee-summary/2026'}},
            });

            renderSection();

            expect(screen.getByText('Základ')).toBeInTheDocument();
        });

        it('shows "Změnit tier" link when voting open and choice exists', () => {
            mockFeeSummary({
                currentGroup: {id: 'group-1', name: 'Základ', yearlyFee: 500},
                votingOpen: true,
                recommendedLevelId: null,
                _links: {self: {href: '/api/members/member-1/fee-summary/2026'}},
            });

            renderSection();

            const link = screen.getByRole('link', {name: /Změnit tier/i});
            expect(link).toBeInTheDocument();
            expect(link).toHaveAttribute('href', expect.stringContaining('/members/member-1/fee-choice'));
        });

        it('shows yearly fee when choice exists', () => {
            mockFeeSummary({
                currentGroup: {id: 'group-1', name: 'Základ', yearlyFee: 500},
                votingOpen: true,
                recommendedLevelId: null,
                _links: {self: {href: '/api/members/member-1/fee-summary/2026'}},
            });

            renderSection();

            expect(screen.getByText(/500/)).toBeInTheDocument();
        });
    });

    describe('votingOpen is false, choice exists', () => {
        it('shows current choice name in read-only mode', () => {
            mockFeeSummary({
                currentGroup: {id: 'group-1', name: 'Základ', yearlyFee: 500},
                votingOpen: false,
                recommendedLevelId: null,
                _links: {self: {href: '/api/members/member-1/fee-summary/2026'}},
            });

            renderSection();

            expect(screen.getByText('Základ')).toBeInTheDocument();
        });

        it('shows locked info text when voting closed and choice exists', () => {
            mockFeeSummary({
                currentGroup: {id: 'group-1', name: 'Základ', yearlyFee: 500},
                votingOpen: false,
                recommendedLevelId: null,
                _links: {self: {href: '/api/members/member-1/fee-summary/2026'}},
            });

            renderSection();

            expect(screen.getByText(/Volba je uzamčena/i)).toBeInTheDocument();
        });

        it('does not show change link when voting is closed', () => {
            mockFeeSummary({
                currentGroup: {id: 'group-1', name: 'Základ', yearlyFee: 500},
                votingOpen: false,
                recommendedLevelId: null,
                _links: {self: {href: '/api/members/member-1/fee-summary/2026'}},
            });

            renderSection();

            expect(screen.queryByRole('link', {name: /Změnit tier/i})).not.toBeInTheDocument();
        });
    });

    describe('votingOpen is false, no choice made', () => {
        it('shows warning when deadline has expired with no choice', () => {
            mockFeeSummary({
                currentGroup: null,
                votingOpen: false,
                recommendedLevelId: null,
                _links: {self: {href: '/api/members/member-1/fee-summary/2026'}},
            });

            renderSection();

            expect(screen.getByText(/Lhůta pro výběr uplynula/i)).toBeInTheDocument();
        });

        it('does not show choose link when voting closed and no choice', () => {
            mockFeeSummary({
                currentGroup: null,
                votingOpen: false,
                recommendedLevelId: null,
                _links: {self: {href: '/api/members/member-1/fee-summary/2026'}},
            });

            renderSection();

            expect(screen.queryByRole('link', {name: /Zvolit tier/i})).not.toBeInTheDocument();
        });
    });

    describe('history link', () => {
        it('shows history link when fee-history link exists in summary', () => {
            mockFeeSummary({
                currentGroup: null,
                votingOpen: true,
                recommendedLevelId: null,
                _links: {
                    self: {href: '/api/members/member-1/fee-summary/2026'},
                    feeHistory: {href: '/api/members/member-1/fee-history'},
                },
            });

            renderSection();

            expect(screen.getByRole('link', {name: /Zobrazit historii příspěvků/i})).toBeInTheDocument();
        });

        it('does not show history link when fee-history link absent', () => {
            mockFeeSummary({
                currentGroup: null,
                votingOpen: true,
                recommendedLevelId: null,
                _links: {
                    self: {href: '/api/members/member-1/fee-summary/2026'},
                },
            });

            renderSection();

            expect(screen.queryByRole('link', {name: /Zobrazit historii příspěvků/i})).not.toBeInTheDocument();
        });
    });
});
