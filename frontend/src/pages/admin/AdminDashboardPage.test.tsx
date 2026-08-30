import '@testing-library/jest-dom';
import {render, screen} from '@testing-library/react';
import {MemoryRouter} from 'react-router-dom';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {vi} from 'vitest';
import type {NavigationItem} from '../../hooks/useRootNavigation';
import * as RootNavigationModule from '../../hooks/useRootNavigation';
import * as AuthContextModule from '../../contexts/authContext';
import AdminDashboardPage from './AdminDashboardPage';

vi.mock('../../hooks/useRootNavigation', () => ({
    useRootNavigation: vi.fn(),
}))

vi.mock('../../contexts/authContext', async () => {
    const actual = await vi.importActual('../../contexts/authContext')
    return {
        ...actual,
        useAuth: vi.fn(),
    }
})

const useRootNavigation = vi.mocked(RootNavigationModule.useRootNavigation)
const useAuth = vi.mocked(AuthContextModule.useAuth)

function createMockQueryResult<T>(data: T | null = null, overrides: Record<string, unknown> = {}) {
    return {
        data,
        isLoading: false,
        isError: false,
        isPending: false,
        error: null,
        status: 'success' as const,
        fetchStatus: 'idle' as const,
        isFetched: true,
        refetch: vi.fn(),
        ...overrides,
    } as unknown as import('@tanstack/react-query').UseQueryResult<T | undefined>;
}

const adminNavItems: NavigationItem[] = [
    {rel: 'members', href: '/members', label: 'Členové', section: 'main'},
    {rel: 'events', href: '/events', label: 'Akce', section: 'main'},
    {rel: 'groups', href: '/groups', label: 'Skupiny', section: 'main'},
    {rel: 'admin', href: '/admin', label: 'Přehled administrace', section: 'admin'},
];

const renderPage = () =>
    render(
        <QueryClientProvider client={new QueryClient({defaultOptions: {queries: {retry: false, gcTime: 0}}})}>
            <MemoryRouter initialEntries={['/admin']}>
                <AdminDashboardPage/>
            </MemoryRouter>
        </QueryClientProvider>
    );

describe('AdminDashboardPage', () => {
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

    it('displays welcome message with user first name', () => {
        renderPage()
        expect(screen.getByText(/Karel/)).toBeInTheDocument()
    })

    it('displays statistics cards with mock numbers', () => {
        renderPage()
        expect(screen.getByText('42')).toBeInTheDocument()
        expect(screen.getByText('3')).toBeInTheDocument()
        expect(screen.getByText('5')).toBeInTheDocument()
    })

    it('displays stats card labels', () => {
        renderPage()
        expect(screen.getByText('Aktivních členů')).toBeInTheDocument()
        expect(screen.getByText('Skupiny a týmy')).toBeInTheDocument()
        expect(screen.getByText('Systémový status')).toBeInTheDocument()
    })

    it('displays Online status', () => {
        renderPage()
        expect(screen.getByText('Online')).toBeInTheDocument()
    })

    it('displays upcoming events section', () => {
        renderPage()
        expect(screen.getAllByText('Nadcházející akce').length).toBeGreaterThanOrEqual(1)
        expect(screen.getByText('Jarní závod')).toBeInTheDocument()
        expect(screen.getByText('Letní kemp')).toBeInTheDocument()
    })

    it('displays navigation cards for available sections', () => {
        renderPage()
        expect(screen.getByText('Členové')).toBeInTheDocument()
        expect(screen.getByText('Akce')).toBeInTheDocument()
    })
})
