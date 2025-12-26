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
        return <div className="flex justify-center items-center h-96">Načítání...</div>;
    }

    if (error) {
        return <Alert severity="error">{error.message}</Alert>;
    }

    return (
        <div className="space-y-6">
            {/* Header */}
            <div>
                <h1 className="text-4xl font-bold text-gray-900 dark:text-white mb-2 capitalize">
                    {monthName}
                </h1>
                <p className="text-lg text-gray-600 dark:text-gray-400">
                    Kalendář akcí a důležitých dat
                </p>
            </div>

            {/* Calendar */}
            <div className="bg-white dark:bg-gray-800 rounded-lg shadow-md p-6">
                {/* Weekday Headers */}
                <div className="grid grid-cols-7 gap-2 mb-4">
                    {weekdayHeaders.map((day) => (
                        <div
                            key={day}
                            className="text-center font-semibold text-gray-700 dark:text-gray-300 py-2 border-b border-gray-300 dark:border-gray-600"
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
                                min-h-24 p-2 rounded-lg border
                                ${day === null ? 'bg-gray-50 dark:bg-gray-900 border-gray-200 dark:border-gray-700' : ''}
                                ${day !== null ? 'bg-white dark:bg-gray-700 border-gray-300 dark:border-gray-600' : ''}
                                ${day === today ? 'ring-2 ring-blue-500 ring-opacity-50 border-blue-400 dark:border-blue-500' : ''}
                            `}
                        >
                            {day && (
                                <>
                                    <div
                                        className={`
                                            font-semibold mb-1 inline-block px-2 py-1 rounded
                                            ${day === today ? 'bg-blue-500 text-white' : 'text-gray-900 dark:text-white'}
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
                                                className="text-xs bg-blue-50 dark:bg-blue-900 text-blue-800 dark:text-blue-200 p-1 rounded truncate cursor-pointer hover:bg-blue-100 dark:hover:bg-blue-800 transition-colors"
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
                <HalLinksSection
                    links={resourceData._links}
                    onNavigate={handleNavigateToItem}
                />
            ) : null}

            {/* Templates/Forms section */}
            {resourceData?._templates && Object.keys(resourceData._templates).length > 0 ? (
                <HalFormsSection templates={resourceData._templates}/>
            ) : null}

            {/* Full JSON preview */}
            <details className="mt-4 p-2 border rounded bg-gray-50 dark:bg-gray-900">
                <summary className="cursor-pointer font-semibold">Zobrazit surový JSON</summary>
                <JsonPreview data={resourceData} label="Kompletní odpověď"/>
            </details>
        </div>
    );
};

export default CalendarPage;
