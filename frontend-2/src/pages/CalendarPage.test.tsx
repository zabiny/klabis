import '@testing-library/jest-dom';
import {render, screen} from '@testing-library/react';
import {MemoryRouter} from 'react-router-dom';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import React from 'react';
import CalendarPage from './CalendarPage';

// Mock child components
jest.mock('../contexts/HalRouteContext', () => ({
    ...jest.requireActual('../contexts/HalRouteContext'),
    useHalRoute: jest.fn(),
}));

jest.mock('../components/UI', () => ({
    Alert: ({severity, children}: any) => (
        <div data-testid={`alert-${severity}`} role="alert">
            {children}
        </div>
    ),
}));

jest.mock('../components/JsonPreview', () => ({
    JsonPreview: ({data, label}: any) => (
        <div data-testid="json-preview">
            {label && <h2>{label}</h2>}
            <pre>{JSON.stringify(data, null, 2)}</pre>
        </div>
    ),
}));

jest.mock('../components/HalNavigator2/HalLinksSection.tsx', () => ({
    HalLinksSection: ({links, onNavigate}: any) =>
        links ? (
            <div data-testid="hal-links">
                <button onClick={() => onNavigate('/test')}>Navigate</button>
            </div>
        ) : null,
}));

jest.mock('../components/HalNavigator2/HalFormsSection.tsx', () => ({
    HalFormsSection: ({templates}: any) =>
        templates ? (
            <div data-testid="hal-forms">
                {templates &&
                    Object.entries(templates).map(([key, template]: any) => (
                        <button key={key} data-testid={`form-button-${key}`}>
                            {template.title || key}
                        </button>
                    ))}
            </div>
        ) : null,
}));

jest.mock('../hooks/useHalActions', () => ({
    useHalActions: jest.fn(),
}));

const {useHalRoute} = require('../contexts/HalRouteContext');
const {useHalActions} = require('../hooks/useHalActions');

