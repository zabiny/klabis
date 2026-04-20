import '@testing-library/jest-dom';
import {render, screen, fireEvent} from '@testing-library/react';
import {MemoryRouter} from 'react-router-dom';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {useHalPageData} from '../../hooks/useHalPageData';
import {mockHalFormsTemplate} from '../../__mocks__/halData';
import {GroupDetailPage} from './GroupDetailPage';
import {vi} from 'vitest';
import type {HalResponse} from '../../api';

vi.mock('../../hooks/useHalPageData', () => ({
    useHalPageData: vi.fn(),
}));

vi.mock('../../contexts/HalFormContext.tsx', () => ({
    useHalForm: vi.fn().mockReturnValue({
        displayHalForm: vi.fn(),
        currentFormRequest: null,
        closeForm: vi.fn(),
    }),
    HalFormProvider: ({children}: {children: React.ReactNode}) => children,
}));

vi.mock('../../components/HalNavigator2/HalFormDisplay.tsx', () => ({
    HalFormDisplay: ({template}: {template: {properties: Array<{name: string; type: string}>}}) => (
        <div data-testid="hal-form-display">
            {template.properties.map(p => (
                p.type === 'textarea'
                    ? <textarea key={p.name} data-testid={`field-${p.name}`} aria-label={p.name}/>
                    : <input key={p.name} data-testid={`field-${p.name}`} aria-label={p.name}/>
            ))}
        </div>
    ),
}));

vi.mock('../../components/UI', async (importOriginal) => {
    const actual = await importOriginal<typeof import('../../components/UI')>();
    return {
        ...actual,
        Modal: ({isOpen, children, title}: {isOpen: boolean; children: React.ReactNode; title: string}) =>
            isOpen ? <div data-testid="modal-overlay" data-title={title}>{children}</div> : null,
    };
});

vi.mock('../../hooks/useAuthorizedFetch', () => ({
    useAuthorizedQuery: vi.fn().mockReturnValue({data: null, error: null}),
    useAuthorizedMutation: vi.fn().mockReturnValue({
        mutate: vi.fn(),
        isPending: false,
        error: null,
    }),
}));

vi.mock('../../contexts/HalRouteContext.tsx', () => ({
    HalRouteProvider: ({children}: {children: React.ReactNode}) => <>{children}</>,
    useHalRoute: vi.fn(() => ({
        resourceData: {firstName: 'Jana', lastName: 'Nováková', registrationNumber: 'ZBM9500', _links: {self: {href: '/api/members/member-1'}}},
        navigateToResource: vi.fn(),
        isLoading: false,
        error: null,
    })),
}));

