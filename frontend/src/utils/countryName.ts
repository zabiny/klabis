const displayNames = new Intl.DisplayNames(['cs'], {type: 'region', fallback: 'none'});

export const countryName = (code: string | null | undefined): string | undefined => {
    if (!code || !/^[A-Za-z]{2}$/.test(code)) return undefined;
    return displayNames.of(code.toUpperCase());
};

export const countryLabel = (code: string | null | undefined): string | null | undefined => countryName(code) ?? code;
