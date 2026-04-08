import {fireEvent, render, screen, waitFor} from '@testing-library/react'
import {MemoryRouter} from 'react-router-dom'
import {QueryClient, QueryClientProvider} from '@tanstack/react-query'
import Layout from './Layout'
import {HalRouteProvider} from '../contexts/HalRouteContext'
import {vi} from 'vitest';
import * as RootNavigationModule from '../hooks/useRootNavigation'
import * as AuthContext2Module from '../contexts/AuthContext2'
import {AdminModeProvider} from "../contexts/AdminModeContext.tsx";

// Mock useRootNavigation hook to avoid API calls
vi.mock('../hooks/useRootNavigation', () => ({
    useRootNavigation: vi.fn(),
}))

// Mock useAuth hook
vi.mock('../contexts/AuthContext2', async () => {
    const actual = await vi.importActual('../contexts/AuthContext2')
    return {
        ...actual,
        useAuth: vi.fn(),
    }
})

// Mock ThemeToggle component
vi.mock('../components/ThemeToggle/ThemeToggle', () => ({
    ThemeToggle: () => <div data-testid="theme-toggle">Theme Toggle</div>,
}))

// Mock useTheme hook
vi.mock('../theme/ThemeContext', () => ({
    useTheme: () => ({theme: 'light', toggleTheme: vi.fn()}),
}))

// Mock LogoutIcon component
vi.mock('../components/Icons', () => ({
    LogoutIcon: () => <div data-testid="logout-icon">Logout Icon</div>,
}))

const DESKTOP_WIDTH = 768
const MOBILE_WIDTH = 767

const useRootNavigation = vi.mocked(RootNavigationModule.useRootNavigation)
const useAuth = vi.mocked(AuthContext2Module.useAuth)

// Helper to create a complete UseQueryResult mock
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

