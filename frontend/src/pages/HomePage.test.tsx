import '@testing-library/jest-dom';
import {render, screen} from '@testing-library/react';
import {MemoryRouter} from 'react-router-dom';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {vi} from 'vitest';
import * as RootNavigationModule from '../hooks/useRootNavigation';
import * as AuthContext2Module from '../contexts/AuthContext2';
import * as UseDashboardModule from '../hooks/useDashboard';
import * as UseMyUpcomingRegistrationsModule from '../hooks/useMyUpcomingRegistrations';
import HomePage from './HomePage';

vi.mock('../hooks/useRootNavigation', () => ({
    useRootNavigation: vi.fn(),
}))

vi.mock('../contexts/AuthContext2', async () => {
    const actual = await vi.importActual('../contexts/AuthContext2')
    return {
        ...actual,
        useAuth: vi.fn(),
    }
})

vi.mock('../hooks/useDashboard', () => ({
    useDashboard: vi.fn(),
}))

vi.mock('../hooks/useMyUpcomingRegistrations', () => ({
    useMyUpcomingRegistrations: vi.fn(),
}))

const useRootNavigation = vi.mocked(RootNavigationModule.useRootNavigation)
const useAuth = vi.mocked(AuthContext2Module.useAuth)
const useDashboard = vi.mocked(UseDashboardModule.useDashboard)
const useMyUpcomingRegistrations = vi.mocked(UseMyUpcomingRegistrationsModule.useMyUpcomingRegistrations)

const createMockQueryResult = (data: any = null, overrides: any = {}) => ({
    data,
    isLoading: false,
    isError: false,
    isPending: false,
    error: null,
    status: 'success' as const,
    fetchStatus: 'idle' as const,
    isFetched: true,
    isStale: false,
    isFetching: false,
    isPlaceholderData: false,
    isRefetching: false,
    refetch: vi.fn(),
    failureCount: 0,
    failureReason: null,
    errorUpdateCount: 0,
    errorUpdatedAt: null,
    dataUpdatedAt: Date.now(),
    ...overrides,
} as any);

const adminNavItems = [
    {rel: 'members', href: '/members', label: 'Členové'},
    {rel: 'events', href: '/events', label: 'Akce'},
    {rel: 'groups', href: '/groups', label: 'Skupiny'},
    {rel: 'admin', href: '/admin', label: 'Admin'},
];

const regularNavItems = [
    {rel: 'events', href: '/events', label: 'Akce'},
];

const renderHomePage = () => {
    const queryClient = new QueryClient({
        defaultOptions: {queries: {retry: false, gcTime: 0}},
    });
    return render(
        <QueryClientProvider client={queryClient}>
            <MemoryRouter initialEntries={['/']}>
                <HomePage/>
            </MemoryRouter>
        </QueryClientProvider>
    );
};

describe('HomePage - Admin Dashboard', () => {
    beforeEach(() => {
        vi.clearAllMocks()
        useRootNavigation.mockReturnValue(createMockQueryResult(adminNavItems))
        useAuth.mockReturnValue({
            isAuthenticated: true,
            login: vi.fn(),
            isLoading: false,
            getUser: vi.fn().mockReturnValue({
                firstName: 'Karel',
                lastName: 'Novák',
                id: 1,
                userName: 'knovak',
                memberId: null,
            }),
            logout: vi.fn(),
        })
    })

    it('should display welcome message with user first name', () => {
        renderHomePage()
        expect(screen.getByText(/Karel/)).toBeInTheDocument()
    })

    it('should display statistics cards with mock numbers', () => {
        renderHomePage()
        expect(screen.getByText('42')).toBeInTheDocument()
        expect(screen.getByText('3')).toBeInTheDocument()
        expect(screen.getByText('5')).toBeInTheDocument()
    })

    it('should display stats card labels', () => {
        renderHomePage()
        expect(screen.getByText('Aktivních členů')).toBeInTheDocument()
        expect(screen.getAllByText('Nadcházející akce').length).toBeGreaterThanOrEqual(1)
        expect(screen.getByText('Skupiny a týmy')).toBeInTheDocument()
        expect(screen.getByText('Systémový status')).toBeInTheDocument()
    })

    it('should display Online status', () => {
        renderHomePage()
        expect(screen.getByText('Online')).toBeInTheDocument()
    })

    it('should not display "Dostupné" or "Nedostupné"', () => {
        renderHomePage()
        expect(screen.queryByText('Dostupné')).not.toBeInTheDocument()
        expect(screen.queryByText('Nedostupné')).not.toBeInTheDocument()
    })

    it('should display upcoming events section', () => {
        renderHomePage()
        expect(screen.getAllByText('Nadcházející akce').length).toBeGreaterThanOrEqual(1)
        expect(screen.getByText('Jarní závod')).toBeInTheDocument()
        expect(screen.getByText('Letní kemp')).toBeInTheDocument()
    })

    it('should display navigation cards for available sections', () => {
        renderHomePage()
        expect(screen.getByText('Členové')).toBeInTheDocument()
        expect(screen.getByText('Akce')).toBeInTheDocument()
    })

    it('should not show "Žádná data dostupná"', () => {
        renderHomePage()
        expect(screen.queryByText('Žádná data dostupná')).not.toBeInTheDocument()
    })
})

