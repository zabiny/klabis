// Local calendar date, not toISOString(): the UTC date is a day behind between 00:00 and 02:00 in Prague.
export const localIsoDate = (daysFromToday: number) => {
    const date = new Date();
    date.setDate(date.getDate() + daysFromToday);
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    return `${date.getFullYear()}-${month}-${day}`;
};

export const toDisplayDate = (iso: string) => {
    const [year, month, day] = iso.split('-').map(Number);
    return `${day}. ${month}. ${year}`;
};
