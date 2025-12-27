import {useMemo} from 'react';
import {useNavigate, useSearchParams} from 'react-router-dom';
import {useHalRoute} from '../contexts/HalRouteContext';
import {Alert} from '../components/UI';
import {JsonPreview} from '../components/JsonPreview';
import {hasCalendarItems} from '../api';
import {extractNavigationPath} from '../utils/navigationPath';
import {HalLinksSection} from "../components/HalNavigator2/HalLinksSection.tsx";
import {HalFormsSection} from "../components/HalNavigator2/HalFormsSection.tsx";
import {useHalActions} from "../hooks/useHalActions";

interface CalendarItem {
    start: string;
    end: string;
    note: string;
    _links: {
        event?: { href: string };
        self: { href: string };
    };
}

/**
 * CalendarPage - Display calendar with month view and calendar items
 * Shows all calendar items on their respective dates
 */
const CalendarPage = () => {
    const {resourceData, isLoading, error} = useHalRoute();
    const {handleNavigateToItem} = useHalActions();
    const navigate = useNavigate();
    const [searchParams] = useSearchParams();

    // Parse referenceDate from query params, fall back to current date
    const currentDate = useMemo(() => {
        const referenceDate = searchParams.get('referenceDate');
        if (referenceDate) {
            // Parse yyyy-mm-dd format manually to avoid timezone issues
            const parts = referenceDate.split('-');
            if (parts.length === 3) {
                const year = parseInt(parts[0], 10);
                const month = parseInt(parts[1], 10);
                const day = parseInt(parts[2], 10);

                // Validate parsed values
                if (!isNaN(year) && !isNaN(month) && !isNaN(day) &&
                    month >= 1 && month <= 12 && day >= 1 && day <= 31) {
                    // Create date in local timezone (month is 0-indexed in Date constructor)
                    const parsed = new Date(year, month - 1, day);
                    // Double-check the date is valid by verifying the month matches
                    if (parsed.getMonth() === month - 1) {
                        return parsed;
                    }
                }
            }
        }
        return new Date();
    }, [searchParams]);

    // Extract calendar items from resource data using type guard
    let calendarItems: CalendarItem[] = [];
    if (resourceData && hasCalendarItems(resourceData)) {
        calendarItems = resourceData._embedded.calendarItems || [];
    }

    // Calculate month properties
    const year = currentDate.getFullYear();
    const month = currentDate.getMonth();
    const today = currentDate.getDate();

    // First day of the month and total days in month
    const firstDayOfMonth = new Date(year, month, 1);
    const lastDayOfMonth = new Date(year, month + 1, 0);
    const daysInMonth = lastDayOfMonth.getDate();
    const startingDayOfWeek = firstDayOfMonth.getDay();

    // Create array of day objects for the calendar grid
    const calendarDays = [];
    const weekdayOffset = startingDayOfWeek === 0 ? 6 : startingDayOfWeek - 1; // Monday = 0

    // Add empty cells for days before month starts
    for (let i = 0; i < weekdayOffset; i++) {
        calendarDays.push(null);
    }

    // Add days of the month
    for (let day = 1; day <= daysInMonth; day++) {
        calendarDays.push(day);
    }

    // Helper function to handle calendar item click
    const handleItemClick = (item: CalendarItem) => {
        // Prefer event link if available, otherwise navigate to calendar-item details
        const href = item._links.event?.href || item._links.self.href;
        const itemPath = extractNavigationPath(href);
        navigate(itemPath);
    };

    // Helper function to check if a calendar item falls on a specific day
    const getItemsForDay = (day: number): CalendarItem[] => {
        const dateStr = `${year}-${String(month + 1).padStart(2, '0')}-${String(day).padStart(2, '0')}`;
        return calendarItems.filter(item => {
            const startDate = new Date(item.start);
            const endDate = new Date(item.end);
            const currentDay = new Date(dateStr);
            return currentDay >= startDate && currentDay <= endDate;
        });
    };

    // Weekday headers (Czech)
    const weekdayHeaders = ['Pondělí', 'Úterý', 'Středa', 'Čtvrtek', 'Pátek', 'Sobota', 'Neděle'];

    const monthName = new Intl.DateTimeFormat('cs-CZ', {month: 'long', year: 'numeric'}).format(
        new Date(year, month)
    );

    if (isLoading) {
        return <div className="flex justify-center items-center h-96 text-text-secondary">Načítání...</div>;
    }

    if (error) {
        return <Alert severity="error">{error.message}</Alert>;
    }

    return (
        <div className="flex flex-col gap-8">
            {/* Header */}
            <div>
                <h1 className="text-4xl font-bold text-text-primary mb-2 capitalize">
                    {monthName}
                </h1>
                <p className="text-lg text-text-secondary">
                    Kalendář akcí a důležitých dat
                </p>
            </div>

            {/* Calendar */}
            <div className="bg-surface-raised rounded-md border border-border shadow-sm p-6">
                {/* Weekday Headers */}
                <div className="grid grid-cols-7 gap-2 mb-4">
                    {weekdayHeaders.map((day) => (
                        <div
                            key={day}
                            className="text-center font-semibold text-text-primary py-2 border-b border-border"
                        >
                            {day}
                        </div>
                    ))}
                </div>

                {/* Calendar Grid */}
                <div className="grid grid-cols-7 gap-2">
                    {calendarDays.map((day, index) => (
                        <div
                            key={index}
                            className={`
                                min-h-24 p-2 rounded-md border
                                ${day === null ? 'bg-surface-base border-border' : ''}
                                ${day !== null ? 'bg-surface-raised border-border' : ''}
                                ${day === today ? 'ring-2 ring-primary ring-opacity-50 border-primary' : ''}
                            `}
                        >
                            {day && (
                                <>
                                    <div
                                        className={`
                                            font-semibold mb-1 inline-block px-2 py-1 rounded
                                            ${day === today ? 'bg-primary text-white' : 'text-text-primary'}
                                        `}
                                    >
                                        {day}
                                    </div>

                                    {/* Calendar Items for this day */}
                                    <div className="space-y-1 mt-2">
                                        {getItemsForDay(day).map((item, itemIndex) => (
                                            <div
                                                key={itemIndex}
                                                onClick={() => handleItemClick(item)}
                                                className="text-xs bg-accent/15 text-accent p-1 rounded truncate cursor-pointer hover:bg-accent/25 transition-colors"
                                                title={item.note}
                                            >
                                                {item.note}
                                            </div>
                                        ))}
                                    </div>
                                </>
                            )}
                        </div>
                    ))}
                </div>
            </div>

            {/* Links/Actions section */}
            {resourceData?._links && Object.keys(resourceData._links).length > 0 ? (
                <div className="flex flex-col gap-4">
                    <HalLinksSection
                        links={resourceData._links}
                        onNavigate={handleNavigateToItem}
                    />
                </div>
            ) : null}

            {/* Templates/Forms section */}
            {resourceData?._templates && Object.keys(resourceData._templates).length > 0 ? (
                <div className="flex flex-col gap-4">
                    <HalFormsSection templates={resourceData._templates}/>
                </div>
            ) : null}

            {/* Full JSON preview */}
            <details className="p-3 border border-border rounded-md bg-surface-base">
                <summary className="cursor-pointer font-semibold text-text-primary">Zobrazit surový JSON</summary>
                <JsonPreview data={resourceData} label="Kompletní odpověď"/>
            </details>
        </div>
    );
};

export default CalendarPage;
