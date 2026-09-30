const ADULT_AGE = 18;

/** Returns undefined while the date is not a complete ISO date, so the form can hold back age-dependent sections. */
export const isMinorOnDate = (dateOfBirth: unknown, today: Date = new Date()): boolean | undefined => {
    if (typeof dateOfBirth !== 'string' || !/^\d{4}-\d{2}-\d{2}$/.test(dateOfBirth)) return undefined;
    const [year, month, day] = dateOfBirth.split('-').map(Number);
    if (year < 1900) return undefined;
    let age = today.getFullYear() - year;
    if (today.getMonth() + 1 < month || (today.getMonth() + 1 === month && today.getDate() < day)) age -= 1;
    return age < ADULT_AGE;
};
