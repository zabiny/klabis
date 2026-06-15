import '@testing-library/jest-dom';
import {render, screen} from '@testing-library/react';
import {MemoryRouter, Route, Routes} from 'react-router-dom';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {vi} from 'vitest';
import {MemberFeeChoicePage} from './MemberFeeChoicePage';
import {useAuthorizedQuery, useAuthorizedMutation} from '../../hooks/useAuthorizedFetch';

vi.mock('../../hooks/useAuthorizedFetch', () => ({
    useAuthorizedQuery: vi.fn(),
    useAuthorizedMutation: vi.fn(),
}));

const mockMutation = (overrides?: Partial<ReturnType<typeof useAuthorizedMutation>>) => {
    vi.mocked(useAuthorizedMutation).mockReturnValue({
        mutate: vi.fn(),
        mutateAsync: vi.fn().mockResolvedValue(undefined),
        isPending: false,
        isSuccess: false,
        isIdle: true,
        isError: false,
        data: undefined,
        error: null,
        reset: vi.fn(),
        status: 'idle',
        variables: undefined,
        context: undefined,
        failureCount: 0,
        failureReason: null,
        submittedAt: 0,
        ...overrides,
    } as unknown as ReturnType<typeof useAuthorizedMutation>);
};

const renderPage = (memberId = 'member-1', year?: string) => {
    const path = year
        ? `/members/${memberId}/fee-choice/${year}`
        : `/members/${memberId}/fee-choice`;
    const queryClient = new QueryClient({defaultOptions: {queries: {retry: false, gcTime: 0}}});
    return render(
        <QueryClientProvider client={queryClient}>
            <MemoryRouter initialEntries={[path]}>
                <Routes>
                    <Route path="/members/:memberId/fee-choice/:year" element={<MemberFeeChoicePage/>}/>
                    <Route path="/members/:memberId/fee-choice" element={<MemberFeeChoicePage/>}/>
                </Routes>
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

describe('MemberFeeChoicePage', () => {
    beforeEach(() => {
        vi.clearAllMocks();
        mockMutation();
    });

    describe('loading state', () => {
        it('shows skeleton when data is loading', () => {
            vi.mocked(useAuthorizedQuery).mockReturnValue({
                data: undefined,
                isLoading: true,
                error: null,
                status: 'pending',
            } as ReturnType<typeof useAuthorizedQuery>);

            renderPage();

            expect(document.querySelector('.animate-pulse')).toBeInTheDocument();
        });
    });

    describe('page title', () => {
        it('renders page heading with year from URL param', () => {
            mockFeeSummary({
                currentGroup: null,
                votingOpen: true,
                recommendedLevelId: null,
                _links: {self: {href: '/api/members/member-1/fee-summary/2025'}},
                _templates: {},
            });

            renderPage('member-1', '2025');

            expect(screen.getByRole('heading', {level: 1})).toHaveTextContent(/2025/);
        });
    });

    describe('voting closed with choice', () => {
        it('shows current group name in read-only mode when voting closed', () => {
            mockFeeSummary({
                currentGroup: {id: 'group-1', name: 'Základ', yearlyFee: 500},
                votingOpen: false,
                recommendedLevelId: null,
                _links: {self: {href: '/api/members/member-1/fee-summary/2026'}},
                _templates: {},
            });

            renderPage();

            expect(screen.getByText('Základ')).toBeInTheDocument();
        });

        it('shows lock info message when voting closed and choice exists', () => {
            mockFeeSummary({
                currentGroup: {id: 'group-1', name: 'Základ', yearlyFee: 500},
                votingOpen: false,
                recommendedLevelId: null,
                _links: {self: {href: '/api/members/member-1/fee-summary/2026'}},
                _templates: {},
            });

            renderPage();

            expect(screen.getByText(/Volba je uzamčena/i)).toBeInTheDocument();
        });
    });

    describe('voting closed without choice', () => {
        it('shows "Lhůta pro výběr uplynula" message', () => {
            mockFeeSummary({
                currentGroup: null,
                votingOpen: false,
                recommendedLevelId: null,
                _links: {self: {href: '/api/members/member-1/fee-summary/2026'}},
                _templates: {},
            });

            renderPage();

            expect(screen.getByText(/Lhůta pro výběr uplynula/i)).toBeInTheDocument();
        });
    });

    describe('voting open with chooseTier template', () => {
        it('shows choose tier form when chooseTier template exists', () => {
            mockFeeSummary({
                currentGroup: null,
                votingOpen: true,
                recommendedLevelId: null,
                _links: {self: {href: '/api/members/member-1/fee-summary/2026'}},
                _templates: {
                    chooseTier: {
                        method: 'POST',
                        target: '/api/members/member-1/fee-choice/2026',
                        properties: [
                            {
                                name: 'membershipFeeGroupId',
                                type: 'text',
                                prompt: 'Skupina',
                                options: {
                                    inline: [
                                        {value: 'group-1', prompt: 'Základ - 500 Kč/rok'},
                                        {value: 'group-2', prompt: 'Rozšířené - 1000 Kč/rok'},
                                    ],
                                },
                            },
                        ],
                    },
                },
            });

            renderPage();

            expect(screen.getByText(/Zvolit tier/i)).toBeInTheDocument();
        });

        it('shows current choice and change option when voting open and choice exists', () => {
            mockFeeSummary({
                currentGroup: {id: 'group-1', name: 'Základ', yearlyFee: 500},
                votingOpen: true,
                recommendedLevelId: null,
                _links: {self: {href: '/api/members/member-1/fee-summary/2026'}},
                _templates: {
                    chooseTier: {
                        method: 'POST',
                        target: '/api/members/member-1/fee-choice/2026',
                        properties: [
                            {
                                name: 'membershipFeeGroupId',
                                type: 'text',
                                prompt: 'Skupina',
                                options: {
                                    inline: [{value: 'group-1', prompt: 'Základ - 500 Kč/rok'}],
                                },
                            },
                        ],
                    },
                },
            });

            renderPage();

            expect(screen.getByText('Základ')).toBeInTheDocument();
        });
    });
});
