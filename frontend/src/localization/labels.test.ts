import {describe, expect, it} from 'vitest';
import {czechPlural, getPermissionInfo, getEnumLabel, getFieldLabel, getNavLabel, getTemplateLabel, labels, t,} from './labels';

describe('labels', () => {
    it('has nav labels', () => {
        expect(labels.nav.calendar).toBe('Kalendář');
        expect(labels.nav.events).toBe('Akce');
        expect(labels.nav.members).toBe('Členové');
    });

    it('has button labels', () => {
        expect(labels.buttons.submit).toBe('Odeslat');
        expect(labels.buttons.cancel).toBe('Zrušit');
        expect(labels.buttons.close).toBe('Zavřít');
        expect(labels.buttons.retry).toBe('Zkusit znovu');
    });

    it('has template labels', () => {
        expect(labels.templates.cancelEvent).toBe('Zrušit akci');
        expect(labels.templates.unregisterFromEvent).toBe('Odhlásit se z akce');
        expect(labels.templates.updateEvent).toBe('Upravit');
        expect(labels.templates.registerForEvent).toBe('Přihlásit se');
        expect(labels.templates.publishEvent).toBe('Publikovat');
        expect(labels.templates.createEvent).toBe('Přidat akci');
        expect(labels.templates.createCalendarItem).toBe('Přidat položku');
        expect(labels.templates.suspendMember).toBe('Ukončit členství');
        expect(labels.templates.resumeMember).toBe('Reaktivovat');
        expect(labels.templates.updateMember).toBe('Upravit profil');
        expect(labels.templates.registerMember).toBe('Registrovat člena');
    });

    it('has field labels', () => {
        expect(labels.fields.coordinators).toBe('Vedoucí');
        expect((labels.fields as Record<string, string>).category).toBe('Kategorie');
        expect(labels.fields.eventDate).toBe('Datum konání');
        expect(labels.fields.location).toBe('Místo');
        expect(labels.fields.name).toBe('Název');
        expect(labels.fields.organizer).toBe('Pořadatel');
        expect(labels.fields.websiteUrl).toBe('Webová stránka');
        expect(labels.fields.firstName).toBe('Jméno');
        expect(labels.fields.lastName).toBe('Příjmení');
        expect(labels.fields.email).toBe('E-mail');
        expect(labels.fields.phone).toBe('Telefon');
        expect(labels.fields.birthDate).toBe('Datum narození');
        expect(labels.fields.gender).toBe('Pohlaví');
        expect(labels.fields.nationality).toBe('Státní příslušnost');
        expect(labels.fields.birthNumber).toBe('Rodné číslo');
        expect(labels.fields.registrationNumber).toBe('Registrační číslo');
        expect(labels.fields.chipNumber).toBe('Číslo čipu');
        expect(labels.fields.bankAccountNumber).toBe('Číslo bankovního účtu');
        expect(labels.fields.dietaryRestrictions).toBe('Stravovací omezení');
    });

    it('has enum labels for eventStatus', () => {
        expect(labels.enums.eventStatus.ACTIVE).toBe('Aktivní');
        expect(labels.enums.eventStatus.FINISHED).toBe('Ukončeno');
        expect(labels.enums.eventStatus.CANCELLED).toBe('Zrušeno');
        expect(labels.enums.eventStatus.DRAFT).toBe('Koncept');
    });

    it('has enum labels for memberStatus', () => {
        expect(labels.enums.memberStatus.active).toBe('Aktivní');
        expect(labels.enums.memberStatus.inactive).toBe('Neaktivní');
    });

    it('has enum labels for gender', () => {
        expect(labels.enums.gender.MALE).toBe('Muž');
        expect(labels.enums.gender.FEMALE).toBe('Žena');
    });

    it('has enum labels for deactivationReason', () => {
        expect(labels.enums.deactivationReason.ODHLASKA).toBe('Odhlášení');
        expect(labels.enums.deactivationReason.PRESTUP).toBe('Přestup');
        expect(labels.enums.deactivationReason.OTHER).toBe('Jiný důvod');
    });

    it('has validation messages', () => {
        expect(labels.validation.requiredField).toBe('Povinné pole');
        expect(labels.validation.invalidEmail).toBe('Neplatný email');
        expect(labels.validation.mustBeNumber).toBe('Musí být číslo');
        expect(labels.validation.invalidFormat).toBe('Nespravny format');
    });

    it('has error labels', () => {
        expect(labels.errors.loadingMenu).toBe('Načítání menu...');
        expect(labels.errors.failedLoadMenu).toBe('Nepodařilo se načíst menu');
        expect(labels.errors.formDataLoadError).toBe('Nepodařilo se načíst data formuláře');
        expect(labels.errors.formValidationErrors).toBe('Chyby validace formuláře');
    });

    it('has ui labels', () => {
        expect(labels.ui.form).toBe('Formulář');
        expect(labels.ui.showRawJson).toBe('Zobrazit zdrojový JSON');
        expect(labels.ui.completeJson).toBe('Úplná JSON data položky');
        expect(labels.ui.availableActions).toBe('Dostupné odkazy');
        expect(labels.ui.availableForms).toBe('Dostupné formuláře');
        expect(labels.ui.collectionEmpty).toBe('Kolekce je prázdná');
        expect(labels.ui.loadingFormData).toBe('Načítání dat formuláře...');
    });

    it('has table headers', () => {
        expect(labels.tables.id).toBe('ID');
        expect(labels.tables.data).toBe('Údaje');
        expect(labels.tables.actions).toBe('Akce');
        expect(labels.tables.attribute).toBe('Atribut');
        expect(labels.tables.value).toBe('Hodnota');
        expect(labels.tables.items).toBe('Položky');
        expect(labels.tables.details).toBe('Detaily');
        expect(labels.tables.coordinator).toBe('Vedoucí');
    });

    it('has section labels', () => {
        expect(labels.sections.eventCoordination).toBe('VEDENÍ AKCE');
    });
});

