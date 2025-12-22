import {useState} from 'react';
import {useNavigate} from 'react-router-dom';
import {useHalRoute} from '../contexts/HalRouteContext';
import {Alert, Button} from '../components/UI';
import {JsonPreview} from '../components/JsonPreview';
import {halFormsFieldsFactory, HalFormsForm} from '../components/HalFormsForm';
import type {HalFormsTemplate, TemplateTarget} from '../api';
import {isFormValidationError, submitHalFormsData} from '../api/hateoas';
import {extractNavigationPath} from '../utils/navigationPath';

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
    const {resourceData, isLoading, error, refetch, pathname} = useHalRoute();
    const navigate = useNavigate();
    const [currentDate] = useState(new Date());
    const [selectedTemplate, setSelectedTemplate] = useState<HalFormsTemplate | null>(null);
    const [submitError, setSubmitError] = useState<Error | null>(null);
    const [isSubmitting, setIsSubmitting] = useState(false);

    // Extract calendar items from resource data
    const calendarItems: CalendarItem[] = resourceData?._embedded?.calendarItems || [];

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

    // Helper function to handle action link clicks
    const handleNavigateToAction = (href: string) => {
        const path = extractNavigationPath(href);
        navigate(path);
    };

    // Helper function to handle form submission
    const handleFormSubmit = async (formData: Record<string, any>) => {
        if (!selectedTemplate) return;

        setIsSubmitting(true);
        setSubmitError(null);

        try {
            const submitTarget: TemplateTarget = {
                target: '/api' + pathname,
                method: selectedTemplate.method || 'POST',
            };
            await submitHalFormsData(submitTarget, formData);
            // Refetch data after successful submission
            await refetch();
            // Close the form
            setSelectedTemplate(null);
        } catch (err) {
            setSubmitError(err instanceof Error ? err : new Error('Failed to submit form'));
        } finally {
            setIsSubmitting(false);
        }
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

            {/* Legend */}
            <div className="bg-blue-50 dark:bg-blue-900 rounded-lg p-4">
                <p className="text-sm text-gray-700 dark:text-gray-300">
                    <span className="font-semibold">Celkem akcí:</span> {calendarItems.length}
                </p>
            </div>

            {/* Links/Actions section */}
            {resourceData?._links && Object.keys(resourceData._links).length > 0 ? (
                <div className="mt-4 p-4 border rounded bg-blue-50 dark:bg-blue-900">
                    <h3 className="font-semibold mb-2">Dostupné akce</h3>
                    <div className="flex flex-wrap gap-2">
                        {Object.entries(resourceData._links as Record<string, any>)
                            .filter(([rel]) => rel !== 'self')
                            .map(([rel, link]: [string, any]) => {
                                const links = Array.isArray(link) ? link : [link];
                                return links.map((l: any, idx: number) => (
                                    <button
                                        key={`${rel}-${idx}`}
                                        onClick={() => handleNavigateToAction(l.href)}
                                        className="px-3 py-1 bg-blue-600 text-white rounded hover:bg-blue-700 text-sm border-none cursor-pointer"
                                        title={rel}
                                    >
                                        {l.title || rel}
                                    </button>
                                ));
                            })}
                    </div>
                </div>
            ) : null}

            {/* Templates/Forms section */}
            {resourceData?._templates && Object.keys(resourceData._templates).length > 0 ? (
                <div className="mt-4 p-4 border rounded bg-green-50 dark:bg-green-900">
                    <h3 className="font-semibold mb-2">Dostupné formuláře</h3>
                    {selectedTemplate ? (
                        <div className="space-y-4">
                            <div className="flex items-center justify-between mb-4">
                                <h4 className="font-semibold">{selectedTemplate.title || 'Formulář'}</h4>
                                <Button
                                    onClick={() => setSelectedTemplate(null)}
                                    variant="secondary"
                                    size="sm"
                                >
                                    Zavřít
                                </Button>
                            </div>

                            {submitError && (
                                <Alert severity="error">
                                    <div className="space-y-1">
                                        <p>{submitError.message}</p>
                                        {isFormValidationError(submitError) && (
                                            <ul className="list-disc list-inside text-sm">
                                                {Object.entries(submitError.validationErrors).map(([field, error]) => (
                                                    <li key={field}>{field}: {error}</li>
                                                ))}
                                            </ul>
                                        )}
                                    </div>
                                </Alert>
                            )}

                            <HalFormsForm
                                data={resourceData}
                                template={selectedTemplate}
                                onSubmit={handleFormSubmit}
                                onCancel={() => setSelectedTemplate(null)}
                                isSubmitting={isSubmitting}
                                fieldsFactory={halFormsFieldsFactory}
                            />
                        </div>
                    ) : (
                        <div className="flex flex-wrap gap-2">
                            {Object.entries(resourceData._templates as Record<string, any>).map(([templateName, template]: [string, any]) => (
                                <button
                                    key={templateName}
                                    onClick={() => setSelectedTemplate(template)}
                                    className="px-3 py-1 bg-green-600 text-white rounded hover:bg-green-700 text-sm border-none cursor-pointer"
                                    title={template.title || templateName}
                                >
                                    {template.title || templateName}
                                </button>
                            ))}
                        </div>
                    )}
                </div>
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