const createMockPageData = (resourceData: HalResponse | null, overrides?: Record<string, unknown>) => ({
    resourceData,
    isLoading: false,
    error: null,
    isAdmin: false,
    route: {
        pathname: '/groups/group-1',
        navigateToResource: vi.fn(),
        refetch: vi.fn().mockResolvedValue(undefined),
        queryState: 'success' as const,
        getResourceLink: vi.fn().mockReturnValue({href: 'http://localhost/api/groups/group-1'}),
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
    ...overrides,
});

const renderPage = (pageData: ReturnType<typeof createMockPageData>) => {
    vi.mocked(useHalPageData).mockReturnValue(pageData as ReturnType<typeof useHalPageData>);
    const queryClient = new QueryClient({defaultOptions: {queries: {retry: false, gcTime: 0}}});
    return render(
        <QueryClientProvider client={queryClient}>
            <MemoryRouter initialEntries={['/groups/group-1']}>
                <GroupDetailPage/>
            </MemoryRouter>
        </QueryClientProvider>
    );
};

const buildGroupDetail = (overrides?: Record<string, unknown>): HalResponse => ({
    id: 'group-1',
    name: 'Testovací skupina',
    owners: [],
    members: [],
    pendingInvitations: [],
    _links: {self: {href: '/api/groups/group-1'}},
    ...overrides,
});

const buildPendingInvitationWithAffordance = (overrides?: Record<string, unknown>) => ({
    groupId: 'group-1',
    groupName: 'Testovací skupina',
    invitationId: 'inv-1',
    invitedBy: 'owner-1',
    _links: {
        self: {href: '/api/groups/group-1/invitations/inv-1'},
        invitedMember: {href: '/api/members/member-2'},
    },
    _templates: {
        // Spring HATEOAS does not emit properties for DELETE affordances — empty array reflects the actual backend response
        cancelInvitation: mockHalFormsTemplate({
            title: 'Zrušit pozvánku',
            method: 'DELETE',
            target: '/api/groups/group-1/invitations/inv-1',
            properties: [],
        }),
    },
    ...overrides,
});

const buildPendingInvitationWithoutAffordance = (overrides?: Record<string, unknown>) => ({
    groupId: 'group-1',
    groupName: 'Testovací skupina',
    invitationId: 'inv-2',
    invitedBy: 'owner-1',
    _links: {
        self: {href: '/api/groups/group-1/invitations/inv-2'},
        invitedMember: {href: '/api/members/member-3'},
    },
    ...overrides,
});

describe('GroupDetailPage — cancel invitation (task 7)', () => {
    beforeEach(() => {
        vi.clearAllMocks();
    });

    describe('7.1 / 7.2 — button visibility driven by HAL affordance', () => {
        it('shows "Zrušit pozvánku" button when cancelInvitation affordance is present on invitation row', () => {
            const resourceData = buildGroupDetail({
                pendingInvitations: [buildPendingInvitationWithAffordance()],
            });
            renderPage(createMockPageData(resourceData));
            expect(screen.getByRole('button', {name: /zrušit pozvánku/i})).toBeInTheDocument();
        });

        it('does NOT show "Zrušit pozvánku" button when cancelInvitation affordance is absent (non-owner view)', () => {
            const resourceData = buildGroupDetail({
                pendingInvitations: [buildPendingInvitationWithoutAffordance()],
            });
            renderPage(createMockPageData(resourceData));
            expect(screen.queryByRole('button', {name: /zrušit pozvánku/i})).not.toBeInTheDocument();
        });

        it('shows cancel button only for invitations that have the affordance when mixed', () => {
            const resourceData = buildGroupDetail({
                pendingInvitations: [
                    buildPendingInvitationWithAffordance(),
                    buildPendingInvitationWithoutAffordance(),
                ],
            });
            renderPage(createMockPageData(resourceData));
            expect(screen.getAllByRole('button', {name: /zrušit pozvánku/i})).toHaveLength(1);
        });
    });

    describe('7.3 — modal opens with reason textarea on button click', () => {
        it('clicking "Zrušit pozvánku" opens a confirmation modal', () => {
            const resourceData = buildGroupDetail({
                pendingInvitations: [buildPendingInvitationWithAffordance()],
            });
            renderPage(createMockPageData(resourceData));
            fireEvent.click(screen.getByRole('button', {name: /zrušit pozvánku/i}));
            expect(screen.getByTestId('modal-overlay')).toBeInTheDocument();
        });

        it('confirmation modal title is "Zrušit pozvánku"', () => {
            const resourceData = buildGroupDetail({
                pendingInvitations: [buildPendingInvitationWithAffordance()],
            });
            renderPage(createMockPageData(resourceData));
            fireEvent.click(screen.getByRole('button', {name: /zrušit pozvánku/i}));
            expect(screen.getByTestId('modal-overlay')).toHaveAttribute('data-title', 'Zrušit pozvánku');
        });

        it('confirmation modal contains the HAL form display (with reason field from template)', () => {
            const resourceData = buildGroupDetail({
                pendingInvitations: [buildPendingInvitationWithAffordance()],
            });
            renderPage(createMockPageData(resourceData));
            fireEvent.click(screen.getByRole('button', {name: /zrušit pozvánku/i}));
            expect(screen.getByTestId('hal-form-display')).toBeInTheDocument();
        });

        it('modal renders "reason" textarea even when backend DELETE affordance has empty properties', () => {
            // Backend does not emit properties for DELETE affordances.
            // GroupDetailPage must inject the reason property client-side.
            const resourceData = buildGroupDetail({
                pendingInvitations: [buildPendingInvitationWithAffordance()],
            });
            renderPage(createMockPageData(resourceData));
            fireEvent.click(screen.getByRole('button', {name: /zrušit pozvánku/i}));
            expect(screen.getByTestId('field-reason')).toBeInTheDocument();
        });
    });

    describe('7.4 — cancel closes modal without API call', () => {
        it('opening the modal does not immediately trigger any mutation', () => {
            const resourceData = buildGroupDetail({
                pendingInvitations: [buildPendingInvitationWithAffordance()],
            });
            renderPage(createMockPageData(resourceData));
            fireEvent.click(screen.getByRole('button', {name: /zrušit pozvánku/i}));

            // Modal is open, form is visible — but we did not submit
            expect(screen.getByTestId('modal-overlay')).toBeInTheDocument();
            expect(screen.getByTestId('hal-form-display')).toBeInTheDocument();
        });
    });
});
