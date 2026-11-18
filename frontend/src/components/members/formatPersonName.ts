export const formatPersonName = (person?: {firstName?: string | null; lastName?: string | null}): string =>
    [person?.firstName, person?.lastName].filter(Boolean).join(' ');
