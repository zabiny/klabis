import {describe, expect, it} from 'vitest';
import {getEnumLabel, getFieldLabel, getNavLabel, getTemplateLabel, labels, t,} from './labels';

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
        expect(labels.templates.finishEvent).toBe('Ukončit akci');
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
        expect(labels.fields.eventCoordinatorId).toBe('Koordinátor');
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
