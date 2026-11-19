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
                recommendedGroupId: null,
                _templates: {chooseFeeType: {method: 'POST', target: '/api/members/member-1/fee-choice/2026'}},
                _links: {self: {href: '/api/members/member-1/fee-summary/2026'}},
            });

            renderSection();

            expect(screen.getByText(/Probíhá volba/i)).toBeInTheDocument();
        });

        it('renders link to fee choice page including year derived from self link', () => {
            mockFeeSummary({
                currentGroup: null,
                votingOpen: true,
                recommendedGroupId: null,
                _templates: {chooseFeeType: {method: 'POST', target: '/api/members/member-1/fee-choice/2026'}},
                _links: {self: {href: '/api/members/member-1/fee-summary/2027'}},
            });

            renderSection();

            const link = screen.getByRole('link', {name: /Zvolit tier/i});
            expect(link).toBeInTheDocument();
            expect(link).toHaveAttribute('href', '/members/member-1/fee-choice/2027');
        });

        it('uses year from self link, not current calendar year', () => {
            mockFeeSummary({
                currentGroup: null,
                votingOpen: true,
                recommendedGroupId: null,
                _templates: {chooseFeeType: {method: 'POST', target: '/api/members/member-1/fee-choice/2026'}},
                _links: {self: {href: '/api/members/member-1/fee-summary/2099'}},
            });

            renderSection();

            const link = screen.getByRole('link', {name: /Zvolit tier/i});
            expect(link).toHaveAttribute('href', '/members/member-1/fee-choice/2099');
        });
    });

    describe('votingOpen is true, choice exists', () => {
        it('shows current choice name when voting open and choice exists', () => {
            mockFeeSummary({
                currentGroup: {id: 'group-1', name: 'Základ', yearlyFee: 500},
                votingOpen: true,
                recommendedGroupId: null,
                _templates: {chooseFeeType: {method: 'POST', target: '/api/members/member-1/fee-choice/2026'}},
                _links: {self: {href: '/api/members/member-1/fee-summary/2026'}},
            });

            renderSection();

            expect(screen.getByText('Základ')).toBeInTheDocument();
        });

        it('shows "Změnit tier" link with year derived from self link', () => {
            mockFeeSummary({
                currentGroup: {id: 'group-1', name: 'Základ', yearlyFee: 500},
                votingOpen: true,
                recommendedGroupId: null,
                _templates: {chooseFeeType: {method: 'POST', target: '/api/members/member-1/fee-choice/2026'}},
                _links: {self: {href: '/api/members/member-1/fee-summary/2027'}},
            });

            renderSection();

            const link = screen.getByRole('link', {name: /Změnit tier/i});
            expect(link).toBeInTheDocument();
            expect(link).toHaveAttribute('href', '/members/member-1/fee-choice/2027');
        });

        it('shows yearly fee when choice exists', () => {
            mockFeeSummary({
                currentGroup: {id: 'group-1', name: 'Základ', yearlyFee: 500},
                votingOpen: true,
                recommendedGroupId: null,
                _templates: {chooseFeeType: {method: 'POST', target: '/api/members/member-1/fee-choice/2026'}},
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
                recommendedGroupId: null,
                _links: {self: {href: '/api/members/member-1/fee-summary/2026'}},
            });

            renderSection();

            expect(screen.getByText('Základ')).toBeInTheDocument();
        });

        it('shows locked info text when voting closed and choice exists', () => {
            mockFeeSummary({
                currentGroup: {id: 'group-1', name: 'Základ', yearlyFee: 500},
                votingOpen: false,
                recommendedGroupId: null,
                _links: {self: {href: '/api/members/member-1/fee-summary/2026'}},
            });

            renderSection();

            expect(screen.getByText(/Volba je uzamčena/i)).toBeInTheDocument();
        });

        it('does not show change link when voting is closed', () => {
            mockFeeSummary({
                currentGroup: {id: 'group-1', name: 'Základ', yearlyFee: 500},
                votingOpen: false,
                recommendedGroupId: null,
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
                recommendedGroupId: null,
                _links: {self: {href: '/api/members/member-1/fee-summary/2026'}},
            });

            renderSection();

            expect(screen.getByText(/Lhůta pro výběr uplynula/i)).toBeInTheDocument();
        });

        it('does not show choose link when voting closed and no choice', () => {
            mockFeeSummary({
                currentGroup: null,
                votingOpen: false,
                recommendedGroupId: null,
                _links: {self: {href: '/api/members/member-1/fee-summary/2026'}},
            });

            renderSection();

            expect(screen.queryByRole('link', {name: /Zvolit tier/i})).not.toBeInTheDocument();
        });
    });

    describe('votingOpen is true, chooseFeeType template absent', () => {
        it('hides choose link and prompt when there is no choice', () => {
            mockFeeSummary({
                currentGroup: null,
                votingOpen: true,
                recommendedGroupId: null,
                _links: {self: {href: '/api/members/member-1/fee-summary/2026'}},
            });

            renderSection();

            expect(screen.queryByRole('link')).not.toBeInTheDocument();
            expect(screen.queryByText(/Probíhá volba/i)).not.toBeInTheDocument();
        });

        it('shows current level read-only without change link or prompt', () => {
            mockFeeSummary({
                currentGroup: {id: 'group-1', name: 'Základ', yearlyFee: 500},
                votingOpen: true,
                recommendedGroupId: null,
                _links: {self: {href: '/api/members/member-1/fee-summary/2026'}},
            });

            renderSection();

            expect(screen.getByText('Základ')).toBeInTheDocument();
            expect(screen.queryByRole('link')).not.toBeInTheDocument();
            expect(screen.queryByText(/Volbu je možné změnit/i)).not.toBeInTheDocument();
        });
    });

    describe('viewer of another member summary (administrator or guardian)', () => {
        it('shows current level and status when summary carries only a self link', () => {
            mockFeeSummary({
                currentGroup: {id: 'group-1', name: 'Základ', yearlyFee: 500},
                votingOpen: false,
                recommendedGroupId: null,
                _links: {self: {href: '/api/members/member-1/fee-summary/2026'}},
            });

            renderSection();

            expect(screen.getByRole('heading', {name: /Členský příspěvek/i})).toBeInTheDocument();
            expect(screen.getByText('Základ')).toBeInTheDocument();
            expect(screen.getByText(/500/)).toBeInTheDocument();
            expect(screen.queryByRole('link')).not.toBeInTheDocument();
        });
    });
});
