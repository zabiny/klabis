export const labels = {
    nav: {
        calendar: 'Kalendář',
        events: 'Závody',
        members: 'Členové',
    },

    buttons: {
        submit: 'Odeslat',
        submitting: 'Odesílám...',
        cancel: 'Zrušit',
        close: 'Zavřít',
        retry: 'Zkusit znovu',
    },

    templates: {
        cancelEvent: 'Zrušit závod',
        finishEvent: 'Ukončit závod',
        unregisterFromEvent: 'Odhlásit se ze závodu',
        updateEvent: 'Upravit',
        registerForEvent: 'Přihlásit se',
        publishEvent: 'Publikovat',
        createEvent: 'Přidat závod',
        createCalendarItem: 'Přidat položku',
        suspendMember: 'Ukončit členství',
        resumeMember: 'Reaktivovat',
        updateMember: 'Upravit profil',
        registerMember: 'Registrovat člena',
    },

    fields: {
        eventCoordinatorId: 'Koordinátor',
        eventDate: 'Datum konání',
        location: 'Místo',
        name: 'Název',
        organizer: 'Pořadatel',
        websiteUrl: 'Webová stránka',
        firstName: 'Jméno',
        lastName: 'Příjmení',
        email: 'E-mail',
        phone: 'Telefon',
        birthDate: 'Datum narození',
        gender: 'Pohlaví',
        nationality: 'Státní příslušnost',
        birthNumber: 'Rodné číslo',
        registrationNumber: 'Registrační číslo',
        chipNumber: 'Číslo čipu',
        bankAccountNumber: 'Číslo bankovního účtu',
        dietaryRestrictions: 'Stravovací omezení',
    },

    enums: {
        eventStatus: {
            ACTIVE: 'Aktivní',
            FINISHED: 'Ukončeno',
            CANCELLED: 'Zrušeno',
            DRAFT: 'Koncept',
        },
        memberStatus: {
            active: 'Aktivní',
            inactive: 'Neaktivní',
        },
        gender: {
            MALE: 'Muž',
            FEMALE: 'Žena',
        },
        deactivationReason: {
            ODHLASKA: 'Odhlášení',
            PRESTUP: 'Přestup',
            OTHER: 'Jiný důvod',
        },
    },

    validation: {
        requiredField: 'Povinné pole',
        invalidEmail: 'Neplatný email',
        mustBeNumber: 'Musí být číslo',
        invalidFormat: 'Nespravny format',
    },

    errors: {
        loadingMenu: 'Načítání menu...',
        noMenuItems: 'Žádné položky menu',
        failedLoadMenu: 'Nepodařilo se načíst menu',
        formValidationErrors: 'Chyby validace formuláře',
        formDataLoadError: 'Nepodařilo se načíst data formuláře',
    },

    ui: {
        form: 'Formulář',
        showRawJson: 'Zobrazit zdrojový JSON',
        completeJson: 'Úplná JSON data položky',
        availableActions: 'Dostupné odkazy',
        availableForms: 'Dostupné formuláře',
        collectionEmpty: 'Kolekce je prázdná',
        viewFullJson: 'Zobrazit celý JSON',
        loadingFormData: 'Načítání dat formuláře...',
    },

    tables: {
        id: 'ID',
        data: 'Údaje',
        actions: 'Akce',
        attribute: 'Atribut',
        value: 'Hodnota',
        items: 'Položky',
        details: 'Detaily',
    },
} as const;

export type Labels = typeof labels;
export type LabelCategory = keyof Labels;

export function t<C extends LabelCategory>(category: C, key: keyof Labels[C]): string {
    return labels[category][key] as string;
}

export function getFieldLabel(fieldName: string, formOverrides?: Record<string, string>): string {
    if (formOverrides && fieldName in formOverrides) {
        return formOverrides[fieldName];
    }
    const fieldLabels = labels.fields as Record<string, string>;
    return fieldLabels[fieldName] ?? fieldName;
}

export function getTemplateLabel(templateName: string): string {
    const templateLabels = labels.templates as Record<string, string>;
    return templateLabels[templateName] ?? templateName;
}

export function getNavLabel(rel: string): string {
    const navLabels = labels.nav as Record<string, string>;
    return navLabels[rel] ?? rel;
}

export function getEnumLabel(enumType: string, value: string): string {
    const enumGroups = labels.enums as Record<string, Record<string, string>>;
    return enumGroups[enumType]?.[value] ?? value;
}