describe('Layout - Responsive Sidebar', () => {
    let queryClient: QueryClient

    beforeEach(() => {
        // Clear mocks first, before setting up new ones
        vi.clearAllMocks()

        queryClient = new QueryClient({
            defaultOptions: {
                queries: {retry: false, gcTime: 0},
            },
        })

        // Mock useRootNavigation to return menu items
        useRootNavigation.mockReturnValue(
            createMockQueryResult([
                {rel: 'members', href: '/members', label: 'Members', section: 'main'},
                {rel: 'events', href: '/events', label: 'Events', section: 'main'},
            ])
        )

        // Mock useAuth
        useAuth.mockReturnValue({
            isAuthenticated: true,
            login: vi.fn(),
            isLoading: false,
            getUser: vi.fn().mockReturnValue({
                id: 'user-1',
                firstName: 'John',
                lastName: 'Doe',
                userName: '12345',
                memberId: 'member-uuid-1',
            }),
            logout: vi.fn(),
        })
    })

    const renderLayout = () => {
        return render(
            <QueryClientProvider client={queryClient}>
                <AdminModeProvider>
                <MemoryRouter initialEntries={['/']}>
                    <HalRouteProvider>
                        <Layout/>
                    </HalRouteProvider>
                </MemoryRouter>
                </AdminModeProvider>
            </QueryClientProvider>,
        )
    }

    describe('Desktop sidebar', () => {
        beforeEach(() => {
            Object.defineProperty(window, 'innerWidth', {writable: true, configurable: true, value: DESKTOP_WIDTH})
            window.dispatchEvent(new Event('resize'))
        })

        it('should render sidebar on large screens', async () => {
            renderLayout()

            await waitFor(() => {
                expect(screen.getByText('Members')).toBeInTheDocument()
            })

            const sidebar = screen.getByRole('complementary')
            expect(sidebar).toBeInTheDocument()
        })

        it('should not render bottom navigation on large screens', async () => {
            renderLayout()

            await waitFor(() => {
                expect(screen.getByText('Members')).toBeInTheDocument()
            })

            const bottomNav = screen.queryByRole('navigation', {name: /navigace/i})
            expect(bottomNav).not.toBeInTheDocument()
        })

        it('should render sidebar with menu items', async () => {
            renderLayout()

            await waitFor(() => {
                expect(screen.getByText('Members')).toBeInTheDocument()
                expect(screen.getByText('Events')).toBeInTheDocument()
            })
        })
    })

    describe('Mobile bottom navigation', () => {
        beforeEach(() => {
            Object.defineProperty(window, 'innerWidth', {writable: true, configurable: true, value: MOBILE_WIDTH})
            window.dispatchEvent(new Event('resize'))
        })

        it('should render bottom navigation on small screens', async () => {
            renderLayout()

            await waitFor(() => {
                expect(screen.getByText('Members')).toBeInTheDocument()
            })

            const bottomNav = screen.getByRole('navigation', {name: /navigace/i})
            expect(bottomNav).toBeInTheDocument()
        })

        it('should not render sidebar on small screens', async () => {
            renderLayout()

            await waitFor(() => {
                expect(screen.getByText('Members')).toBeInTheDocument()
            })

            const sidebar = screen.queryByRole('complementary')
            expect(sidebar).not.toBeInTheDocument()
        })

        it('should render menu items in bottom navigation', async () => {
            renderLayout()

            await waitFor(() => {
                expect(screen.getByText('Members')).toBeInTheDocument()
                expect(screen.getByText('Events')).toBeInTheDocument()
            })
        })
    })

    describe('Sidebar menu items (desktop)', () => {
        beforeEach(() => {
            Object.defineProperty(window, 'innerWidth', {writable: true, configurable: true, value: DESKTOP_WIDTH})
            window.dispatchEvent(new Event('resize'))
        })

        it('should render menu items from useRootNavigation', async () => {
            renderLayout()

            await waitFor(() => {
                expect(screen.getByText('Members')).toBeInTheDocument()
                expect(screen.getByText('Events')).toBeInTheDocument()
            })
        })

        it('should display loading state when menu is loading', async () => {
            useRootNavigation.mockReturnValue(
                createMockQueryResult([], {isLoading: true, status: 'pending', isPending: true})
            )

            renderLayout()

            await waitFor(() => {
                expect(screen.getByText('Načítání menu...')).toBeInTheDocument()
            })
        })

        it('should display error state when menu fails to load', async () => {
            const error = new Error('Failed to load menu')
            useRootNavigation.mockReturnValue(
                createMockQueryResult([], {isLoading: false, error: error, status: 'error', isError: true})
            )

            renderLayout()

            await waitFor(() => {
                expect(screen.getByText('Chyba při načítání menu: Failed to load menu')).toBeInTheDocument()
            })
        })

        it('should display no menu items message when list is empty', async () => {
            useRootNavigation.mockReturnValue(
                createMockQueryResult([])
            )

            renderLayout()

            await waitFor(() => {
                expect(screen.getByText('Žádné položky menu nejsou dostupné')).toBeInTheDocument()
            })
        })
    })

    describe('Header and user info', () => {
        beforeEach(() => {
            Object.defineProperty(window, 'innerWidth', {writable: true, configurable: true, value: DESKTOP_WIDTH})
            window.dispatchEvent(new Event('resize'))
        })

        it('should display user name in header', async () => {
            renderLayout()

            // Wait for user data to be loaded via async getUser call
            await waitFor(() => {
                // Look for the full name text content, not exact match
                const userButton = screen.getByRole('button', {name: /John/i})
                expect(userButton).toBeInTheDocument()
            }, {timeout: 3000})
        })

        it('should render theme toggle', async () => {
            renderLayout()

            await waitFor(() => {
                expect(screen.getByTestId('theme-toggle')).toBeInTheDocument()
            })
        })

        it('should render logout button', async () => {
            renderLayout()

            // There are two logout buttons (desktop and mobile)
            const logoutButtons = screen.getAllByRole('button', {name: /Odhlásit/i})
            expect(logoutButtons.length).toBeGreaterThan(0)
            expect(logoutButtons[0]).toBeInTheDocument()
        })

        it('should call logout when logout button is clicked', async () => {
            const mockLogout = vi.fn()
            useAuth.mockReturnValue({
                isAuthenticated: true,
                login: vi.fn(),
                isLoading: false,
                getUser: vi.fn().mockReturnValue({
                    id: 'user-1',
                    firstName: 'John',
                    lastName: 'Doe',
                    userName: '12345',
                    memberId: 'member-uuid-1',
                }),
                logout: mockLogout,
            })

            renderLayout()

            // Wait for user details to load
            await waitFor(() => {
                const userButton = screen.getByRole('button', {name: /John/i})
                expect(userButton).toBeInTheDocument()
            }, {timeout: 3000})

            // There are two logout buttons (desktop and mobile), get the first one
            const logoutButtons = screen.getAllByRole('button', {name: /Odhlásit/i})
            fireEvent.click(logoutButtons[0])

            expect(mockLogout).toHaveBeenCalled()
        })
    })

    describe('Desktop sidebar — two-section rendering', () => {
        beforeEach(() => {
            Object.defineProperty(window, 'innerWidth', {writable: true, configurable: true, value: DESKTOP_WIDTH})
            window.dispatchEvent(new Event('resize'))
        })

        it('renders both main heading and Administrace heading when both sections are present, and groups items correctly', async () => {
            useRootNavigation.mockReturnValue(
                createMockQueryResult([
                    {rel: 'events', href: '/events', label: 'Akce', section: 'main'},
                    {rel: 'training-groups', href: '/training-groups', label: 'Tréninkové skupiny', section: 'admin'},
                    {rel: 'category-presets', href: '/category-presets', label: 'Šablony', section: 'admin'},
                    {rel: 'family-groups', href: '/family-groups', label: 'Rodinné skupiny', section: 'admin'},
                ])
            )

            renderLayout()

            await waitFor(() => {
                expect(screen.getByText('Akce')).toBeInTheDocument()
            })

            expect(screen.getByText('Administrace')).toBeInTheDocument()
            expect(screen.getByText('Akce')).toBeInTheDocument()
            expect(screen.getByText('Tréninkové skupiny')).toBeInTheDocument()
            expect(screen.getByText('Šablony')).toBeInTheDocument()
            expect(screen.getByText('Rodinné skupiny')).toBeInTheDocument()
        })

        it('renders only the main heading and no Administrace heading when there are no admin items', async () => {
            useRootNavigation.mockReturnValue(
                createMockQueryResult([
                    {rel: 'events', href: '/events', label: 'Akce', section: 'main'},
                    {rel: 'members', href: '/members', label: 'Členové', section: 'main'},
                ])
            )

            renderLayout()

            await waitFor(() => {
                expect(screen.getByText('Akce')).toBeInTheDocument()
            })

            expect(screen.queryByText('Administrace')).not.toBeInTheDocument()
        })

        it('renders both main heading (empty) and Administrace heading when only admin items are present', async () => {
            useRootNavigation.mockReturnValue(
                createMockQueryResult([
                    {rel: 'training-groups', href: '/training-groups', label: 'Tréninkové skupiny', section: 'admin'},
                ])
            )

            renderLayout()

            await waitFor(() => {
                expect(screen.getByText('Tréninkové skupiny')).toBeInTheDocument()
            })

            expect(screen.getByText('Administrace')).toBeInTheDocument()
        })

        it('renders items within each section in the order they came from the hook', async () => {
            useRootNavigation.mockReturnValue(
                createMockQueryResult([
                    {rel: 'members', href: '/members', label: 'Členové', section: 'main'},
                    {rel: 'events', href: '/events', label: 'Akce', section: 'main'},
                    {rel: 'family-groups', href: '/family-groups', label: 'Rodinné skupiny', section: 'admin'},
                    {rel: 'training-groups', href: '/training-groups', label: 'Tréninkové skupiny', section: 'admin'},
                ])
            )

            renderLayout()

            await waitFor(() => {
                expect(screen.getByText('Členové')).toBeInTheDocument()
            })

            const sidebar = screen.getByRole('complementary')
            const navLinks = sidebar.querySelectorAll('a')
            const linkTexts = Array.from(navLinks).map(a => a.textContent?.trim())

            const membersIdx = linkTexts.findIndex(t => t === 'Členové')
            const eventsIdx = linkTexts.findIndex(t => t === 'Akce')
            const familyIdx = linkTexts.findIndex(t => t === 'Rodinné skupiny')
            const trainingIdx = linkTexts.findIndex(t => t === 'Tréninkové skupiny')

            expect(membersIdx).toBeLessThan(eventsIdx)
            expect(familyIdx).toBeLessThan(trainingIdx)
            expect(eventsIdx).toBeLessThan(familyIdx)
        })
    })

    describe('Mobile bottom nav — main-only filtering', () => {
        beforeEach(() => {
            Object.defineProperty(window, 'innerWidth', {writable: true, configurable: true, value: MOBILE_WIDTH})
            window.dispatchEvent(new Event('resize'))
        })

        it('does not render admin items in the bottom nav when admin items are present', async () => {
            useRootNavigation.mockReturnValue(
                createMockQueryResult([
                    {rel: 'events', href: '/events', label: 'Akce', section: 'main'},
                    {rel: 'training-groups', href: '/training-groups', label: 'Tréninkové skupiny', section: 'admin'},
                    {rel: 'category-presets', href: '/category-presets', label: 'Šablony', section: 'admin'},
                ])
            )

            renderLayout()

            await waitFor(() => {
                expect(screen.getByText('Akce')).toBeInTheDocument()
            })

            const bottomNav = screen.getByRole('navigation', {name: /navigace/i})
            expect(bottomNav).not.toHaveTextContent('Tréninkové skupiny')
            expect(bottomNav).not.toHaveTextContent('Šablony')
        })

        it('renders main items exactly in the bottom nav when only main items are present', async () => {
            useRootNavigation.mockReturnValue(
                createMockQueryResult([
                    {rel: 'events', href: '/events', label: 'Akce', section: 'main'},
                    {rel: 'members', href: '/members', label: 'Členové', section: 'main'},
                ])
            )

            renderLayout()

            await waitFor(() => {
                expect(screen.getByText('Akce')).toBeInTheDocument()
            })

            const bottomNav = screen.getByRole('navigation', {name: /navigace/i})
            expect(bottomNav).toHaveTextContent('Akce')
            expect(bottomNav).toHaveTextContent('Členové')
        })
    })
})