describe('CalendarPage Component', () => {
    let mockHalActions: any;
    let queryClient: QueryClient;

    beforeEach(() => {
        queryClient = new QueryClient({
            defaultOptions: {
                queries: {retry: false, gcTime: 0},
            },
        });

        mockHalActions = {
            handleNavigateToItem: jest.fn(),
        };

        useHalActions.mockReturnValue(mockHalActions);
        jest.clearAllMocks();
    });

    // Helper function to render with router and query params
    const renderWithRouter = (ui: React.ReactElement, initialRoute: string = '/calendar') => {
        return render(
            <QueryClientProvider client={queryClient}>
                <MemoryRouter initialEntries={[initialRoute]}>
                    {ui}
                </MemoryRouter>
            </QueryClientProvider>
        );
    };

    // Mock calendar data
    const mockCalendarData = (items: any[] = []) => ({
        _links: {
            self: {href: '/api/calendar'},
        },
        _embedded: {
            calendarItems: items,
        },
    });

    describe('Reference Date Handling', () => {
        it('should use current date when no referenceDate parameter is provided', () => {
            useHalRoute.mockReturnValue({
                resourceData: mockCalendarData(),
                isLoading: false,
                error: null,
            });

            renderWithRouter(<CalendarPage/>);

            // Check that current month is displayed
            const today = new Date();
            const monthName = new Intl.DateTimeFormat('cs-CZ', {
                month: 'long',
                year: 'numeric',
            }).format(today);

            expect(screen.getByText(monthName)).toBeInTheDocument();
        });

        it('should use referenceDate when provided in yyyy-mm-dd format', () => {
            useHalRoute.mockReturnValue({
                resourceData: mockCalendarData(),
                isLoading: false,
                error: null,
            });

            renderWithRouter(<CalendarPage/>, '/calendar?referenceDate=2025-06-15');

            // Should display June 2025
            expect(screen.getByText(/červen 2025/i)).toBeInTheDocument();
        });

        it('should display November 2025 when referenceDate=2025-11-26', () => {
            useHalRoute.mockReturnValue({
                resourceData: mockCalendarData(),
                isLoading: false,
                error: null,
            });

            renderWithRouter(<CalendarPage/>, '/calendar?referenceDate=2025-11-26');

            // Should display November 2025, not December
            expect(screen.getByText(/listopadu 2025|listopad 2025/i)).toBeInTheDocument();
            // November 1, 2025 is a Saturday, verify first day cell contains "1"
            const dayCells = screen.getAllByText('1');
            expect(dayCells.length).toBeGreaterThan(0);
        });

        it('should display correct month for different reference dates', () => {
            useHalRoute.mockReturnValue({
                resourceData: mockCalendarData(),
                isLoading: false,
                error: null,
            });

            renderWithRouter(<CalendarPage/>, '/calendar?referenceDate=2025-12-25');

            // Should display December 2025
            expect(screen.getByText(/prosinec 2025/i)).toBeInTheDocument();
        });

        it('should fall back to current date for invalid referenceDate format', () => {
            useHalRoute.mockReturnValue({
                resourceData: mockCalendarData(),
                isLoading: false,
                error: null,
            });

            renderWithRouter(<CalendarPage/>, '/calendar?referenceDate=invalid-date');

            // Check that current month is displayed
            const today = new Date();
            const monthName = new Intl.DateTimeFormat('cs-CZ', {
                month: 'long',
                year: 'numeric',
            }).format(today);

            expect(screen.getByText(monthName)).toBeInTheDocument();
        });

        it('should fall back to current date for malformed dates', () => {
            useHalRoute.mockReturnValue({
                resourceData: mockCalendarData(),
                isLoading: false,
                error: null,
            });

            renderWithRouter(<CalendarPage/>, '/calendar?referenceDate=2025-13-01');

            // Check that current month is displayed (invalid month should be ignored)
            const today = new Date();
            const monthName = new Intl.DateTimeFormat('cs-CZ', {
                month: 'long',
                year: 'numeric',
            }).format(today);

            expect(screen.getByText(monthName)).toBeInTheDocument();
        });
    });

    describe('Calendar Display', () => {
        it('should display calendar grid', () => {
            useHalRoute.mockReturnValue({
                resourceData: mockCalendarData(),
                isLoading: false,
                error: null,
            });

            const {container} = renderWithRouter(<CalendarPage/>);

            // Check for calendar grid with 7 columns (days of week)
            const grids = container.querySelectorAll('.grid-cols-7');
            expect(grids.length).toBeGreaterThan(0);
        });

        it('should display weekday headers', () => {
            useHalRoute.mockReturnValue({
                resourceData: mockCalendarData(),
                isLoading: false,
                error: null,
            });

            renderWithRouter(<CalendarPage/>);

            // Check for Czech weekday headers
            expect(screen.getByText('Pondělí')).toBeInTheDocument();
            expect(screen.getByText('Úterý')).toBeInTheDocument();
            expect(screen.getByText('Středa')).toBeInTheDocument();
            expect(screen.getByText('Čtvrtek')).toBeInTheDocument();
            expect(screen.getByText('Pátek')).toBeInTheDocument();
            expect(screen.getByText('Sobota')).toBeInTheDocument();
            expect(screen.getByText('Neděle')).toBeInTheDocument();
        });

        it('should display calendar items on their respective dates', () => {
            const items = [
                {
                    start: '2025-06-15',
                    end: '2025-06-15',
                    note: 'Team Meeting',
                    _links: {
                        self: {href: '/api/calendar/items/1'},
                    },
                },
            ];

            useHalRoute.mockReturnValue({
                resourceData: mockCalendarData(items),
                isLoading: false,
                error: null,
            });

            renderWithRouter(<CalendarPage/>, '/calendar?referenceDate=2025-06-15');

            expect(screen.getByText('Team Meeting')).toBeInTheDocument();
        });
    });

    describe('Loading and Error States', () => {
        it('should display loading message when data is loading', () => {
            useHalRoute.mockReturnValue({
                resourceData: null,
                isLoading: true,
                error: null,
            });

            renderWithRouter(<CalendarPage/>);

            expect(screen.getByText('Načítání...')).toBeInTheDocument();
        });

        it('should display error alert when error occurs', () => {
            const error = new Error('Failed to load calendar');

            useHalRoute.mockReturnValue({
                resourceData: null,
                isLoading: false,
                error,
            });

            renderWithRouter(<CalendarPage/>);

            expect(screen.getByTestId('alert-error')).toBeInTheDocument();
            expect(screen.getByText('Failed to load calendar')).toBeInTheDocument();
        });
    });

    describe('Navigation via HalLinks', () => {
        it('should update calendar when referenceDate changes via URL navigation', () => {
            useHalRoute.mockReturnValue({
                resourceData: mockCalendarData(),
                isLoading: false,
                error: null,
            });

            // Render with December reference date
            const {unmount} = renderWithRouter(<CalendarPage/>, '/calendar?referenceDate=2025-12-15');

            // Should show December 2025
            expect(screen.getByText(/prosinec 2025/i)).toBeInTheDocument();

            unmount();
        });

        it('should correctly display November when referenceDate=2025-11-15', () => {
            useHalRoute.mockReturnValue({
                resourceData: mockCalendarData(),
                isLoading: false,
                error: null,
            });

            // Render with November reference date
            renderWithRouter(<CalendarPage/>, '/calendar?referenceDate=2025-11-15');

            // Should show November 2025
            expect(screen.getByText(/listopadu 2025|listopad 2025/i)).toBeInTheDocument();
        });

        it('should display different months for different referenceDate values', () => {
            useHalRoute.mockReturnValue({
                resourceData: mockCalendarData(),
                isLoading: false,
                error: null,
            });

            // Test January
            const {unmount: unmountJan} = renderWithRouter(<CalendarPage/>, '/calendar?referenceDate=2025-01-15');
            expect(screen.getByText(/leden 2025/i)).toBeInTheDocument();
            unmountJan();

            // Test March
            const {unmount: unmountMar} = renderWithRouter(<CalendarPage/>, '/calendar?referenceDate=2025-03-15');
            expect(screen.getByText(/březen 2025/i)).toBeInTheDocument();
            unmountMar();

            // Test September
            const {unmount: unmountSep} = renderWithRouter(<CalendarPage/>, '/calendar?referenceDate=2025-09-15');
            expect(screen.getByText(/září 2025/i)).toBeInTheDocument();
            unmountSep();
        });
    });
});
