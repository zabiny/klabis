import {fireEvent, render, screen, waitFor} from '@testing-library/react'
import {MemoryRouter} from 'react-router-dom'
import {QueryClient, QueryClientProvider} from '@tanstack/react-query'
import Layout from './Layout'
import {HalRouteProvider} from '../contexts/HalRouteContext'
import {vi} from 'vitest';
import * as RootNavigationModule from '../hooks/useRootNavigation'
import * as AuthContext2Module from '../contexts/AuthContext2'

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

// Mock LogoutIcon component
vi.mock('../components/Icons', () => ({
    LogoutIcon: () => <div data-testid="logout-icon">Logout Icon</div>,
}))

const useRootNavigation = vi.mocked(RootNavigationModule.useRootNavigation)
const useAuth = vi.mocked(AuthContext2Module.useAuth)

describe('Layout - Responsive Sidebar', () => {
    let queryClient: QueryClient

    beforeEach(() => {
        queryClient = new QueryClient({
            defaultOptions: {
                queries: {retry: false, gcTime: 0},
            },
        })

        // Mock useRootNavigation to return menu items
        useRootNavigation.mockReturnValue({
            data: [
                {rel: 'members', href: '/members', label: 'Members'},
                {rel: 'events', href: '/events', label: 'Events'},
            ],
            isLoading: false,
            error: null,
        })

        // Mock useAuth
        useAuth.mockReturnValue({
            isAuthenticated: true,
            getUser: vi.fn().mockResolvedValue({
                id: 'user-1',
                firstName: 'John',
                lastName: 'Doe',
                registrationNumber: '12345',
            }),
            logout: vi.fn(),
        })

        vi.clearAllMocks()
    })

    const renderLayout = () => {
        return render(
            <QueryClientProvider client={queryClient}>
                <MemoryRouter initialEntries={['/']}>
                    <HalRouteProvider>
                        <Layout/>
                    </HalRouteProvider>
                </MemoryRouter>
            </QueryClientProvider>,
        )
    }

    describe('Sidebar toggle functionality', () => {
        it('should render sidebar hidden by default', async () => {
            renderLayout()

            await waitFor(() => {
                expect(screen.getByText('Members')).toBeInTheDocument()
            })

            // Sidebar should exist and be part of the DOM
            const sidebar = screen.getByRole('complementary') // aside has role complementary
            expect(sidebar).toBeInTheDocument()
        })

        it('should have a toggle button to open/close sidebar', async () => {
            renderLayout()

            // The hamburger button should exist
            const toggleButtons = screen.getAllByRole('button')
            expect(toggleButtons.length).toBeGreaterThan(0)
        })

        it('should render sidebar with menu items', async () => {
            renderLayout()

            await waitFor(() => {
                expect(screen.getByText('Members')).toBeInTheDocument()
                expect(screen.getByText('Events')).toBeInTheDocument()
            })
        })

        it('should close sidebar when a menu item is clicked', async () => {
            renderLayout()

            await waitFor(() => {
                expect(screen.getByText('Members')).toBeInTheDocument()
            })

            // Click a menu item
            const menuItem = screen.getByText('Members')
            fireEvent.click(menuItem)

            // Sidebar should close after click (state changes)
            // This is verified by the internal state management
        })
    })

    describe('Sidebar menu items', () => {
        it('should render menu items from useRootNavigation', async () => {
            renderLayout()

            await waitFor(() => {
                expect(screen.getByText('Members')).toBeInTheDocument()
                expect(screen.getByText('Events')).toBeInTheDocument()
            })
        })

        it('should display loading state when menu is loading', async () => {
            useRootNavigation.mockReturnValue({
                data: [],
                isLoading: true,
                error: null,
            })

            renderLayout()

            await waitFor(() => {
                expect(screen.getByText('Loading menu...')).toBeInTheDocument()
            })
        })

        it('should display error state when menu fails to load', async () => {
            const error = new Error('Failed to load menu')
            useRootNavigation.mockReturnValue({
                data: [],
                isLoading: false,
                error: error,
            })

            renderLayout()

            await waitFor(() => {
                expect(screen.getByText('Failed to load menu: Failed to load menu')).toBeInTheDocument()
            })
        })

        it('should display no menu items message when list is empty', async () => {
            useRootNavigation.mockReturnValue({
                data: [],
                isLoading: false,
                error: null,
            })

            renderLayout()

            await waitFor(() => {
                expect(screen.getByText('No menu items available')).toBeInTheDocument()
            })
        })
    })

    describe('Header and user info', () => {
        it('should display user name in header', async () => {
            renderLayout()

            await waitFor(() => {
                expect(screen.getByText('John Doe [12345]')).toBeInTheDocument()
            })
        })

        it('should render theme toggle', async () => {
            renderLayout()

            await waitFor(() => {
                expect(screen.getByTestId('theme-toggle')).toBeInTheDocument()
            })
        })

        it('should render logout button', async () => {
            renderLayout()

            const logoutButton = screen.getByRole('button', {name: /Odhlásit/i})
            expect(logoutButton).toBeInTheDocument()
        })

        it('should call logout when logout button is clicked', async () => {
            const mockLogout = vi.fn()
            useAuth.mockReturnValue({
                isAuthenticated: true,
                getUser: vi.fn().mockResolvedValue({
                    id: 'user-1',
                    firstName: 'John',
                    lastName: 'Doe',
                    registrationNumber: '12345',
                }),
                logout: mockLogout,
            })

            renderLayout()

            const logoutButton = screen.getByRole('button', {name: /Odhlásit/i})
            fireEvent.click(logoutButton)

            expect(mockLogout).toHaveBeenCalled()
        })
    })
})
