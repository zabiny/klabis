/**
 * Returns the index of the currently relevant deadline.
 * The relevant deadline is the earliest one that is >= today; if all have passed, returns the last index.
 */
export function getRelevantDeadlineIndex(deadlines: string[], todayIso: string): number {
    const firstFuture = deadlines.findIndex(d => d >= todayIso);
    return firstFuture === -1 ? deadlines.length - 1 : firstFuture;
}

/**
 * Returns today's date as an ISO string (YYYY-MM-DD) in local time.
 */
export function getTodayIso(): string {
    const now = new Date();
    const year = now.getFullYear();
    const month = String(now.getMonth() + 1).padStart(2, '0');
    const day = String(now.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
}

/**
 * Returns the ISO date string for the day before the given ISO date string.
 * Operates purely on the date components to avoid timezone edge cases.
 */
export function yesterdayOf(isoDate: string): string {
    const [year, month, day] = isoDate.split('-').map(Number);
    const date = new Date(year, month - 1, day - 1);
    const y = date.getFullYear();
    const m = String(date.getMonth() + 1).padStart(2, '0');
    const d = String(date.getDate()).padStart(2, '0');
    return `${y}-${m}-${d}`;
}

export const formatDate = (dateString: string) => {
    if (!dateString) return '-';
    const date = new Date(dateString);
    if (isNaN(date.getTime())) return '-';
    return new Intl.DateTimeFormat('cs-CZ', {
        timeZone: 'Europe/Prague'
    }).format(date);
};

export const formatDateTime = (dateString: string) => {
    if (!dateString) return '-';
    const date = new Date(dateString);
    if (isNaN(date.getTime())) return '-';
    return new Intl.DateTimeFormat('cs-CZ', {
        timeZone: 'Europe/Prague',
        day: 'numeric',
        month: 'numeric',
        year: 'numeric',
        hour: '2-digit',
        minute: '2-digit',
    }).format(date);
};