describe('t()', () => {
    it('returns value from given category and key', () => {
        expect(t('nav', 'calendar')).toBe('Kalendář');
        expect(t('buttons', 'submit')).toBe('Odeslat');
        expect(t('tables', 'actions')).toBe('Akce');
    });
});

describe('getFieldLabel()', () => {
    it('returns label from central mapping when field name matches', () => {
        expect(getFieldLabel('firstName')).toBe('Jméno');
        expect(getFieldLabel('eventDate')).toBe('Datum konání');
    });

    it('falls back to raw field name when no mapping exists', () => {
        expect(getFieldLabel('unknownField')).toBe('unknownField');
    });

    it('prefers formOverrides over central mapping', () => {
        const overrides = { eventDate: 'Datum konání akce', firstName: 'Křestní jméno' };
        expect(getFieldLabel('eventDate', overrides)).toBe('Datum konání akce');
        expect(getFieldLabel('firstName', overrides)).toBe('Křestní jméno');
    });

    it('falls back to central mapping when field not in overrides', () => {
        const overrides = { eventDate: 'Datum konání akce' };
        expect(getFieldLabel('firstName', overrides)).toBe('Jméno');
    });
});

describe('getTemplateLabel()', () => {
    it('returns Czech label for known template names', () => {
        expect(getTemplateLabel('cancelEvent')).toBe('Zrušit akci');
        expect(getTemplateLabel('registerForEvent')).toBe('Přihlásit se');
    });

    it('returns undefined when no mapping exists to allow caller fallback chain', () => {
        expect(getTemplateLabel('unknownTemplate')).toBeUndefined();
    });
});

describe('getNavLabel()', () => {
    it('returns Czech label for known nav rels', () => {
        expect(getNavLabel('calendar')).toBe('Kalendář');
        expect(getNavLabel('events')).toBe('Akce');
        expect(getNavLabel('members')).toBe('Členové');
    });

    it('falls back to raw rel name when no mapping exists', () => {
        expect(getNavLabel('unknownRel')).toBe('unknownRel');
    });
});

describe('getEnumLabel()', () => {
    it('returns Czech label for known enum values', () => {
        expect(getEnumLabel('eventStatus', 'ACTIVE')).toBe('Aktivní');
        expect(getEnumLabel('eventStatus', 'CANCELLED')).toBe('Zrušeno');
        expect(getEnumLabel('gender', 'MALE')).toBe('Muž');
        expect(getEnumLabel('deactivationReason', 'ODHLASKA')).toBe('Odhlášení');
    });

    it('falls back to raw value when enum type is unknown', () => {
        expect(getEnumLabel('unknownEnum', 'SOME_VALUE')).toBe('SOME_VALUE');
    });

    it('falls back to raw value when enum value is unknown within known type', () => {
        expect(getEnumLabel('eventStatus', 'UNKNOWN_STATUS')).toBe('UNKNOWN_STATUS');
    });
});

