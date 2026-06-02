import '@testing-library/jest-dom';
import {render, screen} from '@testing-library/react';
import {MemoryRouter, useLocation} from 'react-router-dom';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import React from 'react';
import CalendarPage from './CalendarPage.tsx';
import {vi} from 'vitest';
import userEvent from '@testing-library/user-event';
import {useHalPageData} from '../../hooks/useHalPageData';
import type {HalResponse} from '../../api';

const mockNavigate = vi.fn();
vi.mock('react-router-dom', async () => {
    const actual = await vi.importActual<typeof import('react-router-dom')>('react-router-dom');
    return {
        ...actual,
        useNavigate: () => mockNavigate,
    };
});

// Mock child components
vi.mock('../../components/UI', () => ({
    Alert: ({severity, children}: {severity: string; children: React.ReactNode}) => (
        <div data-testid={`alert-${severity}`} role="alert">
            {children}
        </div>
    ),
    Button: ({children, onClick, disabled, ...props}: {children: React.ReactNode; onClick?: () => void; disabled?: boolean; [key: string]: unknown}) => (
        <button onClick={onClick} disabled={disabled} {...props}>
            {children}
        </button>
    ),
    Spinner: () => <div data-testid="spinner" aria-label="Loading"/>,
}));

vi.mock('../../components/JsonPreview', () => ({
    JsonPreview: ({data, label}: {data: unknown; label?: string}) => (
        <div data-testid="json-preview">
            {label && <h2>{label}</h2>}
            <pre>{JSON.stringify(data, null, 2)}</pre>
        </div>
    ),
}));

vi.mock('../../components/HalNavigator2/HalLinksSection.tsx', () => ({
    HalLinksSection: ({links, onNavigate}: {links: unknown; onNavigate: (href: string) => void}) =>
        links ? (
            <div data-testid="hal-links">
                <button onClick={() => onNavigate('/test')}>Navigate</button>
            </div>
        ) : null,
}));

vi.mock('../../components/HalNavigator2/HalFormsSection.tsx', () => ({
    HalFormsSection: ({templates}: {templates: Record<string, {title?: string}> | undefined}) =>
        templates ? (
            <div data-testid="hal-forms">
                {templates &&
                    Object.entries(templates).map(([key, template]) => (
                        <button key={key} data-testid={`form-button-${key}`}>
                            {template.title || key}
                        </button>
                    ))}
            </div>
        ) : null,
}));

vi.mock('../../components/HalNavigator2/HalFormButton.tsx', () => ({
    HalFormButton: ({name, label}: {name: string; label?: string}) => (
        <button data-testid={`hal-form-button-${name}`}>{label || name}</button>
    ),
}));

vi.mock('../../hooks/useHalPageData', () => ({
    useHalPageData: vi.fn(),
}));

const mockUseHalPageData = vi.mocked(useHalPageData);

