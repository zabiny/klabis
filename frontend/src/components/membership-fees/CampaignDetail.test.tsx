import '@testing-library/jest-dom';
import {render, screen, waitFor} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {MemoryRouter} from 'react-router-dom';
import {QueryClientProvider} from '@tanstack/react-query';
import {useHalPageData} from '../../hooks/useHalPageData';
import {CampaignDetail} from './CampaignDetail';
import {vi} from 'vitest';
import type {HalResponse} from '../../api';
import {mockHalFormsTemplate} from '../../__mocks__/halData';
import {stubFetch, ToastProbe, createTestQueryClient} from '../../__mocks__/halDialogTest.tsx';
import {ToastProvider} from '../../contexts/ToastContext.tsx';

vi.mock('../../hooks/useHalPageData', () => ({
    useHalPageData: vi.fn(),
}));

vi.mock('../../api/klabisUserManager', () => ({
    klabisAuthUserManager: {
        getUser: vi.fn().mockReturnValue({access_token: 'test-token', token_type: 'Bearer'}),
    },
}));

vi.mock('../../hooks/useRootNavigation', () => ({
    useRootNavigation: vi.fn().mockReturnValue({data: [], isLoading: false, error: null}),
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

const createMockPageData = (resourceData: HalResponse | null) => ({
    resourceData,
    isLoading: false,
    error: null,
    pathname: '/administration/fee-selection-campaigns/campaign-1',
    navigateToResource: vi.fn(),
    refetch: async () => {},
    getResourceLink: vi.fn().mockReturnValue({href: 'http://localhost/api/fee-selection-campaigns/campaign-1'}),
});

const renderComponent = (pageData: ReturnType<typeof createMockPageData>) => {
    vi.mocked(useHalPageData).mockReturnValue(pageData as ReturnType<typeof useHalPageData>);
    const queryClient = createTestQueryClient();
    return render(
        <QueryClientProvider client={queryClient}>
            <MemoryRouter>
                <ToastProvider>
                    <CampaignDetail/>
                    <ToastProbe/>
                </ToastProvider>
            </MemoryRouter>
        </QueryClientProvider>
    );
};

describe('CampaignDetail', () => {
    let writeCalls: ReturnType<typeof stubFetch>['writeCalls'];

    beforeEach(() => {
        vi.clearAllMocks();
        ({writeCalls} = stubFetch(url => url.endsWith('/levels')
            ? {_embedded: {membershipFeeGroupResponseList: mockGroups}}
            : {}));
    });

    afterEach(() => {
        vi.unstubAllGlobals();
    });

    it('renders campaign year', () => {
        renderComponent(createMockPageData(buildCampaignDetail()));
        expect(screen.getByText('2025')).toBeInTheDocument();
    });

    it('renders voting deadline label', () => {
        renderComponent(createMockPageData(buildCampaignDetail()));
        expect(screen.getByText('Uzávěrka hlasování')).toBeInTheDocument();
    });

    it('renders fee groups section heading', () => {
        renderComponent(createMockPageData(buildCampaignDetail()));
        expect(screen.getByRole('heading', {name: /skupiny/i})).toBeInTheDocument();
    });

    it('does not render change-deadline button when changeDeadline template is absent', () => {
        renderComponent(createMockPageData(buildCampaignDetail()));
        expect(screen.queryByRole('button', {name: /změnit deadline/i})).not.toBeInTheDocument();
    });

    it('renders change-deadline button when changeDeadline template is present', () => {
        renderComponent(createMockPageData(buildCampaignDetail({
            _templates: {
                changeDeadline: mockHalFormsTemplate({title: 'Změnit deadline', method: 'PATCH'}),
            },
        })));
        expect(screen.getByRole('button', {name: /změnit deadline/i})).toBeInTheDocument();
    });

    it('does not render close-campaign button when closeCampaign template is absent', () => {
        renderComponent(createMockPageData(buildCampaignDetail()));
        expect(screen.queryByRole('button', {name: /uzavřít kampaň/i})).not.toBeInTheDocument();
    });

    it('renders close-campaign button when closeCampaign template is present', () => {
        renderComponent(createMockPageData(buildCampaignDetail({
            _templates: {
                closeCampaign: mockHalFormsTemplate({title: 'Uzavřít kampaň', method: 'POST', properties: []}),
            },
        })));
        expect(screen.getByRole('button', {name: /uzavřít kampaň/i})).toBeInTheDocument();
    });

    it('renders both buttons when both templates are present', () => {
        renderComponent(createMockPageData(buildCampaignDetail({
            _templates: {
                changeDeadline: mockHalFormsTemplate({title: 'Změnit deadline', method: 'PATCH'}),
                closeCampaign: mockHalFormsTemplate({title: 'Uzavřít kampaň', method: 'POST', properties: []}),
            },
        })));
        expect(screen.getByRole('button', {name: /změnit deadline/i})).toBeInTheDocument();
        expect(screen.getByRole('button', {name: /uzavřít kampaň/i})).toBeInTheDocument();
    });

    it('renders fee groups when groups data is available', async () => {
        renderComponent(createMockPageData(buildCampaignDetail()));
        expect(await screen.findByText('Základní členství')).toBeInTheDocument();
    });

    it('opens the change deadline dialog and submits to the template target', async () => {
        const user = userEvent.setup();
        renderComponent(createMockPageData(buildCampaignDetail({
            _templates: {
                changeDeadline: mockHalFormsTemplate({
                    title: 'Změnit deadline', method: 'PATCH', target: '/api/fee-selection-campaigns/campaign-1', properties: [],
                }),
            },
        })));

        await user.click(screen.getByRole('button', {name: /změnit deadline/i}));
        expect(await screen.findByTestId('modal-title')).toHaveTextContent('Změnit deadline');
        await user.click(screen.getByRole('button', {name: /odeslat/i}));

        await waitFor(() => expect(writeCalls()).toHaveLength(1));
        expect(writeCalls()[0][0]).toBe('/api/fee-selection-campaigns/campaign-1');
        await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
        expect(screen.getByTestId('toast')).toHaveTextContent('Změnit deadline — úspěšně uloženo');
    });

    it('opens the close campaign dialog and closes it on cancel without a request', async () => {
        const user = userEvent.setup();
        renderComponent(createMockPageData(buildCampaignDetail({
            _templates: {
                closeCampaign: mockHalFormsTemplate({title: 'Uzavřít kampaň', method: 'POST', properties: []}),
            },
        })));

        await user.click(screen.getByRole('button', {name: /uzavřít kampaň/i}));
        expect(await screen.findByTestId('modal-title')).toHaveTextContent('Uzavřít kampaň');
        await user.click(screen.getByRole('button', {name: /zrušit/i}));

        await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
        expect(writeCalls()).toHaveLength(0);
    });
});