describe('czechPlural()', () => {
    it('returns the correct form on the 1/4/5 boundaries', () => {
        const forms = (n: number) => czechPlural(n, 'člen', 'členové', 'členů');
        expect(forms(0)).toBe('členů');
        expect(forms(1)).toBe('člen');
        expect(forms(2)).toBe('členové');
        expect(forms(4)).toBe('členové');
        expect(forms(5)).toBe('členů');
        expect(forms(12)).toBe('členů');
    });
});

describe('getPermissionInfo()', () => {
    it('labels MEMBERS:EDIT_PROFILE as "Úprava údajů člena"', () => {
        expect(getPermissionInfo('MEMBERS:EDIT_PROFILE')?.label).toBe('Úprava údajů člena');
    });
});

describe('labels.errorTypes membership fees', () => {
    const text = (type: keyof typeof labels.errorTypes, params: Record<string, unknown> = {}) =>
        labels.errorTypes[type](params);

    it('names the event type and ranking of a duplicate payment rule', () => {
        expect(text('/errors/duplicate-payment-rule', {eventTypeName: 'Závod', rankingLabel: 'Morava'}))
            .toBe('Pravidlo pro typ akce „Závod“ a žebříček „Morava“ již v této úrovni existuje.');
    });

    it.each([
        [{}],
        [{eventTypeName: 'Závod'}],
        [{rankingLabel: 'Morava'}],
    ])('uses the wording without names for a duplicate payment rule with params %j', (params) => {
        expect(text('/errors/duplicate-payment-rule', params))
            .toBe('Pravidlo pro tuto kombinaci typu akce a žebříčku již v této úrovni existuje.');
    });

    it('names the event type and ranking of a missing payment rule', () => {
        expect(text('/errors/payment-rule-not-found', {eventTypeName: 'Závod', rankingLabel: 'Morava'}))
            .toBe('Pravidlo pro typ akce „Závod“ a žebříček „Morava“ nebylo nalezeno.');
    });

    it('uses the wording without names for a missing payment rule', () => {
        expect(text('/errors/payment-rule-not-found'))
            .toBe('Pravidlo pro tuto kombinaci typu akce a žebříčku nebylo nalezeno.');
    });

    it('asks for the percentage when a percentage rule has no value', () => {
        expect(text('/errors/payment-rule-value-missing', {ruleType: 'PERCENTAGE'}))
            .toBe('Vyplňte procento spoluúčasti.');
    });

    it('asks for the fixed amount when a fixed amount rule has no value', () => {
        expect(text('/errors/payment-rule-value-missing', {ruleType: 'FIXED_AMOUNT'}))
            .toBe('Vyplňte fixní částku spoluúčasti.');
    });

    it('falls back to a general wording for an unknown rule type', () => {
        expect(text('/errors/payment-rule-value-missing'))
            .toBe('Vyplňte hodnotu pravidla spoluúčasti.');
    });

    it('states the invalid percentage', () => {
        expect(text('/errors/negative-payment-rule-percentage', {percent: -5}))
            .toBe('Procento spoluúčasti nesmí být záporné (zadáno -5 %).');
    });

    it.each([
        ['/errors/active-campaign-exists', 'Již existuje aktivní kampaň volby členského příspěvku. Nejprve ji uzavřete.'],
        ['/errors/campaign-closed', 'Kampaň je již uzavřena, změnu nelze provést.'],
        ['/errors/campaign-already-processed', 'Kampaň již byla zpracována, akci nelze zopakovat.'],
        ['/errors/snapshot-frozen', 'Úroveň je součástí vyhlášené kampaně a již ji nelze měnit.'],
        ['/errors/voting-closed', 'Hlasování v této kampani již skončilo.'],
        ['/errors/membership-fee-tier-not-found', 'Úroveň členského příspěvku nebyla nalezena.'],
        ['/errors/membership-fee-group-not-found', 'Skupina členských příspěvků nebyla nalezena.'],
    ] as const)('has Czech text for %s', (type, expected) => {
        expect(text(type)).toBe(expected);
    });

    it('names the year of a missing fee selection campaign', () => {
        expect(text('/errors/fee-selection-campaign-not-found', {year: 2026}))
            .toBe('Kampaň volby členského příspěvku pro rok 2026 nebyla nalezena.');
    });

    it('has a wording without year for a missing fee selection campaign', () => {
        expect(text('/errors/fee-selection-campaign-not-found'))
            .toBe('Kampaň volby členského příspěvku nebyla nalezena.');
    });
});