describe('HomePage - Regular User Dashboard', () => {
    beforeEach(() => {
        vi.clearAllMocks()
        useRootNavigation.mockReturnValue(createMockQueryResult(regularNavItems))
        useAuth.mockReturnValue({
            isAuthenticated: true,
            login: vi.fn(),
            isLoading: false,
            getUser: vi.fn().mockReturnValue({
                firstName: 'Jana',
                lastName: 'Horáková',
                id: 2,
                userName: 'jhorakova',
                memberId: 'member-uuid-123',
            }),
            logout: vi.fn(),
        })
        useDashboard.mockReturnValue(createMockQueryResult({upcomingRegistrationsHref: undefined}))
        useMyUpcomingRegistrations.mockReturnValue(createMockQueryResult(undefined))
    })

    it('should display welcome message with user first name', () => {
        renderHomePage()
        expect(screen.getByText(/Jana/)).toBeInTheDocument()
    })

    it('should display my profile quick link', () => {
        renderHomePage()
        expect(screen.getByText('Můj profil')).toBeInTheDocument()
    })

    it('should link my profile to member detail page', () => {
        renderHomePage()
        const profileLink = screen.getByRole('link', {name: /Můj profil/i})
        expect(profileLink).toHaveAttribute('href', '/members/member-uuid-123')
    })

    it('should not show admin statistics', () => {
        renderHomePage()
        expect(screen.queryByText('42')).not.toBeInTheDocument()
    })

    it('should not show "Žádná data dostupná"', () => {
        renderHomePage()
        expect(screen.queryByText('Žádná data dostupná')).not.toBeInTheDocument()
    })
})

describe('HomePage - UserDashboard widget: no upcomingRegistrations link', () => {
    beforeEach(() => {
        vi.clearAllMocks()
        useRootNavigation.mockReturnValue(createMockQueryResult(regularNavItems))
        useAuth.mockReturnValue({
            isAuthenticated: true,
            login: vi.fn(),
            isLoading: false,
            getUser: vi.fn().mockReturnValue({
                firstName: 'Jana',
                lastName: 'Horáková',
                id: 2,
                userName: 'jhorakova',
                memberId: 'member-uuid-123',
            }),
            logout: vi.fn(),
        })
        useDashboard.mockReturnValue(createMockQueryResult({upcomingRegistrationsHref: undefined}))
        useMyUpcomingRegistrations.mockReturnValue(createMockQueryResult(undefined))
    })

    it('does not render the "Moje nadcházející akce" section', () => {
        renderHomePage()
        expect(screen.queryByText('Moje nadcházející akce')).not.toBeInTheDocument()
    })

    it('does not render "Zobrazit všechny" link', () => {
        renderHomePage()
        expect(screen.queryByText('Zobrazit všechny')).not.toBeInTheDocument()
    })
})

