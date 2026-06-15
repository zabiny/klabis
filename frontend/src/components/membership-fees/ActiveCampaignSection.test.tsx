import '@testing-library/jest-dom';
import {render, screen} from '@testing-library/react';
import {MemoryRouter} from 'react-router-dom';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {useAuthorizedQuery} from '../../hooks/useAuthorizedFetch';
import {mockHalFormsTemplate} from '../../__mocks__/halData';
import {ActiveCampaignSection} from './ActiveCampaignSection';
import {vi} from 'vitest';
import type {HalResponse} from '../../api';

vi.mock('../../hooks/useAuthorizedFetch', () => ({
    useAuthorizedQuery: vi.fn().mockReturnValue({data: null, error: null}),
    useAuthorizedMutation: vi.fn().mockReturnValue({
        mutate: vi.fn(),
        isPending: false,
        error: null,
    }),
}));

vi.mock('../../contexts/HalFormContext.tsx', () => ({
    HalFormProvider: ({children}: {children: React.ReactNode}) => children,
}));

vi.mock('../../contexts/halFormContext.ts', () => ({
    useHalForm: vi.fn().mockReturnValue({
        displayHalForm: vi.fn(),
        currentFormRequest: null,
        closeForm: vi.fn(),
    }),
}));

const mockGroups = [
    {
        id: 'group-1',
        name: 'Základní členství',
        memberCount: 10,
        status: 'EDITABLE' as const,
        _links: {self: {href: '/api/membership-fee-groups/group-1'}},
    },
];

const buildCampaignDetail = (overrides?: Partial<HalResponse>): HalResponse => ({
    id: 'campaign-1',
    year: 2025,
    votingDeadline: '2025-03-31',
    _links: {
        self: {href: '/api/fee-selection-campaigns/campaign-1'},
        levels: {href: '/api/fee-selection-campaigns/campaign-1/levels'},
    },
    ...overrides,
});

const renderSection = (activeCampaignHref: string | null) => {
    const queryClient = new QueryClient({defaultOptions: {queries: {retry: false, gcTime: 0}}});
    return render(
        <QueryClientProvider client={queryClient}>
            <MemoryRouter>
                <ActiveCampaignSection activeCampaignHref={activeCampaignHref}/>
            </MemoryRouter>
        </QueryClientProvider>
    );
};

describe('ActiveCampaignSection', () => {
    beforeEach(() => {
        vi.clearAllMocks();
    });

    describe('when activeCampaign link is absent', () => {
        it('renders nothing when activeCampaignHref is null', () => {
            const {container} = renderSection(null);
            expect(container).toBeEmptyDOMElement();
        });
    });

    describe('when activeCampaign link is present', () => {
        beforeEach(() => {
            vi.mocked(useAuthorizedQuery).mockReturnValue({
                data: buildCampaignDetail(),
                error: null,
            } as ReturnType<typeof useAuthorizedQuery>);
        });

        it('renders section heading for active campaign', () => {
            renderSection('/api/fee-selection-campaigns/campaign-1');
            expect(screen.getByRole('heading', {name: /aktivní kampaň/i})).toBeInTheDocument();
        });

        it('renders campaign year', () => {
            renderSection('/api/fee-selection-campaigns/campaign-1');
            expect(screen.getByText('2025')).toBeInTheDocument();
        });

        it('renders voting deadline label', () => {
            renderSection('/api/fee-selection-campaigns/campaign-1');
            expect(screen.getByText('Uzávěrka hlasování')).toBeInTheDocument();
        });

        it('renders fee groups table heading', () => {
            renderSection('/api/fee-selection-campaigns/campaign-1');
            expect(screen.getByRole('heading', {name: /skupiny/i})).toBeInTheDocument();
        });

        it('does not render change-deadline button when changeDeadline template is absent', () => {
            renderSection('/api/fee-selection-campaigns/campaign-1');
            expect(screen.queryByRole('button', {name: /změnit deadline/i})).not.toBeInTheDocument();
        });

        it('renders change-deadline button when changeDeadline template is present', () => {
            vi.mocked(useAuthorizedQuery).mockReturnValue({
                data: buildCampaignDetail({
                    _templates: {
                        changeDeadline: mockHalFormsTemplate({title: 'Změnit deadline', method: 'PATCH'}),
                    },
                }),
                error: null,
            } as ReturnType<typeof useAuthorizedQuery>);
            renderSection('/api/fee-selection-campaigns/campaign-1');
            expect(screen.getByRole('button', {name: /změnit deadline/i})).toBeInTheDocument();
        });

        it('renders fee groups when groups data is available', () => {
            vi.mocked(useAuthorizedQuery)
                .mockReturnValueOnce({data: buildCampaignDetail(), error: null} as ReturnType<typeof useAuthorizedQuery>)
                .mockReturnValueOnce({
                    data: {_embedded: {membershipFeeGroupResponseList: mockGroups}},
                    error: null,
                } as ReturnType<typeof useAuthorizedQuery>);
            renderSection('/api/fee-selection-campaigns/campaign-1');
            expect(screen.getByText('Základní členství')).toBeInTheDocument();
        });
    });
});
