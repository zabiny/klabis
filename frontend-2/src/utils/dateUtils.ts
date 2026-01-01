export const formatDate = (dateString: string) => {
    if (!dateString) return '-';
    const date = new Date(dateString);
    if (isNaN(date.getTime())) return '-';
    return new Intl.DateTimeFormat('cs-CZ', {
        timeZone: 'Europe/Prague'
    }).format(date);
};