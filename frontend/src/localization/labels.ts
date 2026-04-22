export const labels = {
    nav: {
        home: 'Domů',
        calendar: 'Kalendář',
        events: 'Akce',
        members: 'Členové',
        groups: 'Skupiny',
        'training-groups': 'Tréninkové skupiny',
        'family-groups': 'Rodinné skupiny',
        'category-presets': 'Šablony kategorií',
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
        addItem: 'Přidat',
        removeItem: 'Odebrat',
        accept: 'Přijmout',
        reject: 'Odmítnout',
        selectFromTemplates: 'Vybrat ze šablon',
    },

    templates: {
        cancelEvent: 'Zrušit akci',
        unregisterFromEvent: 'Odhlásit se z akce',
        editRegistration: 'Upravit přihlášku',
        updateEvent: 'Upravit',
        registerForEvent: 'Přihlásit se',
        publishEvent: 'Publikovat',
        createEvent: 'Přidat akci',
        importEvent: 'Importovat z ORIS',
        syncEventFromOris: 'Synchronizovat z ORISu',
        createCalendarItem: 'Přidat položku',
        suspendMember: 'Ukončit členství',
        resumeMember: 'Reaktivovat',
        updateMember: 'Upravit profil',
        registerMember: 'Registrovat člena',
        createGroup: 'Vytvořit skupinu',
        updateGroup: 'Upravit název',
        deleteGroup: 'Smazat skupinu',
        addGroupMember: 'Přidat člena',
        removeGroupMember: 'Odebrat',
        inviteMember: 'Pozvat člena',
        createTrainingGroup: 'Vytvořit tréninkovou skupinu',
        updateTrainingGroup: 'Upravit skupinu',
        deleteTrainingGroup: 'Smazat tréninkovou skupinu',
        addTrainingGroupMember: 'Přidat člena',
        removeTrainingGroupMember: 'Odebrat',
        createFamilyGroup: 'Vytvořit rodinnou skupinu',
        addOwner: 'Přidat správce',
        removeOwner: 'Odebrat správce',
        addGroupOwner: 'Přidat správce',
        removeGroupOwner: 'Odebrat správce',
        addTrainer: 'Přidat trenéra',
        removeTrainer: 'Odebrat trenéra',
        addFamilyGroupParent: 'Přidat rodiče',
        removeFamilyGroupParent: 'Odebrat rodiče',
        addFamilyGroupChild: 'Přidat dítě',
        removeFamilyGroupChild: 'Odebrat',
        addFamilyGroupMember: 'Přidat člena',
        deleteFamilyGroup: 'Smazat rodinnou skupinu',
        cancelInvitation: 'Zrušit pozvánku',
        createCategoryPreset: 'Přidat šablonu',
        updateCategoryPreset: 'Upravit šablonu',
        deleteCategoryPreset: 'Smazat šablonu',
    },

    dialogTitles: {
        selectCategoryPreset: 'Vybrat šablonu kategorií',
        publishEvent: 'Publikování akce',
        cancelEvent: 'Zrušení akce',
        registerForEvent: 'Přihlásit se na akci',
        unregisterFromEvent: 'Odhlásit se z akce',
        editRegistration: 'Upravit přihlášku',
        suspendMember: 'Ukončení členství',
        resumeMember: 'Reaktivace člena',
        importEvent: 'Import akce z ORIS',
    },

    fields: {
        eventCoordinatorId: 'Koordinátor',
        eventDate: 'Datum konání',
        registrationDeadline: 'Uzávěrka',
        category: 'Kategorie',
        categories: 'Kategorie',
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
        memberId: 'Člen',
        memberIds: 'Členové',
        parentIds: 'Rodiče',
        trainerId: 'Trenér',
        minAge: 'Min. věk',
        maxAge: 'Max. věk',
        memberCount: 'Počet členů',
        ageRange: 'Věkové rozmezí',
        reason: 'Důvod zrušení (volitelné)',
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
        importOrisConflict: 'Tato akce již byla importována.',
        importOrisFailed: 'Import akce se nezdařil. Zkuste to prosím znovu.',
        importOrisLoadFailed: 'Nepodařilo se načíst akce z ORIS.',
        importOrisNoEvents: 'Žádné akce k importu.',
    },

    orisRegions: {
        JIHOMORAVSKA: 'Jihomoravská',
        MORAVA: 'Žebříček Morava',
        CR: 'ČR',
    } as Record<string, string>,

    familyGroupRoles: {
        parent: 'Rodič',
        child: 'Dítě',
    },

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
        navAdminSection: 'Administrace',
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
        notAssigned: 'Není přiřazen',
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
        joinedAt: 'Datum vstupu',
        web: 'Web',
        registrationDeadline: 'Uzávěrka',
        coordinator: 'Koordinátor',
    },

    sections: {
        members: 'Členové',
        membersList: 'Seznam členů',
        events: 'Akce',
        eventsList: 'Seznam akcí',
        eventInfo: 'INFORMACE O AKCI',
        registrations: 'Přihlášky',
        personalInfo: 'OSOBNÍ ÚDAJE',
        contact: 'KONTAKT',
        address: 'ADRESA',
        supplementary: 'DOPLŇKOVÉ INFORMACE',
        documentsAndLicenses: 'DOKLADY A LICENCE',
        guardian: 'ZÁKONNÝ ZÁSTUPCE',
        deactivation: 'DEAKTIVACE',
        newMemberRegistration: 'Registrace nového člena',
        groups: 'Skupiny',
        groupsList: 'Moje skupiny',
        groupOwners: 'SPRÁVCI',
        groupMembers: 'ČLENOVÉ',
        pendingInvitations: 'ČEKAJÍCÍ POZVÁNKY',
        trainingGroupsList: 'Tréninkové skupiny',
        trainingGroupMembers: 'ČLENOVÉ',
        trainingGroupTrainers: 'TRENÉŘI',
        trainingGroup: 'TRÉNINKOVÁ SKUPINA',
        familyGroup: 'RODINNÁ SKUPINA',
        familyGroups: 'Rodinné skupiny',
        familyGroupsList: 'Rodinné skupiny',
        familyGroupParents: 'RODIČE',
        familyGroupChildren: 'DĚTI',
        familyGroupMembers: 'ČLENOVÉ',
        categoryPresetsList: 'Šablony kategorií',
        presetsListHeading: 'Seznam šablon',
    },

    dashboard: {
        welcome: 'Vítejte v Klabis',
        subtitle: 'Moderní systém pro správu členského klubu',
        activeMembers: 'Aktivních členů',
        upcomingEvents: 'Nadcházející akce',
        groups: 'Skupiny a týmy',
        systemStatus: 'Systémový status',
        online: 'Online',
        myProfile: 'Můj profil',
        myEvents: 'Moje nadcházející akce',
        openSection: 'Otevřít',
        noUpcomingEvents: 'Žádné nadcházející akce',
    },

    permissions: {
        'MEMBERS:MANAGE': {label: 'Správa členů', description: 'Registrace, úprava a mazání členů'},
        'MEMBERS:PERMISSIONS': {label: 'Správa oprávnění', description: 'Přidělování a odebírání oprávnění uživatelům'},
        'EVENTS:MANAGE': {label: 'Správa akcí', description: 'Vytváření a úprava akcí'},
        'CALENDAR:MANAGE': {label: 'Správa kalendáře', description: 'Vytváření a úprava kalendářních událostí'},
        'GROUPS:TRAINING': {label: 'Správa tréninkových skupin', description: 'Umožňuje vytvářet a spravovat tréninkové skupiny a jejich členy.'},
    },

    links: {
        trainingGroup: 'Tréninková skupina',
        familyGroup: 'Rodina',
    },

    suspensionWarning: {
        title: 'Varování: Správce skupin',
        description: 'Člen je posledním správcem následujících skupin. Před ukončením členství je nutné určit nástupce nebo skupiny rozpustit.',
        designateSuccessor: 'Určit nástupce',
        dissolveGroup: 'Rozpustit skupinu',
        trainingGroupNote: 'U tréninkové skupiny je nutné určit nástupce.',
        groupTypeTrainingGroup: 'Tréninková skupina',
        groupTypeFamilyGroup: 'Rodinná skupina',
        groupTypeFreeGroup: 'Volná skupina',
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