describe('labels.errorTypes members, events and calendar', () => {
    const text = (type: keyof typeof labels.errorTypes, params: Record<string, unknown> = {}) =>
        labels.errorTypes[type](params);

    it.each([
        ['/errors/member-not-found', 'Člen nebyl nalezen.'],
        ['/errors/legal-guardian-not-found', 'Zákonný zástupce nebyl nalezen.'],
        ['/errors/email-required', 'Vyplňte e-mail.'],
        ['/errors/phone-required', 'Vyplňte telefon.'],
        ['/errors/address-required', 'Vyplňte adresu.'],
        ['/errors/birth-number-required', 'Vyplňte rodné číslo.'],
        ['/errors/guardian-required', 'Nezletilý člen musí mít alespoň jednoho zákonného zástupce.'],
        ['/errors/birth-number-not-allowed', 'Rodné číslo lze zadat jen u členů z České republiky.'],
        ['/errors/invalid-birth-number', 'Rodné číslo není platné.'],
        ['/errors/invalid-bank-account-number', 'Číslo bankovního účtu není platné.'],
        ['/errors/invalid-email-address', 'E-mailová adresa není platná.'],
        ['/errors/invalid-phone-number', 'Telefonní číslo není platné.'],
        ['/errors/date-of-birth-in-future', 'Datum narození nesmí být v budoucnosti.'],
        ['/errors/medical-course-validity-not-after-completion', 'Platnost zdravotního kurzu musí být později než datum jeho absolvování.'],
        ['/errors/member-already-suspended', 'Člen je již pozastaven.'],
        ['/errors/member-already-active', 'Člen je již aktivní.'],
        ['/errors/suspension-blocked', 'Členství nelze pozastavit, dokud není vyřešeno vlastnictví skupin nebo dluh člena.'],
        ['/errors/adult-member-cannot-have-guardians', 'Zletilý člen nemůže mít zákonné zástupce.'],
        ['/errors/minor-cannot-take-over-guardian', 'Nezletilý člen nemůže převzít roli zákonného zástupce.'],
        ['/errors/account-activation-not-available', 'Aktivaci účtu nelze pro tohoto člena provést.'],
        ['/errors/guardian-not-found', 'Vybraný uživatel nemůže být zákonným zástupcem.'],
        ['/errors/legal-guardian-group-without-guardian', 'Skupina zákonných zástupců musí mít alespoň jednoho zákonného zástupce.'],
        ['/errors/only-minors-allowed', 'Tuto akci lze provést jen u nezletilých členů.'],
        ['/errors/event-not-found', 'Akce nebyla nalezena.'],
        ['/errors/discipline-not-found', 'Disciplína nebyla nalezena.'],
        ['/errors/event-type-not-found', 'Typ akce nebyl nalezen.'],
        ['/errors/category-preset-not-found', 'Předvolba kategorií nebyla nalezena.'],
        ['/errors/registration-not-found', 'Přihláška nebyla nalezena.'],
        ['/errors/coordinator-not-found', 'Koordinátor nebyl nalezen.'],
        ['/errors/member-registration-blocked', 'Přihlášení člena na akci není možné.'],
        ['/errors/duplicate-category-oris-id', 'ORIS ID kategorie je v akci uvedeno vícekrát.'],
        ['/errors/registration-deadline-after-event-date', 'Termín přihlášek nesmí být po datu konání akce.'],
        ['/errors/finished-event-cannot-be-updated', 'Ukončenou akci nelze upravovat.'],
        ['/errors/cancelled-event-cannot-be-updated', 'Zrušenou akci nelze upravovat.'],
        ['/errors/category-required', 'Vyberte kategorii.'],
        ['/errors/category-not-available', 'Vybraná kategorie není na této akci k dispozici.'],
        ['/errors/event-not-active-for-registration', 'Na tuto akci se nelze přihlásit, protože není aktivní.'],
        ['/errors/registration-deadline-passed', 'Termín přihlášek na tuto akci již vypršel.'],
        ['/errors/registration-closed-after-event-date', 'Přihlášky na akci jsou uzavřeny, protože akce již proběhla.'],
        ['/errors/si-card-rental-not-allowed', 'Půjčení SI čipu není u této akce možné.'],
        ['/errors/si-card-number-required', 'Vyplňte číslo SI čipu.'],
        ['/errors/unknown-event-category', 'Akce neobsahuje zadanou kategorii.'],
        ['/errors/accommodation-list-access-denied', 'K seznamu ubytování nemáte přístup.'],
        ['/errors/shared-accommodation-not-offered', 'Sdílené ubytování u této akce není nabízeno.'],
        ['/errors/calendar-item-not-found', 'Položka kalendáře nebyla nalezena.'],
        ['/errors/calendar-item-read-only', 'Tuto položku kalendáře nelze upravovat.'],
        ['/errors/calendar-item-end-before-start', 'Datum konce nesmí být před datem začátku.'],
    ] as const)('has a fixed text for %s', (type, expected) => {
        expect(text(type)).toBe(expected);
    });

    it.each([
        ['/errors/registration-number-already-in-use', {registrationNumber: 'ZBM9500'}, 'Registrační číslo ZBM9500 již používá jiný člen.'],
        ['/errors/registration-number-already-in-use', {}, 'Registrační číslo již používá jiný člen.'],
        ['/errors/legal-guardian-email-already-in-use', {email: 'a@b.cz'}, 'E-mail a@b.cz již používá jiný uživatel.'],
        ['/errors/legal-guardian-email-already-in-use', {}, 'Tento e-mail již používá jiný uživatel.'],
        ['/errors/oris-event-not-found', {orisId: 123}, 'Akce s ORIS ID 123 nebyla v ORISu nalezena.'],
        ['/errors/oris-event-not-found', {}, 'Akce nebyla v ORISu nalezena.'],
        ['/errors/oris-discipline-not-found', {orisId: 'SB'}, 'Disciplína s ORIS ID SB nebyla v ORISu nalezena.'],
        ['/errors/oris-discipline-not-found', {}, 'Disciplína nebyla v ORISu nalezena.'],
        ['/errors/duplicate-registration', {eventName: 'Závod'}, 'Na akci „Závod“ již existuje přihláška.'],
        ['/errors/duplicate-registration', {}, 'Na tuto akci již existuje přihláška.'],
        ['/errors/discipline-not-editable', {disciplineName: 'Sprint'}, 'Disciplínu „Sprint“ nelze upravit.'],
        ['/errors/discipline-not-editable', {}, 'Disciplínu nelze upravit.'],
        ['/errors/discipline-not-archived', {disciplineName: 'Sprint'}, 'Disciplína „Sprint“ není archivovaná.'],
        ['/errors/discipline-not-archived', {}, 'Disciplína není archivovaná.'],
        ['/errors/oris-discipline-already-mapped', {eventTypeName: 'Závod'}, 'Tato disciplína z ORISu je již přiřazena typu akce „Závod“.'],
        ['/errors/oris-discipline-already-mapped', {}, 'Tato disciplína z ORISu je již přiřazena jinému typu akce.'],
        ['/errors/event-type-in-use', {eventNames: ['A', 'B']}, 'Typ akce nelze smazat, používají ho akce: A, B.'],
        ['/errors/event-type-in-use', {}, 'Typ akce nelze smazat, protože ho používají akce.'],
        ['/errors/event-type-in-use', {eventNames: []}, 'Typ akce nelze smazat, protože ho používají akce.'],
        ['/errors/event-type-name-already-exists', {name: 'Závod'}, 'Typ akce s názvem „Závod“ již existuje.'],
        ['/errors/event-type-name-already-exists', {}, 'Typ akce s tímto názvem již existuje.'],
        ['/errors/event-sync-needs-resolution', {orisId: 7}, 'Synchronizace akce s ORIS ID 7 vyžaduje ruční vyřešení rozdílů.'],
        ['/errors/event-sync-needs-resolution', {}, 'Synchronizace akce vyžaduje ruční vyřešení rozdílů.'],
        ['/errors/duplicate-category-name', {categoryName: 'H21'}, 'Kategorie „H21“ je v akci uvedena vícekrát.'],
        ['/errors/duplicate-category-name', {}, 'Název kategorie je v akci uveden vícekrát.'],
        ['/errors/invalid-event-status-transition', {from: 'DRAFT', to: 'FINISHED'}, 'Stav akce nelze změnit z „Koncept“ na „Ukončeno“.'],
        ['/errors/invalid-event-status-transition', {from: 'CANCELLED', to: 'ACTIVE'}, 'Stav akce nelze změnit z „Zrušeno“ na „Aktivní“.'],
        ['/errors/invalid-event-status-transition', {from: 'X', to: 'ACTIVE'}, 'Stav akce nelze v tuto chvíli změnit.'],
        ['/errors/invalid-event-status-transition', {}, 'Stav akce nelze v tuto chvíli změnit.'],
        ['/errors/invalid-oris-registration-deadlines', {orisId: 9}, 'Termíny přihlášek akce s ORIS ID 9 v ORISu nejsou platné.'],
        ['/errors/invalid-oris-registration-deadlines', {}, 'Termíny přihlášek akce v ORISu nejsou platné.'],
        ['/errors/date-range-too-long', {maxDays: 1}, 'Zvolené období je příliš dlouhé, nejvýše může mít 1 den.'],
        ['/errors/date-range-too-long', {maxDays: 3}, 'Zvolené období je příliš dlouhé, nejvýše může mít 3 dny.'],
        ['/errors/date-range-too-long', {maxDays: 366}, 'Zvolené období je příliš dlouhé, nejvýše může mít 366 dnů.'],
        ['/errors/date-range-too-long', {}, 'Zvolené období je příliš dlouhé.'],
        ['/errors/date-range-too-long', {maxDays: 'abc'}, 'Zvolené období je příliš dlouhé.'],
        ['/errors/event-type-in-use', {eventNames: ['A', 5, null, 'B']}, 'Typ akce nelze smazat, používají ho akce: A, B.'],
    ] as const)('renders %s with params %j', (type, params, expected) => {
        expect(text(type, params)).toBe(expected);
    });
});