describe('HomePage - UserDashboard widget: link present, events returned', () => {
    const href = '/api/events?registeredBy=me&dateFrom=2026-04-24&sort=eventDate,ASC&size=3'

    beforeEach(() => {
        vi.clearAllMocks()
        useRootNavigation.mockReturnValue(createMockQueryResult(regularNavItems))
        useAuth.mockReturnValue({
            isAuthenticated: true,
            login: vi.fn(),
            isLoading: false,
            getUser: vi.fn().mockReturnValue({
                firstName: 'Jana',
                lastName: 'Horáková',
                id: 2,
                userName: 'jhorakova',
                memberId: 'member-uuid-123',
            }),
            logout: vi.fn(),
        })
        useDashboard.mockReturnValue(createMockQueryResult({upcomingRegistrationsHref: href}))
        useMyUpcomingRegistrations.mockReturnValue(createMockQueryResult({
            items: [
                {selfHref: '/api/events/evt-1', name: 'Jarní závod', eventDate: '2026-05-01', location: 'Praha'},
                {selfHref: '/api/events/evt-2', name: 'Letní kemp', eventDate: '2026-06-15', location: 'Brno'},
                {selfHref: '/api/events/evt-3', name: 'Podzimní turnaj', eventDate: '2026-09-10', location: 'Ostrava'},
            ],
        }))
    })

    it('renders the "Moje nadcházející akce" section heading', () => {
        renderHomePage()
        expect(screen.getByText('Moje nadcházející akce')).toBeInTheDocument()
    })

    it('renders event names', () => {
        renderHomePage()
        expect(screen.getByText('Jarní závod')).toBeInTheDocument()
        expect(screen.getByText('Letní kemp')).toBeInTheDocument()
        expect(screen.getByText('Podzimní turnaj')).toBeInTheDocument()
    })

    it('renders event locations', () => {
        renderHomePage()
        expect(screen.getByText('Praha')).toBeInTheDocument()
        expect(screen.getByText('Brno')).toBeInTheDocument()
        expect(screen.getByText('Ostrava')).toBeInTheDocument()
    })

    it('renders "Zobrazit všechny" link pointing to filtered events list', () => {
        renderHomePage()
        const link = screen.getByRole('link', {name: /Zobrazit všechny/i})
        expect(link).toBeInTheDocument()
        expect(link).toHaveAttribute('href', '/events?registeredBy=me&time=budouci')
    })

    it('does not render empty-state copy', () => {
        renderHomePage()
        expect(screen.queryByText('Žádné nadcházející akce')).not.toBeInTheDocument()
    })

    it('does not render "Prohlédnout nadcházející akce klubu" CTA', () => {
        renderHomePage()
        expect(screen.queryByText('Prohlédnout nadcházející akce klubu')).not.toBeInTheDocument()
    })
})

describe('HomePage - UserDashboard widget: link present, no events (empty state)', () => {
    const href = '/api/events?registeredBy=me&dateFrom=2026-04-24&sort=eventDate,ASC&size=3'

    beforeEach(() => {
        vi.clearAllMocks()
        useRootNavigation.mockReturnValue(createMockQueryResult(regularNavItems))
        useAuth.mockReturnValue({
            isAuthenticated: true,
            login: vi.fn(),
            isLoading: false,
            getUser: vi.fn().mockReturnValue({
                firstName: 'Jana',
                lastName: 'Horáková',
                id: 2,
                userName: 'jhorakova',
                memberId: 'member-uuid-123',
            }),
            logout: vi.fn(),
        })
        useDashboard.mockReturnValue(createMockQueryResult({upcomingRegistrationsHref: href}))
        useMyUpcomingRegistrations.mockReturnValue(createMockQueryResult({items: []}))
    })

    it('renders the "Moje nadcházející akce" section heading', () => {
        renderHomePage()
        expect(screen.getByText('Moje nadcházející akce')).toBeInTheDocument()
    })

    it('renders the "Žádné nadcházející akce" empty state message', () => {
        renderHomePage()
        expect(screen.getByText('Žádné nadcházející akce')).toBeInTheDocument()
    })

    it('renders "Prohlédnout nadcházející akce klubu" CTA linking to unfiltered future events', () => {
        renderHomePage()
        const link = screen.getByRole('link', {name: /Prohlédnout nadcházející akce klubu/i})
        expect(link).toBeInTheDocument()
        expect(link).toHaveAttribute('href', '/events?time=budouci')
    })

    it('does not render "Zobrazit všechny" when no events', () => {
        renderHomePage()
        expect(screen.queryByText('Zobrazit všechny')).not.toBeInTheDocument()
    })
})
