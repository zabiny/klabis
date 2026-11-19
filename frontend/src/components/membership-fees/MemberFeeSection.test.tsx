import '@testing-library/jest-dom';
import {fireEvent, render, screen, waitFor} from '@testing-library/react';
import {MemoryRouter} from 'react-router-dom';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {vi} from 'vitest';
import {MemberFeeSection} from './MemberFeeSection';
import {useAuthorizedMutation, useAuthorizedQuery} from '../../hooks/useAuthorizedFetch';

vi.mock('../../hooks/useAuthorizedFetch', () => ({
    useAuthorizedQuery: vi.fn(),
    useAuthorizedMutation: vi.fn(),
}));

vi.mock('../../api/authorizedFetch', () => ({
    authorizedFetch: vi.fn().mockResolvedValue({headers: new Headers({Allow: 'POST'})}),
}));

vi.mock('../../contexts/toastContext', () => ({
    useToast: () => ({addToast: vi.fn()}),
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
                _templates: {chooseFeeType: {method: 'POST', target: '/api/members/member-1/fee-choice/2026', properties: []}},
                _links: {self: {href: '/api/members/member-1/fee-summary'}},
            });

            renderSection();

            expect(screen.getByText(/Probíhá volba/i)).toBeInTheDocument();
        });

    });

    describe('votingOpen is true, choice exists', () => {
        it('does not render the current-choice card while the form is editable', () => {
            mockFeeSummary({
                currentGroup: {id: 'group-1', name: 'Základ', yearlyFee: 500},
                votingOpen: true,
                recommendedGroupId: null,
                _templates: {chooseFeeType: {method: 'POST', target: '/api/members/member-1/fee-choice/2026', properties: []}},
                _links: {self: {href: '/api/members/member-1/fee-summary'}},
            });

            renderSection();

            expect(screen.queryByText(/Aktuální typ členského příspěvku/i)).not.toBeInTheDocument();
            expect(screen.getByText(/Volbu je možné změnit/i)).toBeInTheDocument();
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

            expect(screen.queryByRole('button', {name: /Změnit typ členského příspěvku/i})).not.toBeInTheDocument();
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

            expect(screen.queryByRole('button', {name: /Zvolit typ členského příspěvku/i})).not.toBeInTheDocument();
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
            properties: [{
                name: 'membershipFeeGroupId',
                type: 'MembershipFeeGroupChoice',
                required: true,
                options: {link: {href: optionsHref}},
            }],
        };
        const removeTemplate = {method: 'DELETE', target: '/api/members/member-1/fee-choice/2026'};

        const mockSummaryAndGroups = (summary: unknown) => {
            vi.mocked(useAuthorizedQuery).mockImplementation(((href: string | undefined, options?: {enabled?: boolean}) => ({
                data: options?.enabled === false ? undefined : href === optionsHref ? groups : href ? summary : undefined,
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

        it('shows each fee type from the options link with name and yearly price', async () => {
            mockSummaryAndGroups(openSummary());
            renderSection();

            expect(await screen.findByRole('radio', {name: /Základ.*500/})).toBeInTheDocument();
            expect(screen.getByRole('radio', {name: /Plný.*1200/})).toBeInTheDocument();
        });

        it('pre-selects the recommended fee type', async () => {
            mockSummaryAndGroups(openSummary());
            renderSection();

            expect(await screen.findByRole('radio', {name: /Plný/})).toHaveAttribute('aria-checked', 'true');
            expect(screen.getByRole('radio', {name: /Základ/})).toHaveAttribute('aria-checked', 'false');
        });

        it('submits the chosen type to the chooseFeeType template target', async () => {
            mockSummaryAndGroups(openSummary());
            renderSection();

            fireEvent.click(await screen.findByRole('radio', {name: /Základ/}));
            fireEvent.click(screen.getByRole('button', {name: /Zvolit typ členského příspěvku/i}));

            await waitFor(() => expect(mutate).toHaveBeenCalledWith({
                url: '/api/members/member-1/fee-choice/2026',
                data: {membershipFeeGroupId: 'group-1'},
            }, expect.anything()));
        });

        it('pre-selects the currently chosen type instead of the recommended one and hides the current-choice card', async () => {
            mockSummaryAndGroups(openSummary({
                currentGroup: {id: 'group-1', name: 'Základ', yearlyFee: 500},
                recommendedGroupId: 'group-2',
            }));
            renderSection();

            const selected = await screen.findByRole('radio', {name: /Základ/});
            expect(selected).toHaveAttribute('aria-checked', 'true');
            expect(selected.querySelector('svg')).not.toBeNull();
            expect(screen.getByRole('radio', {name: /Plný/})).toHaveAttribute('aria-checked', 'false');
            expect(screen.queryByText(/Aktuální typ členského příspěvku/i)).not.toBeInTheDocument();
        });

        it('returns the preselection to the recommended type after the choice is withdrawn', async () => {
            const chosen = openSummary({
                currentGroup: {id: 'group-1', name: 'Základ', yearlyFee: 500},
                _templates: {chooseFeeType: chooseTemplate, removeChoice: removeTemplate},
            });
            mockSummaryAndGroups(chosen);
            const view = renderSection();
            expect(await screen.findByRole('radio', {name: /Základ/})).toHaveAttribute('aria-checked', 'true');

            mockSummaryAndGroups(openSummary());
            view.rerender(
                <QueryClientProvider client={new QueryClient({defaultOptions: {queries: {retry: false, gcTime: 0}}})}>
                    <MemoryRouter initialEntries={['/members/member-1']}>
                        <MemberFeeSection feeSummaryHref="/api/members/member-1/fee-summary"/>
                    </MemoryRouter>
                </QueryClientProvider>
            );

            await waitFor(() => expect(screen.getByRole('radio', {name: /Plný/})).toHaveAttribute('aria-checked', 'true'));
            expect(screen.getByRole('radio', {name: /Základ/})).toHaveAttribute('aria-checked', 'false');
        });

        it('submits a change to the same template target when a choice already exists', async () => {
            mockSummaryAndGroups(openSummary({
                currentGroup: {id: 'group-1', name: 'Základ', yearlyFee: 500},
                recommendedGroupId: null,
                _templates: {chooseFeeType: chooseTemplate, removeChoice: removeTemplate},
            }));
            renderSection();

            fireEvent.click(await screen.findByRole('radio', {name: /Plný/}));
            fireEvent.click(screen.getByRole('button', {name: /Změnit typ členského příspěvku/i}));

            await waitFor(() => expect(mutate).toHaveBeenCalledWith({
                url: '/api/members/member-1/fee-choice/2026',
                data: {membershipFeeGroupId: 'group-2'},
            }, expect.anything()));
        });

        it('withdraws the choice via the removeChoice template target', async () => {
            mockSummaryAndGroups(openSummary({
                currentGroup: {id: 'group-1', name: 'Základ', yearlyFee: 500},
                _templates: {chooseFeeType: chooseTemplate, removeChoice: removeTemplate},
            }));
            renderSection();

            fireEvent.click(await screen.findByRole('button', {name: /Zrušit volbu/i}));

            expect(mutate).toHaveBeenCalledWith({url: '/api/members/member-1/fee-choice/2026'});
        });

        it('does not offer withdraw without a removeChoice template', async () => {
            mockSummaryAndGroups(openSummary({
                currentGroup: {id: 'group-1', name: 'Základ', yearlyFee: 500},
            }));
            renderSection();

            await screen.findByRole('radio', {name: /Plný/});
            expect(screen.queryByRole('button', {name: /Zrušit volbu/i})).not.toBeInTheDocument();
        });

        it('does not pre-select a recommended type that is not among the offered options and blocks submit', async () => {
            mockSummaryAndGroups(openSummary({recommendedGroupId: 'unknown'}));
            renderSection();

            await screen.findByRole('radio', {name: /Základ/});
            await waitFor(() => expect(screen.getAllByRole('radio').every(r => r.getAttribute('aria-checked') === 'false')).toBe(true));

            fireEvent.click(screen.getByRole('button', {name: /Zvolit typ členského příspěvku/i}));

            await waitFor(() => expect(screen.getByText('Povinné pole')).toBeInTheDocument());
            expect(mutate).not.toHaveBeenCalled();
        });

        it('shows an error and blocks submit when the options link is missing', async () => {
            mockSummaryAndGroups(openSummary({
                _templates: {chooseFeeType: {...chooseTemplate, properties: [{...chooseTemplate.properties[0], options: undefined}]}},
            }));
            renderSection();

            expect(await screen.findByText(/Nabídku typů příspěvku se nepodařilo načíst/i)).toBeInTheDocument();

            fireEvent.click(screen.getByRole('button', {name: /Zvolit typ členského příspěvku/i}));

            await waitFor(() => expect(screen.getByText('Povinné pole')).toBeInTheDocument());
            expect(mutate).not.toHaveBeenCalled();
        });

        it('shows an error and blocks submit when loading the options fails', async () => {
            vi.mocked(useAuthorizedQuery).mockImplementation(((href: string | undefined, options?: {enabled?: boolean}) => ({
                data: href === optionsHref || options?.enabled === false ? undefined : openSummary(),
                isLoading: false,
                error: href === optionsHref ? new Error('boom') : null,
                status: href === optionsHref ? 'error' : 'success',
            })) as unknown as typeof useAuthorizedQuery);
            renderSection();

            expect(await screen.findByRole('alert')).not.toHaveTextContent(/Nabídku typů příspěvku se nepodařilo načíst/i);

            fireEvent.click(screen.getByRole('button', {name: /Zvolit typ členského příspěvku/i}));

            await waitFor(() => expect(screen.getByText('Povinné pole')).toBeInTheDocument());
            expect(mutate).not.toHaveBeenCalled();
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