describe('labels.errorTypes finance, groups, users, sync and notifications', () => {
    const text = (type: keyof typeof labels.errorTypes, params: Record<string, unknown> = {}) =>
        labels.errorTypes[type](params).replace(/\s/g, ' ');
    const czk = (amount: number) => ({amount, currency: 'CZK'});

    it.each([
        ['/errors/group-not-found', 'Skupina nebyla nalezena.'],
        ['/errors/invitation-not-found', 'Pozvánka nebyla nalezena.'],
        ['/errors/user-not-found', 'Uživatel nebyl nalezen.'],
        ['/errors/cannot-remove-last-permission-manager', 'Nelze odebrat oprávnění správce — systém musí mít alespoň jednoho uživatele se správou oprávnění.'],
        ['/errors/admin-lockout', 'Změna by odebrala přístup ke správě oprávnění všem administrátorům. Alespoň jeden aktivní uživatel musí toto oprávnění zachovat.'],
        ['/errors/incorrect-current-password', 'Zadané aktuální heslo je nesprávné.'],
        ['/errors/passwords-do-not-match', 'Hesla se neshodují.'],
        ['/errors/invalid-token', 'Odkaz pro nastavení hesla není platný.'],
        ['/errors/token-expired', 'Platnost odkazu vypršela. Požádejte o nový odkaz.'],
        ['/errors/token-already-used', 'Tento odkaz již byl použit.'],
        ['/errors/insufficient-authority', 'K této akci nemáte dostatečné oprávnění.'],
        ['/errors/member-profile-required', 'Pro tuto akci musíte mít propojený členský profil.'],
        ['/errors/rate-limit-exceeded', 'Příliš mnoho požadavků. Zkuste to prosím za chvíli znovu.'],
        ['/errors/sync-record-not-in-conflict', 'Záznam synchronizace není v konfliktu.'],
        ['/errors/conflict-not-acknowledged', 'Konflikt je nejprve nutné potvrdit.'],
        ['/errors/sync-record-not-failed', 'Záznam synchronizace nemá stav selhání, nelze ho resetovat.'],
        ['/errors/sync-record-claimed', 'Záznam právě zpracovává jiná synchronizace. Zkuste to za chvíli znovu.'],
        ['/errors/sync-record-not-found', 'Záznam synchronizace nebyl nalezen.'],
        ['/errors/sync-record-not-enrolled', 'Záznam není zařazen do synchronizace.'],
        ['/errors/notification-device-not-found', 'Zařízení pro notifikace nebylo nalezeno.'],
    ] as const)('has a fixed text for %s', (type, expected) => {
        expect(text(type)).toBe(expected);
    });

    it.each([
        ['/errors/overdraft-limit-exceeded', {memberName: 'Jan Novák', chargeAmount: czk(500), currentBalance: czk(-200), overdraftLimit: czk(1000)},
            'Částku 500,00 Kč nelze strhnout z účtu člena Jan Novák, protože by byl překročen povolený debet 1 000,00 Kč (aktuální zůstatek -200,00 Kč).'],
        ['/errors/overdraft-limit-exceeded', {chargeAmount: czk(500), currentBalance: czk(-200), overdraftLimit: czk(1000)},
            'Částku 500,00 Kč nelze strhnout z účtu, protože by byl překročen povolený debet 1 000,00 Kč (aktuální zůstatek -200,00 Kč).'],
        ['/errors/overdraft-limit-exceeded', {overdraftLimit: czk(1000)}, 'Částku nelze strhnout z účtu, protože by byl překročen povolený debet 1 000,00 Kč.'],
        ['/errors/overdraft-limit-exceeded', {chargeAmount: 'x'}, 'Částku nelze strhnout z účtu, protože by byl překročen povolený debet.'],
        ['/errors/transaction-already-reversed', {memberName: 'Jan Novák', amount: czk(250)}, 'Transakce ve výši 250,00 Kč na účtu člena Jan Novák již byla stornována.'],
        ['/errors/transaction-already-reversed', {amount: czk(250)}, 'Transakce ve výši 250,00 Kč již byla stornována.'],
        ['/errors/transaction-already-reversed', {}, 'Transakce již byla stornována.'],
        ['/errors/member-account-not-found', {memberName: 'Jan Novák'}, 'Účet člena Jan Novák nebyl nalezen.'],
        ['/errors/member-account-not-found', {}, 'Účet člena nebyl nalezen.'],
        ['/errors/transaction-not-found', {memberName: 'Jan Novák'}, 'Transakce na účtu člena Jan Novák nebyla nalezena.'],
        ['/errors/transaction-not-found', {}, 'Transakce nebyla nalezena.'],
        ['/errors/cannot-remove-last-owner', {groupName: 'Trenéři', memberName: 'Jan Novák'}, 'Ze skupiny „Trenéři“ nelze odebrat člena Jan Novák, protože je jejím posledním vlastníkem.'],
        ['/errors/cannot-remove-last-owner', {groupName: 'Trenéři'}, 'Ze skupiny „Trenéři“ nelze odebrat posledního vlastníka.'],
        ['/errors/direct-member-addition-not-allowed', {groupName: 'Trenéři'}, 'Do skupiny „Trenéři“ nelze členy přidávat přímo, musí být pozváni.'],
        ['/errors/direct-member-addition-not-allowed', {}, 'Do této skupiny nelze členy přidávat přímo, musí být pozváni.'],
        ['/errors/owner-cannot-be-member', {groupName: 'Trenéři', memberName: 'Jan Novák'}, 'Člen Jan Novák je vlastníkem skupiny „Trenéři“ a nemůže být zároveň jejím členem.'],
        ['/errors/owner-cannot-be-member', {groupName: 'Trenéři'}, 'Vlastník skupiny „Trenéři“ nemůže být zároveň jejím členem.'],
        ['/errors/owner-cannot-be-member', {}, 'Vlastník skupiny nemůže být zároveň jejím členem.'],
        ['/errors/member-already-in-group', {groupName: 'Trenéři', memberName: 'Jan Novák'}, 'Člen Jan Novák již je ve skupině „Trenéři“.'],
        ['/errors/member-already-in-group', {groupName: 'Trenéři'}, 'Člen již je ve skupině „Trenéři“.'],
        ['/errors/member-not-in-group', {groupName: 'Trenéři', memberName: 'Jan Novák'}, 'Člen Jan Novák není ve skupině „Trenéři“.'],
        ['/errors/member-not-in-group', {}, 'Člen není v této skupině.'],
        ['/errors/group-ownership-required', {groupName: 'Trenéři'}, 'Tuto akci může provést jen vlastník skupiny „Trenéři“.'],
        ['/errors/group-ownership-required', {}, 'Tuto akci může provést jen vlastník skupiny.'],
        ['/errors/not-invited-member', {groupName: 'Trenéři'}, 'Do skupiny „Trenéři“ nejste pozváni.'],
        ['/errors/not-invited-member', {}, 'Do této skupiny nejste pozváni.'],
        ['/errors/cannot-invite-existing-member', {groupName: 'Trenéři', memberName: 'Jan Novák'}, 'Člen Jan Novák již je členem skupiny „Trenéři“, nelze ho pozvat.'],
        ['/errors/cannot-invite-existing-member', {}, 'Člen již je členem této skupiny, nelze ho pozvat.'],
        ['/errors/duplicate-pending-invitation', {groupName: 'Trenéři', memberName: 'Jan Novák'}, 'Člen Jan Novák již má do skupiny „Trenéři“ čekající pozvánku.'],
        ['/errors/duplicate-pending-invitation', {}, 'Člen již má do této skupiny čekající pozvánku.'],
        ['/errors/cannot-promote-non-member-to-owner', {groupName: 'Trenéři', memberName: 'Jan Novák'}, 'Člen Jan Novák není členem skupiny „Trenéři“, proto ho nelze povýšit na vlastníka.'],
        ['/errors/cannot-promote-non-member-to-owner', {}, 'Člen není členem této skupiny, proto ho nelze povýšit na vlastníka.'],
        ['/errors/invitation-not-cancellable', {invitationStatus: 'ACCEPTED'}, 'Pozvánku nelze zrušit, protože již byla přijata.'],
        ['/errors/invitation-not-cancellable', {invitationStatus: 'REJECTED'}, 'Pozvánku nelze zrušit, protože již byla odmítnuta.'],
        ['/errors/invitation-not-cancellable', {invitationStatus: 'CANCELLED'}, 'Pozvánku nelze zrušit, protože již byla zrušena.'],
        ['/errors/invitation-not-cancellable', {}, 'Pozvánku nelze zrušit.'],
        ['/errors/member-already-in-training-group', {groupName: 'Žáci', memberName: 'Jan Novák'}, 'Člen Jan Novák již trénuje v tréninkové skupině „Žáci“.'],
        ['/errors/member-already-in-training-group', {}, 'Člen již trénuje v jiné tréninkové skupině.'],
        ['/errors/trainer-required', {groupName: 'Žáci'}, 'Tréninková skupina „Žáci“ musí mít alespoň jednoho trenéra.'],
        ['/errors/trainer-required', {}, 'Tréninková skupina musí mít alespoň jednoho trenéra.'],
        ['/errors/invalid-age-range', {minAge: 10, maxAge: 8}, 'Maximální věk (8) nesmí být nižší než minimální věk (10).'],
        ['/errors/invalid-age-range', {minAge: 'x'}, 'Maximální věk nesmí být nižší než minimální věk.'],
        ['/errors/overlapping-age-range', {minAge: 10, maxAge: 12}, 'Věkové rozmezí 10–12 se překrývá s jinou tréninkovou skupinou.'],
        ['/errors/overlapping-age-range', {}, 'Věkové rozmezí se překrývá s jinou tréninkovou skupinou.'],
        ['/errors/invalid-authority', {authority: 'FOO:BAR'}, 'Oprávnění „FOO:BAR“ není platné.'],
        ['/errors/invalid-authority', {}, 'Zadané oprávnění není platné.'],
        ['/errors/password-requirements-not-met', {violations: ['TOO_SHORT', 'MISSING_UPPERCASE', 'MISSING_DIGIT'], minLength: 12}, 'Heslo musí mít alespoň 12 znaků, obsahovat velké písmeno a číslici.'],
        ['/errors/password-requirements-not-met', {violations: ['TOO_SHORT'], minLength: 4}, 'Heslo musí mít alespoň 4 znaky.'],
        ['/errors/password-requirements-not-met', {violations: ['TOO_SHORT']}, 'Heslo musí být delší.'],
        ['/errors/password-requirements-not-met', {violations: ['MISSING_UPPERCASE', 'MISSING_LOWERCASE', 'MISSING_DIGIT', 'MISSING_SPECIAL_CHARACTER']}, 'Heslo musí obsahovat velké písmeno, malé písmeno, číslici a speciální znak.'],
        ['/errors/password-requirements-not-met', {violations: ['MISSING_DIGIT', 'CONTAINS_FIRST_NAME', 'CONTAINS_LAST_NAME', 'CONTAINS_REGISTRATION_NUMBER']}, 'Heslo musí obsahovat číslici. Heslo nesmí obsahovat vaše jméno, vaše příjmení a vaše registrační číslo.'],
        ['/errors/password-requirements-not-met', {violations: ['REQUIRED', 'TOO_SHORT'], minLength: 12}, 'Heslo je povinné.'],
        ['/errors/password-requirements-not-met', {violations: ['UNKNOWN']}, 'Heslo nesplňuje požadavky na složitost.'],
        ['/errors/password-requirements-not-met', {}, 'Heslo nesplňuje požadavky na složitost.'],
        ['/errors/sync-record-needs-resolution', {syncStatus: 'CONFLICT'}, 'Záznam synchronizace je ve stavu „Konflikt“ a je třeba ho nejprve vyřešit.'],
        ['/errors/sync-record-needs-resolution', {syncStatus: 'X'}, 'Záznam synchronizace je třeba nejprve vyřešit.'],
        ['/errors/sync-record-needs-resolution', {}, 'Záznam synchronizace je třeba nejprve vyřešit.'],
        ['/errors/unsupported-resolution', {resolution: 'ACCEPT_DIVERGENCE'}, 'Způsob řešení „Ponechat obě verze“ není pro tento záznam podporován.'],
        ['/errors/unsupported-resolution', {}, 'Zvolený způsob řešení není pro tento záznam podporován.'],
    ] as const)('renders %s with params %j', (type, params, expected) => {
        expect(text(type, params)).toBe(expected);
    });
});
