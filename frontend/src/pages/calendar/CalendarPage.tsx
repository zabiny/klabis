import {useMemo} from 'react';
import {useNavigate, useSearchParams} from 'react-router-dom';
import {Alert, Button, Spinner} from '../../components/UI';
import {hasCalendarItems} from '../../api';
import {HalFormButton} from '../../components/HalNavigator2/HalFormButton.tsx';
import {toHref} from '../../api/hateoas.ts';
import {extractNavigationPath} from '../../utils/navigationPath.ts';
import {useHalPageData} from '../../hooks/useHalPageData.ts';

interface CalendarItem {
    id: string;
    name: string;
    description: string;
    startDate: string;
    endDate: string;
    eventId: string | null;
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
    const {resourceData, isLoading, error} = useHalPageData();
    const navigate = useNavigate();
    const [searchParams] = useSearchParams();

    const currentDate = useMemo(() => {
        // Primary: use startDate from self link (reflects actual fetched month)
        if (resourceData?._links?.self) {
            const selfHref = toHref(resourceData._links.self);
            const url = new URL(selfHref, 'https://placeholder');
            const startDate = url.searchParams.get('startDate');
            if (startDate) {
                const parts = startDate.split('-');
                if (parts.length === 3) {
                    const year = parseInt(parts[0], 10);
                    const month = parseInt(parts[1], 10);
                    if (!isNaN(year) && !isNaN(month) && month >= 1 && month <= 12) {
                        return new Date(year, month - 1, 1);
                    }
                }
            }
        }
        // Fallback: referenceDate URL param or today
        const referenceDate = searchParams.get('referenceDate');
        if (referenceDate) {
            const parts = referenceDate.split('-');
            if (parts.length === 3) {
                const year = parseInt(parts[0], 10);
                const month = parseInt(parts[1], 10);
                const day = parseInt(parts[2], 10);
                if (!isNaN(year) && !isNaN(month) && !isNaN(day) &&
                    month >= 1 && month <= 12 && day >= 1 && day <= 31) {
                    const parsed = new Date(year, month - 1, day);
                    if (parsed.getMonth() === month - 1) {
                        return parsed;
                    }
                }
            }
        }
        return new Date();
    }, [resourceData, searchParams]);

    // Extract calendar items from resource data using type guard
    let calendarItems: CalendarItem[] = [];
    if (resourceData && hasCalendarItems(resourceData)) {
        calendarItems = resourceData._embedded.calendarItemDtoList || [];
    }

    // Calculate month properties
    const year = currentDate.getFullYear();
    const month = currentDate.getMonth();
    const now = new Date();
    const todayDay = (now.getFullYear() === year && now.getMonth() === month) ? now.getDate() : null;

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

    const handlePrevMonth = () => {
        const prevLink = resourceData?._links?.prev;
        if (prevLink) navigate(extractNavigationPath(toHref(prevLink)));
    };

    const handleNextMonth = () => {
        const nextLink = resourceData?._links?.next;
        if (nextLink) navigate(extractNavigationPath(toHref(nextLink)));
    };

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
            const startDate = new Date(item.startDate);
            const endDate = new Date(item.endDate);
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
        return (
            <div className="flex justify-center items-center gap-2 h-96 text-text-secondary">
                <Spinner/>
                Načítání...
            </div>
        );
    }

    if (error) {
        return <Alert severity="error">{error.message}</Alert>;
    }

    return (
        <div className="flex flex-col gap-8">
            {/* Header */}
            <div className="flex items-center justify-between">
                <div className="flex items-center gap-4">
                    <Button
                        variant="secondary"
                        size="sm"
                        onClick={handlePrevMonth}
                        disabled={!resourceData?._links?.prev}
                        aria-label="Předchozí měsíc"
                    >
                        ←
                    </Button>
                    <div>
                        <h1 className="text-4xl font-bold text-text-primary mb-2 capitalize">
                            {monthName}
                        </h1>
                        <p className="text-lg text-text-secondary">
                            Kalendář akcí a důležitých dat
                        </p>
                    </div>
                    <Button
                        variant="secondary"
                        size="sm"
                        onClick={handleNextMonth}
                        disabled={!resourceData?._links?.next}
                        aria-label="Následující měsíc"
                    >
                        →
                    </Button>
                </div>
                <HalFormButton name="createCalendarItem" modal={true} label="Přidat položku"/>
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
                                ${day === todayDay ? 'ring-2 ring-primary ring-opacity-50 border-primary' : ''}
                            `}
                        >
                            {day && (
                                <>
                                    <div
                                        className={`
                                            font-semibold mb-1 inline-block px-2 py-1 rounded
                                            ${day === todayDay ? 'bg-primary text-white' : 'text-text-primary'}
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
                                                title={`${item.name}\n${item.description}`}
                                            >
                                                {item.name}
                                            </div>
                                        ))}
                                    </div>
                                </>
                            )}
                        </div>
                    ))}
                </div>
            </div>
        </div>
    );
};

export default CalendarPage;