describe('CalendarPage Component', () => {
    let queryClient: QueryClient;

    beforeEach(() => {
        queryClient = new QueryClient({
            defaultOptions: {
                queries: {retry: false, gcTime: 0},
            },
        });

        vi.clearAllMocks();
        mockNavigate.mockReset();
    });

    // Helper function to create a complete mock context for useHalPageData
    const createMockContext = (resourceData: HalResponse | null = null) => ({
        resourceData,
        isLoading: false,
        error: null,
        isAdmin: false,
        route: {
            pathname: '/calendar',
            navigateToResource: vi.fn(),
            refetch: vi.fn(),
            queryState: 'success' as const,
            getResourceLink: vi.fn(),
        },
        actions: {
            handleNavigateToItem: vi.fn(),
        },
        getLinks: () => resourceData?._links,
        getTemplates: () => resourceData?._templates,
        hasEmbedded: () => !!resourceData?._embedded && Object.keys(resourceData._embedded).length > 0,
        getEmbeddedItems: () => resourceData?._embedded ? Object.values(resourceData._embedded).flat() : [],
        isCollection: () => !!resourceData?.page || (!!resourceData?._embedded && Object.keys(resourceData._embedded).length > 0),
        hasLink: (name: string) => !!resourceData?._links?.[name],
        hasTemplate: (name: string) => !!resourceData?._templates?.[name],
        hasForms: () => !!resourceData?._templates && Object.keys(resourceData._templates).length > 0,
        getPageMetadata: () => resourceData?.page,
    } as unknown as ReturnType<typeof useHalPageData>);

    // Helper function to render with router and query params
    const locationRef: {current: ReturnType<typeof useLocation> | null} = {current: null};
    const LocationCapture = () => {
        locationRef.current = useLocation();
        return null;
    };
    const renderWithRouter = (ui: React.ReactElement, initialRoute: string = '/calendar') => {
        locationRef.current = null;
        return render(
            <QueryClientProvider client={queryClient}>
                <MemoryRouter initialEntries={[initialRoute]}>
                    {ui}
                    <LocationCapture/>
                </MemoryRouter>
            </QueryClientProvider>
        );
    };

    // Mock calendar data
    const mockCalendarData = (items: HalResponse[] = []) => ({
        _links: {
            self: {href: '/api/calendar'},
        },
        _embedded: {
            calendarItemDtoList: items,
        },
    });

    describe('Reference Date Handling', () => {
        it('should use current date when no referenceDate parameter is provided', () => {
            mockUseHalPageData.mockReturnValue(createMockContext(mockCalendarData()));

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
            mockUseHalPageData.mockReturnValue(createMockContext(mockCalendarData()));

            renderWithRouter(<CalendarPage/>, '/calendar?referenceDate=2025-06-15');

            // Should display June 2025
            expect(screen.getByText(/červen 2025/i)).toBeInTheDocument();
        });

        it('should display November 2025 when referenceDate=2025-11-26', () => {
            mockUseHalPageData.mockReturnValue(createMockContext(mockCalendarData()));

            renderWithRouter(<CalendarPage/>, '/calendar?referenceDate=2025-11-26');

            // Should display November 2025, not December
            expect(screen.getByText(/listopadu 2025|listopad 2025/i)).toBeInTheDocument();
            // November 1, 2025 is a Saturday, verify first day cell contains "1"
            const dayCells = screen.getAllByText('1');
            expect(dayCells.length).toBeGreaterThan(0);
        });

        it('should display correct month for different reference dates', () => {
            mockUseHalPageData.mockReturnValue(createMockContext(mockCalendarData()));

            renderWithRouter(<CalendarPage/>, '/calendar?referenceDate=2025-12-25');

            // Should display December 2025
            expect(screen.getByText(/prosinec 2025/i)).toBeInTheDocument();
        });

        it('should fall back to current date for invalid referenceDate format', () => {
            mockUseHalPageData.mockReturnValue(createMockContext(mockCalendarData()));

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
            mockUseHalPageData.mockReturnValue(createMockContext(mockCalendarData()));

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
            mockUseHalPageData.mockReturnValue(createMockContext(mockCalendarData()));

            const {container} = renderWithRouter(<CalendarPage/>);

            // Check for calendar grid with 7 columns (days of week)
            const grids = container.querySelectorAll('.grid-cols-7');
            expect(grids.length).toBeGreaterThan(0);
        });

        it('should display weekday headers', () => {
            mockUseHalPageData.mockReturnValue(createMockContext(mockCalendarData()));

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
                    id: '123e4567-e89b-12d3-a456-426614174000',
                    name: 'Team Meeting',
                    description: 'Monthly team sync',
                    startDate: '2025-06-15',
                    endDate: '2025-06-15',
                    eventId: null,
                    _links: {
                        self: {href: '/api/calendar-items/123e4567-e89b-12d3-a456-426614174000'},
                    },
                },
            ];

            mockUseHalPageData.mockReturnValue(createMockContext(mockCalendarData(items)));

            renderWithRouter(<CalendarPage/>, '/calendar?referenceDate=2025-06-15');

            expect(screen.getByText('Team Meeting')).toBeInTheDocument();
        });

        it('should not show "null" in tooltip when calendar item has no description', () => {
            const items = [
                {
                    id: '123e4567-e89b-12d3-a456-426614174001',
                    name: 'Klubová schůze',
                    description: null,
                    startDate: '2025-06-15',
                    endDate: '2025-06-15',
                    eventId: null,
                    _links: {
                        self: {href: '/api/calendar-items/123e4567-e89b-12d3-a456-426614174001'},
                    },
                },
            ];

            mockUseHalPageData.mockReturnValue(createMockContext(mockCalendarData(items)));

            renderWithRouter(<CalendarPage/>, '/calendar?referenceDate=2025-06-15');

            const itemEl = screen.getByText('Klubová schůze');
            expect(itemEl).toBeInTheDocument();
            expect(itemEl.getAttribute('title')).not.toContain('null');
        });
    });

    describe('Loading and Error States', () => {
        it('should display loading message when data is loading', () => {
            mockUseHalPageData.mockReturnValue({
                ...createMockContext(null),
                isLoading: true,
                resourceData: null,
            });

            renderWithRouter(<CalendarPage/>);

            expect(screen.getByText('Načítání...')).toBeInTheDocument();
        });

        it('should display error alert when error occurs', () => {
            const error = new Error('Failed to load calendar');

            mockUseHalPageData.mockReturnValue({
                ...createMockContext(null),
                error,
                resourceData: null,
            });

            renderWithRouter(<CalendarPage/>);

            expect(screen.getByTestId('alert-error')).toBeInTheDocument();
            expect(screen.getByText('Failed to load calendar')).toBeInTheDocument();
        });
    });

    describe('Navigation via HalLinks', () => {
        it('should update calendar when referenceDate changes via URL navigation', () => {
            mockUseHalPageData.mockReturnValue(createMockContext(mockCalendarData()));

            // Render with December reference date
            const {unmount} = renderWithRouter(<CalendarPage/>, '/calendar?referenceDate=2025-12-15');

            // Should show December 2025
            expect(screen.getByText(/prosinec 2025/i)).toBeInTheDocument();

            unmount();
        });

        it('should correctly display November when referenceDate=2025-11-15', () => {
            mockUseHalPageData.mockReturnValue(createMockContext(mockCalendarData()));

            // Render with November reference date
            renderWithRouter(<CalendarPage/>, '/calendar?referenceDate=2025-11-15');

            // Should show November 2025
            expect(screen.getByText(/listopadu 2025|listopad 2025/i)).toBeInTheDocument();
        });

        it('should display different months for different referenceDate values', () => {
            mockUseHalPageData.mockReturnValue(createMockContext(mockCalendarData()));

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

    describe('Task 6: Můj rozvrh toggle', () => {
        it('6.1 renders "Můj rozvrh" toggle that is OFF by default when URL has no mySchedule param', () => {
            mockUseHalPageData.mockReturnValue(createMockContext(mockCalendarData()));

            renderWithRouter(<CalendarPage/>, '/calendar');

            const toggle = screen.getByRole('button', {name: /můj rozvrh/i});
            expect(toggle).toBeInTheDocument();
            expect(toggle).toHaveAttribute('aria-pressed', 'false');
        });

        it('6.1 renders "Můj rozvrh" toggle that is ON when URL has mySchedule=true', () => {
            mockUseHalPageData.mockReturnValue(createMockContext(mockCalendarData()));

            renderWithRouter(<CalendarPage/>, '/calendar?mySchedule=true');

            const toggle = screen.getByRole('button', {name: /můj rozvrh/i});
            expect(toggle).toBeInTheDocument();
            expect(toggle).toHaveAttribute('aria-pressed', 'true');
        });

        it('6.2 clicking toggle when OFF navigates to URL with mySchedule=true appended', async () => {
            mockUseHalPageData.mockReturnValue(createMockContext(mockCalendarData()));

            renderWithRouter(<CalendarPage/>, '/calendar?referenceDate=2025-06-15');

            const user = userEvent.setup();
            const toggle = screen.getByRole('button', {name: /můj rozvrh/i});
            await user.click(toggle);

            expect(locationRef.current?.search).toContain('mySchedule=true');
        });

        it('6.2 clicking toggle when ON navigates to URL with mySchedule removed', async () => {
            mockUseHalPageData.mockReturnValue(createMockContext(mockCalendarData()));

            renderWithRouter(<CalendarPage/>, '/calendar?mySchedule=true&referenceDate=2025-06-15');

            const user = userEvent.setup();
            const toggle = screen.getByRole('button', {name: /můj rozvrh/i});
            await user.click(toggle);

            expect(locationRef.current?.search).not.toContain('mySchedule');
        });
    });

    describe('Task 7: Toggle drives API call', () => {
        it('7.1 when mySchedule=true is in the URL, the toggle is active', () => {
            mockUseHalPageData.mockReturnValue(createMockContext(mockCalendarData()));

            renderWithRouter(<CalendarPage/>, '/calendar?mySchedule=true');

            const toggle = screen.getByRole('button', {name: /můj rozvrh/i});
            expect(toggle).toHaveAttribute('aria-pressed', 'true');
        });

        it('7.3 calendar renders filtered items when mySchedule=true and API returns only event-date items', () => {
            const filteredItems = [
                {
                    id: 'abc-123',
                    name: 'My Event',
                    description: null,
                    startDate: '2025-06-10',
                    endDate: '2025-06-10',
                    eventId: 'evt-1',
                    _links: {
                        self: {href: '/api/calendar-items/abc-123'},
                        event: {href: '/api/events/evt-1'},
                    },
                },
            ];

            mockUseHalPageData.mockReturnValue(createMockContext(mockCalendarData(filteredItems)));

            renderWithRouter(<CalendarPage/>, '/calendar?mySchedule=true&referenceDate=2025-06-15');

            expect(screen.getByText('My Event')).toBeInTheDocument();
            const toggle = screen.getByRole('button', {name: /můj rozvrh/i});
            expect(toggle).toHaveAttribute('aria-pressed', 'true');
        });
    });

    describe('Task 8: Empty grid with active filter', () => {
        it('8.1 renders empty month grid (no item chips) and toggle stays ON when API returns no items', () => {
            mockUseHalPageData.mockReturnValue(createMockContext(mockCalendarData([])));

            renderWithRouter(<CalendarPage/>, '/calendar?mySchedule=true&referenceDate=2025-06-15');

            const toggle = screen.getByRole('button', {name: /můj rozvrh/i});
            expect(toggle).toHaveAttribute('aria-pressed', 'true');

            // Calendar grid still renders (weekday headers present)
            expect(screen.getByText('Pondělí')).toBeInTheDocument();
        });

        it('8.2 no banner or empty-state message is shown when filtered result is empty', () => {
            mockUseHalPageData.mockReturnValue(createMockContext(mockCalendarData([])));

            renderWithRouter(<CalendarPage/>, '/calendar?mySchedule=true&referenceDate=2025-06-15');

            // No alert element should be present
            expect(screen.queryByRole('alert')).not.toBeInTheDocument();
        });
    });

    describe('Task 9: URL/shareable state and month navigation', () => {
        it('9.2 deep-linked URL with mySchedule=true renders calendar with toggle ON', () => {
            mockUseHalPageData.mockReturnValue(createMockContext(mockCalendarData()));

            renderWithRouter(<CalendarPage/>, '/calendar?mySchedule=true&referenceDate=2025-06-15');

            const toggle = screen.getByRole('button', {name: /můj rozvrh/i});
            expect(toggle).toHaveAttribute('aria-pressed', 'true');
        });

        it('9.1 clicking prev month when mySchedule=true follows HAL prev link (which carries mySchedule)', () => {
            const dataWithPrevLink = {
                ...mockCalendarData(),
                _links: {
                    self: {href: '/api/calendar?startDate=2025-06-01&endDate=2025-06-30&mySchedule=true'},
                    prev: {href: '/api/calendar?startDate=2025-05-01&endDate=2025-05-31&mySchedule=true'},
                    next: {href: '/api/calendar?startDate=2025-07-01&endDate=2025-07-31&mySchedule=true'},
                },
            };

            mockUseHalPageData.mockReturnValue(createMockContext(dataWithPrevLink));

            renderWithRouter(<CalendarPage/>, '/calendar?mySchedule=true&referenceDate=2025-06-15');

            const prevButton = screen.getByRole('button', {name: /předchozí měsíc/i});
            prevButton.click();

            expect(mockNavigate).toHaveBeenCalledOnce();
            const navigatedPath: string = mockNavigate.mock.calls[0][0];
            expect(navigatedPath).toContain('mySchedule=true');
        });

        it('9.1 clicking next month when mySchedule=true follows HAL next link (which carries mySchedule)', () => {
            const dataWithNextLink = {
                ...mockCalendarData(),
                _links: {
                    self: {href: '/api/calendar?startDate=2025-06-01&endDate=2025-06-30&mySchedule=true'},
                    prev: {href: '/api/calendar?startDate=2025-05-01&endDate=2025-05-31&mySchedule=true'},
                    next: {href: '/api/calendar?startDate=2025-07-01&endDate=2025-07-31&mySchedule=true'},
                },
            };

            mockUseHalPageData.mockReturnValue(createMockContext(dataWithNextLink));

            renderWithRouter(<CalendarPage/>, '/calendar?mySchedule=true&referenceDate=2025-06-15');

            const nextButton = screen.getByRole('button', {name: /následující měsíc/i});
            nextButton.click();

            expect(mockNavigate).toHaveBeenCalledOnce();
            const navigatedPath: string = mockNavigate.mock.calls[0][0];
            expect(navigatedPath).toContain('mySchedule=true');
        });
    });
});
