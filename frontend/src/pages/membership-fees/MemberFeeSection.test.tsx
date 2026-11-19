import '@testing-library/jest-dom';
import {act, fireEvent, render, screen} from '@testing-library/react';
import {MemoryRouter} from 'react-router-dom';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {vi} from 'vitest';
import {MemberFeeSection} from './MemberFeeSection';
import {useAuthorizedMutation, useAuthorizedQuery} from '../../hooks/useAuthorizedFetch';

vi.mock('../../hooks/useAuthorizedFetch', () => ({
    useAuthorizedQuery: vi.fn(),
    useAuthorizedMutation: vi.fn(),
}));

const mutate = vi.fn();
const mockMutation = () => {
    vi.mocked(useAuthorizedMutation).mockReturnValue({
        mutate,
        isPending: false,
        error: null,
    } as unknown as ReturnType<typeof useAuthorizedMutation>);
};

const renderSection = (feeSummaryHref = '/api/members/member-1/fee-summary') => {
    const queryClient = new QueryClient({defaultOptions: {queries: {retry: false, gcTime: 0}}});
    return render(
        <QueryClientProvider client={queryClient}>
            <MemoryRouter initialEntries={['/members/member-1']}>
                <MemberFeeSection feeSummaryHref={feeSummaryHref}/>
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
        mockMutation();
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
                _links: {self: {href: '/api/members/member-1/fee-summary'}},
            });

            renderSection();

            expect(screen.getByText(/Probíhá volba/i)).toBeInTheDocument();
        });

    });

    describe('votingOpen is true, choice exists', () => {
        it('shows current choice name when voting open and choice exists', () => {
            mockFeeSummary({
                currentGroup: {id: 'group-1', name: 'Základ', yearlyFee: 500},
                votingOpen: true,
                recommendedGroupId: null,
                _templates: {chooseFeeType: {method: 'POST', target: '/api/members/member-1/fee-choice/2026'}},
                _links: {self: {href: '/api/members/member-1/fee-summary'}},
            });

            renderSection();

            expect(screen.getByText('Základ')).toBeInTheDocument();
        });

        it('shows yearly fee when choice exists', () => {
            mockFeeSummary({
                currentGroup: {id: 'group-1', name: 'Základ', yearlyFee: 500},
                votingOpen: true,
                recommendedGroupId: null,
                _templates: {chooseFeeType: {method: 'POST', target: '/api/members/member-1/fee-choice/2026'}},
                _links: {self: {href: '/api/members/member-1/fee-summary'}},
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
                _links: {self: {href: '/api/members/member-1/fee-summary'}},
            });

            renderSection();

            expect(screen.getByText('Základ')).toBeInTheDocument();
        });

        it('shows locked info text when voting closed and choice exists', () => {
            mockFeeSummary({
                currentGroup: {id: 'group-1', name: 'Základ', yearlyFee: 500},
                votingOpen: false,
                recommendedGroupId: null,
                _links: {self: {href: '/api/members/member-1/fee-summary'}},
            });

            renderSection();

            expect(screen.getByText(/Volba je uzamčena/i)).toBeInTheDocument();
        });

        it('does not show change link when voting is closed', () => {
            mockFeeSummary({
                currentGroup: {id: 'group-1', name: 'Základ', yearlyFee: 500},
                votingOpen: false,
                recommendedGroupId: null,
                _links: {self: {href: '/api/members/member-1/fee-summary'}},
            });

            renderSection();

            expect(screen.queryByRole('button', {name: /Změnit tier/i})).not.toBeInTheDocument();
        });
    });

    describe('votingOpen is false, no choice made', () => {
        it('shows warning when deadline has expired with no choice', () => {
            mockFeeSummary({
                currentGroup: null,
                votingOpen: false,
                recommendedGroupId: null,
                _links: {self: {href: '/api/members/member-1/fee-summary'}},
            });

            renderSection();

            expect(screen.getByText(/Lhůta pro výběr uplynula/i)).toBeInTheDocument();
        });

        it('does not show choose link when voting closed and no choice', () => {
            mockFeeSummary({
                currentGroup: null,
                votingOpen: false,
                recommendedGroupId: null,
                _links: {self: {href: '/api/members/member-1/fee-summary'}},
            });

            renderSection();

            expect(screen.queryByRole('button', {name: /Zvolit tier/i})).not.toBeInTheDocument();
        });
    });

    describe('votingOpen is true, chooseFeeType template absent', () => {
        it('hides choose link and prompt when there is no choice', () => {
            mockFeeSummary({
                currentGroup: null,
                votingOpen: true,
                recommendedGroupId: null,
                _links: {self: {href: '/api/members/member-1/fee-summary'}},
            });

            renderSection();

            expect(screen.queryByRole('button')).not.toBeInTheDocument();
            expect(screen.queryByText(/Probíhá volba/i)).not.toBeInTheDocument();
        });

        it('shows current level read-only without change link or prompt', () => {
            mockFeeSummary({
                currentGroup: {id: 'group-1', name: 'Základ', yearlyFee: 500},
                votingOpen: true,
                recommendedGroupId: null,
                _links: {self: {href: '/api/members/member-1/fee-summary'}},
            });

            renderSection();

            expect(screen.getByText('Základ')).toBeInTheDocument();
            expect(screen.queryByRole('button')).not.toBeInTheDocument();
            expect(screen.queryByText(/Volbu je možné změnit/i)).not.toBeInTheDocument();
        });
    });

    describe('fetching', () => {
        it('fetches the summary via the supplied link href', () => {
            mockFeeSummary(null);
            renderSection('/api/members/member-1/fee-summary');
            expect(useAuthorizedQuery).toHaveBeenCalledWith('/api/members/member-1/fee-summary');
        });
    });

    describe('viewer of another member summary (administrator or guardian)', () => {
        it('shows current level and status when summary carries only a self link', () => {
            mockFeeSummary({
                currentGroup: {id: 'group-1', name: 'Základ', yearlyFee: 500},
                votingOpen: false,
                recommendedGroupId: null,
                _links: {self: {href: '/api/members/member-1/fee-summary'}},
            });

            renderSection();

            expect(screen.getByRole('heading', {name: /Členský příspěvek/i})).toBeInTheDocument();
            expect(screen.getByText('Základ')).toBeInTheDocument();
            expect(screen.getByText(/500/)).toBeInTheDocument();
            expect(screen.queryByRole('button')).not.toBeInTheDocument();
        });
    });

    describe('choosing a fee type', () => {
        const optionsHref = '/api/fee-selection-campaigns/2026/groups';
        const groups = {
            _embedded: {
                membershipFeeGroupResponseList: [
                    {id: 'group-1', name: 'Základ', yearlyFeeAmount: 500},
                    {id: 'group-2', name: 'Plný', yearlyFeeAmount: 1200},
                ],
            },
        };
        const chooseTemplate = {
            method: 'POST',
            target: '/api/members/member-1/fee-choice/2026',
            properties: [{name: 'membershipFeeGroupId', type: 'MembershipFeeGroupChoice', options: {link: {href: optionsHref}}}],
        };
        const removeTemplate = {method: 'DELETE', target: '/api/members/member-1/fee-choice/2026'};

        const mockSummaryAndGroups = (summary: unknown) => {
            vi.mocked(useAuthorizedQuery).mockImplementation(((href: string | undefined) => ({
                data: href === optionsHref ? groups : href ? summary : undefined,
                isLoading: false,
                error: null,
                status: 'success',
            })) as unknown as typeof useAuthorizedQuery);
        };

        const openSummary = (overrides: object = {}) => ({
            year: 2026,
            currentGroup: null,
            votingOpen: true,
            recommendedGroupId: 'group-2',
            _templates: {chooseFeeType: chooseTemplate},
            _links: {self: {href: '/api/members/member-1/fee-summary'}},
            ...overrides,
        });

        it('shows each fee type from the options link with name and yearly price', () => {
            mockSummaryAndGroups(openSummary());
            renderSection();

            expect(screen.getByRole('radio', {name: /Základ.*500/})).toBeInTheDocument();
            expect(screen.getByRole('radio', {name: /Plný.*1200/})).toBeInTheDocument();
        });

        it('pre-selects the recommended fee type', () => {
            mockSummaryAndGroups(openSummary());
            renderSection();

            expect(screen.getByRole('radio', {name: /Plný/})).toHaveAttribute('aria-checked', 'true');
            expect(screen.getByRole('radio', {name: /Základ/})).toHaveAttribute('aria-checked', 'false');
        });

        it('submits the chosen type to the chooseFeeType template target', () => {
            mockSummaryAndGroups(openSummary());
            renderSection();

            fireEvent.click(screen.getByRole('radio', {name: /Základ/}));
            fireEvent.click(screen.getByRole('button', {name: /Zvolit tier/i}));

            expect(mutate).toHaveBeenCalledWith({
                url: '/api/members/member-1/fee-choice/2026',
                data: {membershipFeeGroupId: 'group-1'},
            }, expect.anything());
        });

        it('submits a change to the same template target when a choice already exists', () => {
            mockSummaryAndGroups(openSummary({
                currentGroup: {id: 'group-1', name: 'Základ', yearlyFee: 500},
                recommendedGroupId: null,
                _templates: {chooseFeeType: chooseTemplate, removeChoice: removeTemplate},
            }));
            renderSection();

            fireEvent.click(screen.getByRole('radio', {name: /Plný/}));
            fireEvent.click(screen.getByRole('button', {name: /Změnit tier/i}));

            expect(mutate).toHaveBeenCalledWith({
                url: '/api/members/member-1/fee-choice/2026',
                data: {membershipFeeGroupId: 'group-2'},
            }, expect.anything());
        });

        it('withdraws the choice via the removeChoice template target', () => {
            mockSummaryAndGroups(openSummary({
                currentGroup: {id: 'group-1', name: 'Základ', yearlyFee: 500},
                _templates: {chooseFeeType: chooseTemplate, removeChoice: removeTemplate},
            }));
            renderSection();

            fireEvent.click(screen.getByRole('button', {name: /Zrušit volbu/i}));

            expect(mutate).toHaveBeenCalledWith({url: '/api/members/member-1/fee-choice/2026'}, expect.anything());
        });

        it('does not offer withdraw without a removeChoice template', () => {
            mockSummaryAndGroups(openSummary({
                currentGroup: {id: 'group-1', name: 'Základ', yearlyFee: 500},
            }));
            renderSection();

            expect(screen.queryByRole('button', {name: /Zrušit volbu/i})).not.toBeInTheDocument();
        });

        it('reverts the selection to the recommended type after a successful withdraw', () => {
            mockSummaryAndGroups(openSummary({
                currentGroup: {id: 'group-1', name: 'Základ', yearlyFee: 500},
                _templates: {chooseFeeType: chooseTemplate, removeChoice: removeTemplate},
            }));
            renderSection();

            fireEvent.click(screen.getByRole('radio', {name: /Základ/}));
            expect(screen.getByRole('radio', {name: /Základ/})).toHaveAttribute('aria-checked', 'true');

            fireEvent.click(screen.getByRole('button', {name: /Zrušit volbu/i}));
            act(() => {
                (mutate.mock.calls[0][1] as {onSuccess: () => void}).onSuccess();
            });

            expect(screen.getByRole('radio', {name: /Plný/})).toHaveAttribute('aria-checked', 'true');
        });

        it('does not pre-select a recommended type that is not among the offered options', () => {
            mockSummaryAndGroups(openSummary({recommendedGroupId: 'unknown'}));
            renderSection();

            expect(screen.getAllByRole('radio').every(r => r.getAttribute('aria-checked') === 'false')).toBe(true);
            expect(screen.getByRole('button', {name: /Zvolit tier/i})).toBeDisabled();
        });

        it('shows an error and keeps choose disabled when the options link is missing', () => {
            mockSummaryAndGroups(openSummary({
                _templates: {chooseFeeType: {...chooseTemplate, properties: []}},
            }));
            renderSection();

            expect(screen.getByText(/Nabídku typů příspěvku se nepodařilo načíst/i)).toBeInTheDocument();
            expect(screen.getByRole('button', {name: /Zvolit tier/i})).toBeDisabled();
        });

        it('shows an error and keeps choose disabled when loading the options fails', () => {
            vi.mocked(useAuthorizedQuery).mockImplementation(((href: string | undefined) => ({
                data: href === optionsHref ? undefined : openSummary(),
                isLoading: false,
                error: href === optionsHref ? new Error('boom') : null,
                status: href === optionsHref ? 'error' : 'success',
            })) as unknown as typeof useAuthorizedQuery);
            renderSection();

            expect(screen.getByRole('alert')).toBeInTheDocument();
            expect(screen.getByRole('button', {name: /Zvolit tier/i})).toBeDisabled();
        });

        it('shows a locked card without radio options after the deadline', () => {
            mockSummaryAndGroups(openSummary({
                votingOpen: false,
                currentGroup: {id: 'group-1', name: 'Základ', yearlyFee: 500},
                _templates: undefined,
            }));
            renderSection();

            expect(screen.getByText(/Volba je uzamčena/i)).toBeInTheDocument();
            expect(screen.getByText('Základ')).toBeInTheDocument();
            expect(screen.queryByRole('radio')).not.toBeInTheDocument();
            expect(screen.queryByRole('button')).not.toBeInTheDocument();
        });
    });
});
