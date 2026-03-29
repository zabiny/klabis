export const labels = {
    nav: {
        home: 'Domů',
        calendar: 'Kalendář',
        events: 'Závody',
        members: 'Členové',
    },

    buttons: {
        submit: 'Odeslat',
        submitting: 'Odesílám...',
        importing: 'Importuji...',
        cancel: 'Zrušit',
        close: 'Zavřít',
        retry: 'Zkusit znovu',
        logout: 'Odhlásit',
        edit: 'Upravit',
        save: 'Uložit změny',
        saveChanges: 'Uložit změny',
        savePermissions: 'Uložit oprávnění',
        sendRequest: 'Odeslat žádost',
        setPassword: 'Nastavit heslo a aktivovat účet',
    },

    templates: {
        cancelEvent: 'Zrušit závod',
        finishEvent: 'Ukončit závod',
        unregisterFromEvent: 'Odhlásit se ze závodu',
        updateEvent: 'Upravit',
        registerForEvent: 'Přihlásit se',
        publishEvent: 'Publikovat',
        createEvent: 'Přidat závod',
        importEvent: 'Importovat z ORIS',
        createCalendarItem: 'Přidat položku',
        suspendMember: 'Ukončit členství',
        resumeMember: 'Reaktivovat',
        updateMember: 'Upravit profil',
        registerMember: 'Registrovat člena',
    },

    dialogTitles: {
        publishEvent: 'Publikování závodu',
        cancelEvent: 'Zrušení závodu',
        finishEvent: 'Ukončení závodu',
        registerForEvent: 'Přihlásit se na závod',
        unregisterFromEvent: 'Odhlásit se ze závodu',
        suspendMember: 'Ukončení členství',
        resumeMember: 'Reaktivace člena',
        importEvent: 'Import závodu z ORIS',
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
        dateOfBirth: 'Datum narození',
        gender: 'Pohlaví',
        nationality: 'Státní příslušnost',
        birthNumber: 'Rodné číslo',
        registrationNumber: 'Registrační číslo',
        chipNumber: 'Číslo čipu',
        bankAccountNumber: 'Číslo bankovního účtu',
        dietaryRestrictions: 'Stravovací omezení',
        drivingLicenseGroup: 'Řidičský průkaz',
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
        registrationNumberRequired: 'Registrační číslo je povinné',
        emailRequired: 'Email je povinný',
        emailInvalidFormat: 'Email má neplatný formát',
        passwordRequirements: 'Heslo nesplňuje všechny požadavky',
        passwordsMismatch: 'Hesla se neshodují',
    },

    errors: {
        invalidCredentials: 'Nesprávné registrační číslo nebo heslo',
        configurationError: 'Chyba konfigurace, kontaktujte administrátora',
        loadingMenu: 'Načítání menu...',
        noMenuItems: 'Žádné položky menu',
        failedLoadMenu: 'Nepodařilo se načíst menu',
        formValidationErrors: 'Chyby validace formuláře',
        formDataLoadError: 'Nepodařilo se načíst data formuláře',
        rateLimitedRetryAfter: 'Požadavek byl omezen. Zkuste to znovu za {seconds} sekund.',
        rateLimitedRetryLater: 'Požadavek byl omezen. Zkuste to prosím později.',
        requestFailed: 'Požadavek selhal.',
        unexpectedError: 'Došlo k neočekávané chybě. Zkuste to prosím znovu.',
        tokenExpired: 'Platnost tokenu vypršela nebo byl již použit. Požádejte si o nový.',
        tokenInvalid: 'Neplatný token. Požádejte si o nový.',
        savePermissionsFailed: 'Nepodařilo se uložit oprávnění. Zkuste to prosím znovu.',
        removeLastPermissionsAdmin: 'Nelze odebrat oprávnění správce — systém musí mít alespoň jednoho uživatele se správou oprávnění.',
        memberRegistrationUnavailable: 'Registrace nového člena není k dispozici.',
        importOrisConflict: 'Tento závod již byl importován.',
        importOrisFailed: 'Import závodu se nezdařil. Zkuste to prosím znovu.',
        importOrisLoadFailed: 'Nepodařilo se načíst závody z ORIS.',
        importOrisNoEvents: 'Žádné závody k importu.',
    },

    orisRegions: {
        JM: 'Jihomoravská',
        M: 'Žebříček Morava',
        'ČR': 'ČR',
    } as Record<string, string>,

    ui: {
        hide: 'Skrýt',
        reveal: 'Zobrazit',
        form: 'Formulář',
        showRawJson: 'Zobrazit zdrojový JSON',
        completeJson: 'Úplná JSON data položky',
        availableActions: 'Dostupné odkazy',
        availableForms: 'Dostupné formuláře',
        collectionEmpty: 'Kolekce je prázdná',
        viewFullJson: 'Zobrazit celý JSON',
        loadingFormData: 'Načítání dat formuláře...',
        loading: 'Načítání...',
        navigation: 'Navigace',
        appName: 'Klabis Club Manager',
        appVersion: 'Verze 1.0.0',
        showMemberDetails: 'Zobrazit detail člena',
        backToList: '← Zpět na seznam',
        switchToDark: 'Přepnout do tmavého režimu',
        switchToLight: 'Přepnout do světlého režimu',
        themeDark: 'Tmavý',
        themeLight: 'Světlý',
        menuLoadError: 'Chyba při načítání menu',
        noMenuAvailable: 'Žádné položky menu nejsou dostupné',
        calendarSubtitle: 'Kalendář akcí a důležitých dat',
        prevMonth: 'Předchozí měsíc',
        nextMonth: 'Následující měsíc',
        emailSent: 'Email byl odeslán',
        emailSentDescription: 'Pokud je váš účet stále čekající na aktivaci, obdržíte email s odkazem pro nastavení hesla. Zkontrolujte prosím svou schránku.',
        passwordSetSuccess: 'Heslo úspěšně nastaveno',
        passwordSetSuccessDescription: 'Váš účet byl aktivován. Nyní budete přesměrováni na přihlašovací stránku.',
        permissionsDescription: 'Oprávnění určují, ke kterým funkcím aplikace má uživatel přístup. Změny se projeví okamžitě.',
        permissionsSaved: 'Oprávnění uložena',
        savedSuccessfully: 'Úspěšně uloženo',
    },

    tables: {
        id: 'ID',
        data: 'Údaje',
        actions: 'Akce',
        attribute: 'Atribut',
        value: 'Hodnota',
        items: 'Položky',
        details: 'Detaily',
        status: 'Status',
        date: 'Datum',
        registeredAt: 'Datum přihlášení',
    },

    sections: {
        members: 'Členové',
        membersList: 'Seznam členů',
        events: 'Závody',
        eventsList: 'Seznam závodů',
        eventInfo: 'INFORMACE O ZÁVODĚ',
        registrations: 'Přihlášky',
        personalInfo: 'OSOBNÍ ÚDAJE',
        contact: 'KONTAKT',
        address: 'ADRESA',
        supplementary: 'DOPLŇKOVÉ INFORMACE',
        documentsAndLicenses: 'DOKLADY A LICENCE',
        guardian: 'ZÁKONNÝ ZÁSTUPCE',
        deactivation: 'DEAKTIVACE',
        newMemberRegistration: 'Registrace nového člena',
    },

    dashboard: {
        welcome: 'Vítejte v Klabis',
        subtitle: 'Moderní systém pro správu členského klubu',
        activeMembers: 'Aktivních členů',
        upcomingEvents: 'Nadcházející závody',
        groups: 'Skupiny a týmy',
        systemStatus: 'Systémový status',
        online: 'Online',
        myProfile: 'Můj profil',
        myEvents: 'Moje nadcházející závody',
        openSection: 'Otevřít',
        noUpcomingEvents: 'Žádné nadcházející závody',
    },

    permissions: {
        'MEMBERS:MANAGE': {label: 'Správa členů', description: 'Registrace, úprava a mazání členů'},
        'MEMBERS:PERMISSIONS': {label: 'Správa oprávnění', description: 'Přidělování a odebírání oprávnění uživatelům'},
        'EVENTS:MANAGE': {label: 'Správa akcí', description: 'Vytváření a úprava akcí'},
        'CALENDAR:MANAGE': {label: 'Správa kalendáře', description: 'Vytváření a úprava kalendářních událostí'},
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

export function getTemplateLabel(templateName: string): string | undefined {
    const templateLabels = labels.templates as Record<string, string>;
    return templateLabels[templateName];
}

export function getDialogTitleLabel(templateName: string): string | undefined {
    const dialogTitleLabels = labels.dialogTitles as Record<string, string>;
    return dialogTitleLabels[templateName];
}

export function getNavLabel(rel: string): string {
    const navLabels = labels.nav as Record<string, string>;
    return navLabels[rel] ?? rel;
}

export function getEnumLabel(enumType: string, value: string): string {
    const enumGroups = labels.enums as Record<string, Record<string, string>>;
    return enumGroups[enumType]?.[value] ?? value;
}
