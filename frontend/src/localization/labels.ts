import type {ErrorType} from '../api/apiError';

export function czechPlural(count: number, one: string, few: string, many: string): string {
    if (count === 1) return one;
    if (count >= 2 && count <= 4) return few;
    return many;
}

export type ErrorParams = Record<string, unknown>;

const UNEXPECTED_ERROR_TEXT = 'Došlo k neočekávané chybě. Zkuste to prosím znovu.';

const eventStatusNames = {
    ACTIVE: 'Aktivní',
    FINISHED: 'Ukončeno',
    CANCELLED: 'Zrušeno',
    DRAFT: 'Koncept',
} as const;

const nonEmptyString = (value: unknown): string | undefined =>
    typeof value === 'string' && value !== '' ? value : undefined;

const finiteNumber = (value: unknown): number | undefined =>
    typeof value === 'number' && Number.isFinite(value) ? value : undefined;

const formatMoney = (value: unknown): string | undefined => {
    if (typeof value !== 'object' || value === null) return undefined;
    const {amount, currency} = value as {amount?: unknown; currency?: unknown};
    const number = finiteNumber(amount);
    if (number === undefined || typeof currency !== 'string' || currency === '') return undefined;
    try {
        return new Intl.NumberFormat('cs-CZ', {style: 'currency', currency}).format(number);
    } catch {
        return `${number} ${currency}`;
    }
};

const syncStatusNames = {
    NEW: 'Nový',
    IN_SYNC: 'V synchronizaci',
    RETRYING: 'Opakovaný pokus',
    CONFLICT: 'Konflikt',
    FAILED: 'Selhalo',
    RETIRED: 'Ukončeno',
} as const;

const resolutionNames = {
    INWARD: 'Převzít z externího systému',
    OUTWARD: 'Zapsat do externího systému',
    ACCEPT_DIVERGENCE: 'Ponechat obě verze',
} as const;

const invitationStatusNames = {
    ACCEPTED: 'přijata',
    REJECTED: 'odmítnuta',
    CANCELLED: 'zrušena',
} as const;

const joinCzech = (items: string[]): string =>
    items.length <= 1 ? items.join('') : `${items.slice(0, -1).join(', ')} a ${items[items.length - 1]}`;

const passwordContainTexts: Record<string, string> = {
    MISSING_UPPERCASE: 'velké písmeno',
    MISSING_LOWERCASE: 'malé písmeno',
    MISSING_DIGIT: 'číslici',
    MISSING_SPECIAL_CHARACTER: 'speciální znak',
};

const passwordForbiddenTexts: Record<string, string> = {
    CONTAINS_FIRST_NAME: 'vaše jméno',
    CONTAINS_LAST_NAME: 'vaše příjmení',
    CONTAINS_REGISTRATION_NUMBER: 'vaše registrační číslo',
};

const describePasswordViolations = (violations: unknown, minLength: unknown): string => {
    const list = Array.isArray(violations) ? violations.filter((v): v is string => typeof v === 'string') : [];
    if (list.includes('REQUIRED')) return 'Heslo je povinné.';
    const length = finiteNumber(minLength);
    const musts: string[] = [];
    if (list.includes('TOO_SHORT')) {
        musts.push(length !== undefined
            ? `mít alespoň ${length} ${czechPlural(length, 'znak', 'znaky', 'znaků')}`
            : 'být delší');
    }
    const contains = list.filter((v) => Object.hasOwn(passwordContainTexts, v)).map((v) => passwordContainTexts[v]);
    if (contains.length > 0) musts.push(`obsahovat ${joinCzech(contains)}`);
    const forbidden = list.filter((v) => Object.hasOwn(passwordForbiddenTexts, v)).map((v) => passwordForbiddenTexts[v]);
    const sentences: string[] = [];
    if (musts.length > 0) sentences.push(`Heslo musí ${musts.join(', ')}.`);
    if (forbidden.length > 0) sentences.push(`Heslo nesmí obsahovat ${joinCzech(forbidden)}.`);
    return sentences.length > 0 ? sentences.join(' ') : 'Heslo nesplňuje požadavky na složitost.';
};

const syncEntityTypeNames = {
    EVENT: 'Akce',
    DISCIPLINE: 'Disciplína',
    MEMBER: 'Člen',
    EVENT_REGISTRATION: 'Přihláška na akci',
} as const;

const authorityName = (code: string): string | undefined => getPermissionInfo(code)?.label;

const errorTypes: Record<ErrorType, (params: ErrorParams) => string> = {
    '/errors/validation-failed': () => 'Zadané údaje nejsou platné. Zkontrolujte formulář a zkuste to znovu.',
    '/errors/invalid-request': () => 'Požadavek se nepodařilo zpracovat. Zkontrolujte zadané údaje a zkuste to znovu.',
    '/errors/unauthorized': () => 'Platnost přihlášení vypršela. Přihlaste se prosím znovu.',
    '/errors/forbidden': () => 'K této akci nemáte oprávnění.',
    '/errors/account-inactive': () => 'Váš účet je deaktivován. Kontaktujte prosím administrátora.',
    '/errors/not-found': () => 'Požadovaná položka nebyla nalezena.',
    '/errors/conflict': () => 'Akci nelze provést, protože je v rozporu s aktuálním stavem dat.',
    '/errors/concurrent-update': () => 'Data mezitím změnil někdo jiný. Načtěte stránku znovu a zopakujte změnu.',
    '/errors/unexpected': () => UNEXPECTED_ERROR_TEXT,
    '/errors/voting-deadline-not-in-future': () => 'Termín pro výběr musí být v budoucnosti.',
    '/errors/voting-deadline-in-past': () => 'Termín pro výběr nesmí být v minulosti.',
    '/errors/duplicate-payment-rule': ({eventTypeName, rankingLabel}) =>
        eventTypeName && rankingLabel
            ? `Pravidlo pro typ akce „${eventTypeName}“ a žebříček „${rankingLabel}“ již v této úrovni existuje.`
            : 'Pravidlo pro tuto kombinaci typu akce a žebříčku již v této úrovni existuje.',
    '/errors/payment-rule-not-found': ({eventTypeName, rankingLabel}) =>
        eventTypeName && rankingLabel
            ? `Pravidlo pro typ akce „${eventTypeName}“ a žebříček „${rankingLabel}“ nebylo nalezeno.`
            : 'Pravidlo pro tuto kombinaci typu akce a žebříčku nebylo nalezeno.',
    '/errors/payment-rule-value-missing': ({ruleType}) => {
        if (ruleType === 'PERCENTAGE') return 'Vyplňte procento spoluúčasti.';
        if (ruleType === 'FIXED_AMOUNT') return 'Vyplňte fixní částku spoluúčasti.';
        return 'Vyplňte hodnotu pravidla spoluúčasti.';
    },
    '/errors/negative-payment-rule-percentage': ({percent}) =>
        `Procento spoluúčasti nesmí být záporné (zadáno ${percent} %).`,
    '/errors/active-campaign-exists': () => 'Již existuje aktivní kampaň volby členského příspěvku. Nejprve ji uzavřete.',
    '/errors/campaign-closed': () => 'Kampaň je již uzavřena, změnu nelze provést.',
    '/errors/campaign-already-processed': () => 'Kampaň již byla zpracována, akci nelze zopakovat.',
    '/errors/snapshot-frozen': () => 'Úroveň je součástí vyhlášené kampaně a již ji nelze měnit.',
    '/errors/voting-closed': () => 'Hlasování v této kampani již skončilo.',
    '/errors/membership-fee-tier-not-found': () => 'Úroveň členského příspěvku nebyla nalezena.',
    '/errors/membership-fee-group-not-found': () => 'Skupina členských příspěvků nebyla nalezena.',
    '/errors/fee-selection-campaign-not-found': ({year}) =>
        year !== undefined && year !== null
            ? `Kampaň volby členského příspěvku pro rok ${year} nebyla nalezena.`
            : 'Kampaň volby členského příspěvku nebyla nalezena.',
    '/errors/member-not-found': () => 'Člen nebyl nalezen.',
    '/errors/legal-guardian-not-found': () => 'Zákonný zástupce nebyl nalezen.',
    '/errors/email-required': () => 'Vyplňte e-mail.',
    '/errors/phone-required': () => 'Vyplňte telefon.',
    '/errors/address-required': () => 'Vyplňte adresu.',
    '/errors/birth-number-required': () => 'Vyplňte rodné číslo.',
    '/errors/guardian-required': () => 'Nezletilý člen musí mít alespoň jednoho zákonného zástupce.',
    '/errors/birth-number-not-allowed': () => 'Rodné číslo lze zadat jen u členů z České republiky.',
    '/errors/invalid-birth-number': () => 'Rodné číslo není platné.',
    '/errors/invalid-bank-account-number': () => 'Číslo bankovního účtu není platné.',
    '/errors/invalid-email-address': () => 'E-mailová adresa není platná.',
    '/errors/invalid-phone-number': () => 'Telefonní číslo není platné.',
    '/errors/date-of-birth-in-future': () => 'Datum narození nesmí být v budoucnosti.',
    '/errors/medical-course-validity-not-after-completion': () =>
        'Platnost zdravotního kurzu musí být později než datum jeho absolvování.',
    '/errors/member-already-suspended': () => 'Člen je již pozastaven.',
    '/errors/member-already-active': () => 'Člen je již aktivní.',
    '/errors/suspension-blocked': () => 'Členství nelze pozastavit, dokud není vyřešeno vlastnictví skupin nebo dluh člena.',
    '/errors/adult-member-cannot-have-guardians': () => 'Zletilý člen nemůže mít zákonné zástupce.',
    '/errors/minor-cannot-take-over-guardian': () => 'Nezletilý člen nemůže převzít roli zákonného zástupce.',
    '/errors/registration-number-already-in-use': ({registrationNumber}) =>
        registrationNumber
            ? `Registrační číslo ${registrationNumber} již používá jiný člen.`
            : 'Registrační číslo již používá jiný člen.',
    '/errors/account-activation-not-available': () => 'Aktivaci účtu nelze pro tohoto člena provést.',
    '/errors/legal-guardian-email-already-in-use': ({email}) =>
        email
            ? `E-mail ${email} již používá jiný uživatel.`
            : 'Tento e-mail již používá jiný uživatel.',
    '/errors/guardian-not-found': () => 'Vybraný uživatel nemůže být zákonným zástupcem.',
    '/errors/legal-guardian-group-without-guardian': () => 'Skupina zákonných zástupců musí mít alespoň jednoho zákonného zástupce.',
    '/errors/only-minors-allowed': () => 'Tuto akci lze provést jen u nezletilých členů.',
    '/errors/event-not-found': () => 'Akce nebyla nalezena.',
    '/errors/oris-event-not-found': ({orisId}) =>
        orisId !== undefined && orisId !== null
            ? `Akce s ORIS ID ${orisId} nebyla v ORISu nalezena.`
            : 'Akce nebyla v ORISu nalezena.',
    '/errors/discipline-not-found': () => 'Disciplína nebyla nalezena.',
    '/errors/oris-discipline-not-found': ({orisId}) =>
        orisId !== undefined && orisId !== null
            ? `Disciplína s ORIS ID ${orisId} nebyla v ORISu nalezena.`
            : 'Disciplína nebyla v ORISu nalezena.',
    '/errors/event-type-not-found': () => 'Typ akce nebyl nalezen.',
    '/errors/category-preset-not-found': () => 'Předvolba kategorií nebyla nalezena.',
    '/errors/registration-not-found': () => 'Přihláška nebyla nalezena.',
    '/errors/coordinator-not-found': () => 'Koordinátor nebyl nalezen.',
    '/errors/duplicate-registration': ({eventName}) =>
        eventName
            ? `Na akci „${eventName}“ již existuje přihláška.`
            : 'Na tuto akci již existuje přihláška.',
    '/errors/discipline-not-editable': ({disciplineName}) =>
        disciplineName
            ? `Disciplínu „${disciplineName}“ nelze upravit.`
            : 'Disciplínu nelze upravit.',
    '/errors/discipline-not-archived': ({disciplineName}) =>
        disciplineName
            ? `Disciplína „${disciplineName}“ není archivovaná.`
            : 'Disciplína není archivovaná.',
    '/errors/oris-discipline-already-mapped': ({eventTypeName}) =>
        eventTypeName
            ? `Tato disciplína z ORISu je již přiřazena typu akce „${eventTypeName}“.`
            : 'Tato disciplína z ORISu je již přiřazena jinému typu akce.',
    '/errors/event-type-in-use': ({eventNames}) => {
        const names = Array.isArray(eventNames) ? eventNames.filter((n): n is string => typeof n === 'string' && n !== '') : [];
        return names.length > 0
            ? `Typ akce nelze smazat, používají ho akce: ${names.join(', ')}.`
            : 'Typ akce nelze smazat, protože ho používají akce.';
    },
    '/errors/event-type-name-already-exists': ({name}) =>
        name
            ? `Typ akce s názvem „${name}“ již existuje.`
            : 'Typ akce s tímto názvem již existuje.',
    '/errors/event-sync-needs-resolution': ({orisId}) =>
        orisId !== undefined && orisId !== null
            ? `Synchronizace akce s ORIS ID ${orisId} vyžaduje ruční vyřešení rozdílů.`
            : 'Synchronizace akce vyžaduje ruční vyřešení rozdílů.',
    '/errors/member-registration-blocked': () => 'Přihlášení člena na akci není možné.',
    '/errors/duplicate-category-name': ({categoryName}) =>
        categoryName
            ? `Kategorie „${categoryName}“ je v akci uvedena vícekrát.`
            : 'Název kategorie je v akci uveden vícekrát.',
    '/errors/duplicate-category-oris-id': () => 'ORIS ID kategorie je v akci uvedeno vícekrát.',
    '/errors/registration-deadline-after-event-date': () => 'Termín přihlášek nesmí být po datu konání akce.',
    '/errors/finished-event-cannot-be-updated': () => 'Ukončenou akci nelze upravovat.',
    '/errors/cancelled-event-cannot-be-updated': () => 'Zrušenou akci nelze upravovat.',
    '/errors/category-required': () => 'Vyberte kategorii.',
    '/errors/category-not-available': () => 'Vybraná kategorie není na této akci k dispozici.',
    '/errors/event-not-active-for-registration': () => 'Na tuto akci se nelze přihlásit, protože není aktivní.',
    '/errors/registration-deadline-passed': () => 'Termín přihlášek na tuto akci již vypršel.',
    '/errors/registration-closed-after-event-date': () => 'Přihlášky na akci jsou uzavřeny, protože akce již proběhla.',
    '/errors/si-card-rental-not-allowed': () => 'Půjčení SI čipu není u této akce možné.',
    '/errors/si-card-number-required': () => 'Vyplňte číslo SI čipu.',
    '/errors/invalid-event-status-transition': ({from, to}) => {
        const fromName = typeof from === 'string' && Object.hasOwn(eventStatusNames, from) ? eventStatusNames[from as keyof typeof eventStatusNames] : undefined;
        const toName = typeof to === 'string' && Object.hasOwn(eventStatusNames, to) ? eventStatusNames[to as keyof typeof eventStatusNames] : undefined;
        return fromName && toName
            ? `Stav akce nelze změnit z „${fromName}“ na „${toName}“.`
            : 'Stav akce nelze v tuto chvíli změnit.';
    },
    '/errors/unknown-event-category': () => 'Akce neobsahuje zadanou kategorii.',
    '/errors/invalid-oris-registration-deadlines': ({orisId}) =>
        orisId !== undefined && orisId !== null
            ? `Termíny přihlášek akce s ORIS ID ${orisId} v ORISu nejsou platné.`
            : 'Termíny přihlášek akce v ORISu nejsou platné.',
    '/errors/accommodation-list-access-denied': () => 'K seznamu ubytování nemáte přístup.',
    '/errors/shared-accommodation-not-offered': () => 'Sdílené ubytování u této akce není nabízeno.',
    '/errors/calendar-item-not-found': () => 'Položka kalendáře nebyla nalezena.',
    '/errors/calendar-item-read-only': () => 'Tuto položku kalendáře nelze upravovat.',
    '/errors/calendar-item-end-before-start': () => 'Datum konce nesmí být před datem začátku.',
    '/errors/date-range-too-long': ({maxDays}) =>
        maxDays !== undefined && maxDays !== null && maxDays !== '' && Number.isFinite(Number(maxDays))
            ? `Zvolené období je příliš dlouhé, nejvýše může mít ${maxDays} ${czechPlural(Number(maxDays), 'den', 'dny', 'dnů')}.`
            : 'Zvolené období je příliš dlouhé.',
    '/errors/overdraft-limit-exceeded': ({memberName, chargeAmount, currentBalance, overdraftLimit}) => {
        const charge = formatMoney(chargeAmount);
        const balance = formatMoney(currentBalance);
        const limit = formatMoney(overdraftLimit);
        const member = nonEmptyString(memberName);
        const subject = member ? `Částku${charge ? ` ${charge}` : ''} nelze strhnout z účtu člena ${member}` : `Částku${charge ? ` ${charge}` : ''} nelze strhnout z účtu`;
        if (balance && limit) return `${subject}, protože by byl překročen povolený debet ${limit} (aktuální zůstatek ${balance}).`;
        if (limit) return `${subject}, protože by byl překročen povolený debet ${limit}.`;
        return `${subject}, protože by byl překročen povolený debet.`;
    },
    '/errors/transaction-already-reversed': ({memberName, amount}) => {
        const money = formatMoney(amount);
        const member = nonEmptyString(memberName);
        const transaction = money ? `Transakce ve výši ${money}` : 'Transakce';
        return member
            ? `${transaction} na účtu člena ${member} již byla stornována.`
            : `${transaction} již byla stornována.`;
    },
    '/errors/member-account-not-found': ({memberName}) =>
        nonEmptyString(memberName) ? `Účet člena ${memberName} nebyl nalezen.` : 'Účet člena nebyl nalezen.',
    '/errors/transaction-not-found': ({memberName}) =>
        nonEmptyString(memberName)
            ? `Transakce na účtu člena ${memberName} nebyla nalezena.`
            : 'Transakce nebyla nalezena.',
    '/errors/group-not-found': () => 'Skupina nebyla nalezena.',
    '/errors/cannot-remove-last-owner': ({groupName, memberName}) => {
        const group = nonEmptyString(groupName);
        const member = nonEmptyString(memberName);
        const where = group ? `Ze skupiny „${group}“` : 'Ze skupiny';
        return member
            ? `${where} nelze odebrat člena ${member}, protože je jejím posledním vlastníkem.`
            : `${where} nelze odebrat posledního vlastníka.`;
    },
    '/errors/direct-member-addition-not-allowed': ({groupName}) =>
        nonEmptyString(groupName)
            ? `Do skupiny „${groupName}“ nelze členy přidávat přímo, musí být pozváni.`
            : 'Do této skupiny nelze členy přidávat přímo, musí být pozváni.',
    '/errors/owner-cannot-be-member': ({groupName, memberName}) => {
        const group = nonEmptyString(groupName);
        const member = nonEmptyString(memberName);
        if (member) {
            return group
                ? `Člen ${member} je vlastníkem skupiny „${group}“ a nemůže být zároveň jejím členem.`
                : `Člen ${member} je vlastníkem skupiny a nemůže být zároveň jejím členem.`;
        }
        return group
            ? `Vlastník skupiny „${group}“ nemůže být zároveň jejím členem.`
            : 'Vlastník skupiny nemůže být zároveň jejím členem.';
    },
    '/errors/member-already-in-group': ({groupName, memberName}) => {
        const group = nonEmptyString(groupName);
        const member = nonEmptyString(memberName);
        const who = member ? `Člen ${member} již je` : 'Člen již je';
        return group ? `${who} ve skupině „${group}“.` : `${who} v této skupině.`;
    },
    '/errors/member-not-in-group': ({groupName, memberName}) => {
        const group = nonEmptyString(groupName);
        const member = nonEmptyString(memberName);
        const who = member ? `Člen ${member} není` : 'Člen není';
        return group ? `${who} ve skupině „${group}“.` : `${who} v této skupině.`;
    },
    '/errors/group-ownership-required': ({groupName}) =>
        nonEmptyString(groupName)
            ? `Tuto akci může provést jen vlastník skupiny „${groupName}“.`
            : 'Tuto akci může provést jen vlastník skupiny.',
    '/errors/not-invited-member': ({groupName}) =>
        nonEmptyString(groupName)
            ? `Do skupiny „${groupName}“ nejste pozváni.`
            : 'Do této skupiny nejste pozváni.',
    '/errors/cannot-invite-existing-member': ({groupName, memberName}) => {
        const group = nonEmptyString(groupName);
        const member = nonEmptyString(memberName);
        const who = member ? `Člen ${member} již je` : 'Člen již je';
        return group
            ? `${who} členem skupiny „${group}“, nelze ho pozvat.`
            : `${who} členem této skupiny, nelze ho pozvat.`;
    },
    '/errors/duplicate-pending-invitation': ({groupName, memberName}) => {
        const group = nonEmptyString(groupName);
        const member = nonEmptyString(memberName);
        const who = member ? `Člen ${member} již má` : 'Člen již má';
        return group
            ? `${who} do skupiny „${group}“ čekající pozvánku.`
            : `${who} do této skupiny čekající pozvánku.`;
    },
    '/errors/cannot-promote-non-member-to-owner': ({groupName, memberName}) => {
        const group = nonEmptyString(groupName);
        const member = nonEmptyString(memberName);
        const who = member ? `Člen ${member} není` : 'Člen není';
        return group
            ? `${who} členem skupiny „${group}“, proto ho nelze povýšit na vlastníka.`
            : `${who} členem této skupiny, proto ho nelze povýšit na vlastníka.`;
    },
    '/errors/invitation-not-cancellable': ({invitationStatus}) =>
        typeof invitationStatus === 'string' && Object.hasOwn(invitationStatusNames, invitationStatus)
            ? `Pozvánku nelze zrušit, protože již byla ${invitationStatusNames[invitationStatus as keyof typeof invitationStatusNames]}.`
            : 'Pozvánku nelze zrušit.',
    '/errors/invitation-not-found': () => 'Pozvánka nebyla nalezena.',
    '/errors/member-already-in-training-group': ({groupName, memberName}) => {
        const group = nonEmptyString(groupName);
        const member = nonEmptyString(memberName);
        const who = member ? `Člen ${member} již trénuje` : 'Člen již trénuje';
        return group
            ? `${who} v tréninkové skupině „${group}“.`
            : `${who} v jiné tréninkové skupině.`;
    },
    '/errors/trainer-required': ({groupName}) =>
        nonEmptyString(groupName)
            ? `Tréninková skupina „${groupName}“ musí mít alespoň jednoho trenéra.`
            : 'Tréninková skupina musí mít alespoň jednoho trenéra.',
    '/errors/invalid-age-range': ({minAge, maxAge}) => {
        const min = finiteNumber(minAge);
        const max = finiteNumber(maxAge);
        return min !== undefined && max !== undefined
            ? `Maximální věk (${max}) nesmí být nižší než minimální věk (${min}).`
            : 'Maximální věk nesmí být nižší než minimální věk.';
    },
    '/errors/overlapping-age-range': ({minAge, maxAge}) => {
        const min = finiteNumber(minAge);
        const max = finiteNumber(maxAge);
        return min !== undefined && max !== undefined
            ? `Věkové rozmezí ${min}–${max} se překrývá s jinou tréninkovou skupinou.`
            : 'Věkové rozmezí se překrývá s jinou tréninkovou skupinou.';
    },
    '/errors/user-not-found': () => 'Uživatel nebyl nalezen.',
    '/errors/cannot-remove-last-permission-manager': () =>
        'Nelze odebrat oprávnění správce — systém musí mít alespoň jednoho uživatele se správou oprávnění.',
    '/errors/admin-lockout': () =>
        'Změna by odebrala přístup ke správě oprávnění všem administrátorům. Alespoň jeden aktivní uživatel musí toto oprávnění zachovat.',
    '/errors/invalid-authority': ({authority}) =>
        nonEmptyString(authority) ? `Oprávnění „${authority}“ není platné.` : 'Zadané oprávnění není platné.',
    '/errors/incorrect-current-password': () => 'Zadané aktuální heslo je nesprávné.',
    '/errors/passwords-do-not-match': () => 'Hesla se neshodují.',
    '/errors/password-requirements-not-met': ({violations, minLength}) =>
        describePasswordViolations(violations, minLength),
    '/errors/invalid-token': () => 'Odkaz pro nastavení hesla není platný.',
    '/errors/token-expired': () => 'Platnost odkazu vypršela. Požádejte o nový odkaz.',
    '/errors/token-already-used': () => 'Tento odkaz již byl použit.',
    '/errors/insufficient-authority': () => 'K této akci nemáte dostatečné oprávnění.',
    '/errors/member-profile-required': () => 'Pro tuto akci musíte mít propojený členský profil.',
    '/errors/rate-limit-exceeded': () => 'Příliš mnoho požadavků. Zkuste to prosím za chvíli znovu.',
    '/errors/sync-record-needs-resolution': ({syncStatus}) =>
        typeof syncStatus === 'string' && Object.hasOwn(syncStatusNames, syncStatus)
            ? `Záznam synchronizace je ve stavu „${syncStatusNames[syncStatus as keyof typeof syncStatusNames]}“ a je třeba ho nejprve vyřešit.`
            : 'Záznam synchronizace je třeba nejprve vyřešit.',
    '/errors/sync-record-not-in-conflict': () => 'Záznam synchronizace není v konfliktu.',
    '/errors/conflict-not-acknowledged': () => 'Konflikt je nejprve nutné potvrdit.',
    '/errors/sync-record-not-failed': () => 'Záznam synchronizace nemá stav selhání, nelze ho resetovat.',
    '/errors/sync-record-claimed': () => 'Záznam právě zpracovává jiná synchronizace. Zkuste to za chvíli znovu.',
    '/errors/unsupported-resolution': ({resolution}) =>
        typeof resolution === 'string' && Object.hasOwn(resolutionNames, resolution)
            ? `Způsob řešení „${resolutionNames[resolution as keyof typeof resolutionNames]}“ není pro tento záznam podporován.`
            : 'Zvolený způsob řešení není pro tento záznam podporován.',
    '/errors/sync-record-not-found': () => 'Záznam synchronizace nebyl nalezen.',
    '/errors/sync-record-not-enrolled': () => 'Záznam není zařazen do synchronizace.',
    '/errors/notification-device-not-found': () => 'Zařízení pro notifikace nebylo nalezeno.',
    '/errors/field-authorization': () => 'Toto pole nemáte oprávnění měnit.',
    '/errors/authority-not-grantable-over-all': ({authorities}) => {
        const names = Array.isArray(authorities)
            ? authorities
                .map((a) => (typeof a === 'string' ? authorityName(a) : undefined))
                .filter((n): n is string => n !== undefined)
            : [];
        return names.length > 0
            ? `Oprávnění ${names.map((n) => `„${n}“`).join(', ')} nelze předat všem členům skupiny.`
            : 'Vybraná oprávnění nelze předat všem členům skupiny.';
    },
    '/errors/invalid-delegated-authority': ({authority}) => {
        const name = typeof authority === 'string' ? authorityName(authority) : undefined;
        return name
            ? `Oprávnění „${name}“ nelze skupině delegovat.`
            : 'Zvolené oprávnění nelze skupině delegovat.';
    },
    '/errors/unknown-sync-entity-type': ({entityType, externalSystem}) => {
        const entity = typeof entityType === 'string' && Object.hasOwn(syncEntityTypeNames, entityType)
            ? syncEntityTypeNames[entityType as keyof typeof syncEntityTypeNames]
            : undefined;
        const system = nonEmptyString(externalSystem);
        if (entity && system) return `Synchronizace typu „${entity}“ se systémem ${system} není podporována.`;
        if (entity) return `Synchronizace typu „${entity}“ není podporována.`;
        return 'Tento typ záznamu nelze synchronizovat.';
    },
};

export const labels = {
    errorTypes,

    nav: {
        home: 'Domů',
        calendar: 'Kalendář',
        events: 'Akce',
        members: 'Členové',
        groups: 'Skupiny',
        'training-groups': 'Tréninkové skupiny',
        legalGuardianGroups: 'Zákonní zástupci',
        'category-presets': 'Šablony kategorií',
        'event-types': 'Typy akcí',
        disciplines: 'Disciplíny',
        account: 'Finance',
        'membership-fee-tiers': 'Katalog tierů',
        'fee-selection-campaigns': 'Kampaň volby členského příspěvku',
        'membership-fees': 'Členské příspěvky',
        admin: 'Přehled administrace',
        syncOverview: 'Synchronizace',
    },

    buttons: {
        submit: 'Odeslat',
        submitting: 'Odesílám...',
        importing: 'Importuji...',
        cancel: 'Zrušit',
        close: 'Zavřít',
        retry: 'Zkusit znovu',
        logout: 'Odhlásit',
        unregister: 'Odhlásit',
        edit: 'Upravit',
        save: 'Uložit změny',
        saveChanges: 'Uložit změny',
        print: 'Tisknout',
        downloadCsv: 'Stáhnout CSV',
        accommodationList: 'Seznam pro ubytování',
        savePermissions: 'Uložit oprávnění',
        sendRequest: 'Odeslat žádost',
        activateAccount: 'Aktivovat účet',
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
        discoverOrisRegistrations: 'Načíst přihlášky z ORIS',
        editRegistration: 'Upravit přihlášku',
        showRegistrationDetail: 'Detail',
        updateEvent: 'Upravit',
        registerForEvent: 'Přihlásit se',
        publishEvent: 'Publikovat',
        createEvent: 'Přidat akci',
        importEvent: 'Importovat z ORIS',
        syncAllUpcomingFromOris: 'Synchronizovat všechny budoucí z ORIS',
        createCalendarItem: 'Přidat položku',
        suspendMember: 'Ukončit členství',
        resumeMember: 'Reaktivovat',
        sendMemberAccountActivation: 'Založit účet',
        updateMember: 'Upravit profil',
        registerMember: 'Registrovat člena',
        importFromOris: 'Synchronizovat s ORIS',
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
        addOwner: 'Přidat správce',
        removeOwner: 'Odebrat správce',
        addGroupOwner: 'Přidat správce',
        removeGroupOwner: 'Odebrat správce',
        removeGroupOwnerLeavesGroup: 'Odebráním správce dotčená osoba opustí skupinu.',
        removeSelfAsOwnerLeavesGroup: 'Odebráním sebe ze správců opustíte skupinu.',
        leaveGroup: 'Opustit skupinu',
        leaveGroupConfirm: 'Opuštěním skupiny ztratíte členství ve skupině.',
        addTrainer: 'Přidat trenéra',
        removeTrainer: 'Odebrat trenéra',
        setLegalGuardianGroupGuardians: 'Upravit zástupce',
        setMemberLegalGuardians: 'Upravit zástupce',
        updateLegalGuardian: 'Upravit profil',
        cancelInvitation: 'Zrušit pozvánku',
        createCategoryPreset: 'Přidat šablonu',
        updateCategoryPreset: 'Upravit šablonu',
        deleteCategoryPreset: 'Smazat šablonu',
        createEventType: 'Přidat typ akce',
        updateEventType: 'Upravit typ akce',
        deleteEventType: 'Smazat typ akce',
        deposit: 'Vložit',
        charge: 'Strhnout',
        reverse: 'Stornovat',
        createTier: 'Přidat tier',
        editTier: 'Upravit tier',
        deleteTier: 'Smazat tier',
        updateMembershipFeeTier: 'Upravit tier',
        deleteMembershipFeeTier: 'Smazat tier',
        addRule: 'Přidat pravidlo',
        editRule: 'Upravit pravidlo',
        removeRule: 'Smazat pravidlo',
        publishYear: 'Vypsat kampaň',
        changeDeadline: 'Změnit deadline',
        closeCampaign: 'Uzavřít kampaň',
        editSnapshot: 'Upravit skupinu',
        assignMember: 'Přiřadit člena',
        synchronizeNow: 'Synchronizovat',
        acknowledgeSyncConflict: 'Potvrdit konflikt',
        resolveSyncConflict: 'Vyřešit konflikt',
        resetSyncRecord: 'Resetovat synchronizaci',
    },

    dialogTitles: {
        selectCategoryPreset: 'Vybrat šablonu kategorií',
        publishEvent: 'Publikování akce',
        cancelEvent: 'Zrušení akce',
        registerForEvent: 'Přihlásit se na akci',
        unregisterFromEvent: 'Odhlásit se z akce',
        discoverOrisRegistrations: 'Načtení přihlášek z ORIS',
        editRegistration: 'Upravit přihlášku',
        registrationDetail: 'Detail přihlášky',
        suspendMember: 'Ukončení členství',
        resumeMember: 'Reaktivace člena',
        sendMemberAccountActivation: 'Založení účtu',
        importEvent: 'Import akce z ORIS',
        syncAllUpcomingFromOris: 'Hromadná synchronizace z ORIS',
        createEvent: 'Nová akce',
        registerMember: 'Registrace nového člena',
        importFromOris: 'Synchronizace členů s ORIS',
    },

    bulkSync: {
        progress: 'Synchronizuji...',
        successCount: (n: number) => `${n} úspěšně synchronizováno`,
        failureCount: (n: number) => `${n} chyb`,
        failuresHeading: 'Chyby:',
    },

    syncOverview: {
        title: 'Synchronizace',
        tabAttempts: 'Všechny pokusy',
        tabAttention: 'Vyžaduje pozornost',
        attentionEmpty: 'Nic nevyžaduje pozornost.',
        empty: 'Zatím nebyl zaznamenán žádný pokus o synchronizaci.',
        emptyFiltered: 'Žádný pokus neodpovídá zvoleným filtrům.',
        filters: {
            entityType: 'Druh entity',
            outcome: 'Výsledek',
            dateFrom: 'Datum od',
            dateTo: 'Datum do',
            all: 'Vše',
            includeUnchanged: 'Zobrazit i pokusy bez změny',
        },
        columns: {
            startedAt: 'Kdy',
            entity: 'Entita',
            entityType: 'Druh entity',
            trigger: 'Spouštěč',
            direction: 'Směr',
            outcome: 'Výsledek',
            failureReason: 'Důvod selhání',
            performedBy: 'Provedl',
            status: 'Stav',
            currentStatus: 'Aktuální stav',
            lastSuccessfulSyncAt: 'Poslední úspěšná synchronizace',
            failedAttempts: 'Selhaných pokusů',
        },
    },

    sync: {
        neverSynced: 'Nikdy synchronizováno',
        overlayTitle: 'Synchronizace',
        sectionLastSync: 'Poslední úspěšná synchronizace',
        sectionManagerDetails: 'Detaily správy',
        externalId: 'Externí ID',
        direction: 'Směr',
        directionInward: 'Do Klabisu',
        directionOutward: 'Ven z Klabisu',
        nextAttemptDueAt: 'Plánovaný další pokus',
        failedAttemptsSinceLastSuccess: 'Počet neúspěšných pokusů',
        divergenceHeading: 'Rozdílná pole',
        divergenceField: 'Pole',
        divergenceLocal: 'Lokální',
        divergenceExternal: 'Externí',
        divergenceBaseline: 'Výchozí',
        divergenceSide: 'Změněno',
        divergenceSideLocal: 'lokálně',
        divergenceSideExternal: 'externě',
        divergenceSideBoth: 'oboustranně',
        actionsHeading: 'Akce',
        historyHeading: 'Historie pokusů',
        historyEmpty: 'V rámci doby uchování nejsou zaznamenány žádné pokusy.',
    },

    orisImport: {
        dialogTitle: 'Importovat z ORIS',
        selectAll: 'Vybrat vše',
        selectedOf: (selected: number, total: number) => `Vybráno ${selected} z ${total} závodů`,
        showOnlySelected: 'Zobrazit jen vybrané',
        clearSelection: 'Zrušit výběr',
        shownOf: (shown: number, total: number) => `zobrazeno ${shown} z ${total}`,
        importSelected: (n: number) => `Importovat vybrané (${n})`,
        loadingMessage: 'Komunikuji s ORIS…',
        emptyHeading: 'Žádné závody k importu',
        emptyHint: 'Zkuste změnit oblast.',
        resultTitle: 'Výsledek importu',
        resultSummary: (success: number, total: number) => `Naimportováno ${success} z ${total}`,
        importFailed: 'Import se nezdařil. Zkuste to prosím znovu.',
        done: 'Hotovo',
        limitReachedHint: (limit: number) => `Můžete vybrat nejvýše ${limit} závodů.`,
    },

    fields: {
        legalGuardians: 'Zákonní zástupci',
        guardians: 'Zástupci',
        legalGuardianUserId: 'Zákonný zástupce',
        coordinators: 'Vedoucí',
        eventDate: 'Datum konání',
        registrationDeadline: 'Uzávěrka',
        deadlines: 'Uzávěrky přihlášek',
        cancellationReason: 'Důvod zrušení (volitelné)',
        category: 'Kategorie',
        categories: 'Kategorie',
        categoryFee: 'Startovné kategorie (volitelné)',
        location: 'Místo',
        name: 'Název',
        organizer: 'Pořadatel',
        websiteUrl: 'Webová stránka',
        firstName: 'Jméno',
        loginName: 'Přihlašovací číslo',
        lastName: 'Příjmení',
        email: 'E-mail',
        phone: 'Telefon',
        contact: 'Kontakt',
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
        userId: 'Uživatel',
        trainerId: 'Trenér',
        minAge: 'Min. věk',
        maxAge: 'Max. věk',
        memberCount: 'Počet členů',
        minorCount: 'Počet nezletilých',
        ageRange: 'Věkové rozmezí',
        reason: 'Důvod zrušení (volitelné)',
        delegatedAuthorities: 'Oprávnění vlastníků nad členy',
        color: 'Barva',
        sortOrder: 'Pořadí',
        eventTypeId: 'Typ závodu',
        disciplineIds: 'ORIS disciplíny',
        ranking: 'Žebříček',
        rankingLevelId: 'ID úrovně',
        rankingName: 'Název žebříčku',
        baseEntryFee: 'Startovné',
        sharedTransportEnabled: 'Nabídnout společnou dopravu',
        sharedAccommodationEnabled: 'Nabídnout společné ubytování',
        siUsage: 'Použití SI čipu',
        siRentalAllowed: 'Půjčení čipu povoleno',
        wantsToRentSi: 'Chci si půjčit čip',
        wantsSharedTransport: 'Chci využít společnou dopravu',
        wantsSharedAccommodation: 'Chci využít společné ubytování',
        internalNote: 'Interní poznámka',
        orisNote: 'Poznámka do ORIS',
        yearlyFeeAmount: 'Roční poplatek',
        yearlyFeeCurrency: 'Měna',
        tierIds: 'Tiery',
        levelIds: 'Tiery',
        votingDeadline: 'Uzávěrka hlasování',
        year: 'Rok',
        membershipFeeGroupId: 'Tier poplatku',
        groupStatus: 'Stav skupiny',
        ruleType: 'Typ pravidla',
        raceTypeId: 'Typ závodu',
        rules: 'Pravidla spoluúčasti',
        rankingShortName: 'Žebříček',
        percentage: 'Procento spoluúčasti (%)',
        percent: 'Procento (%)',
        fixedAmount: 'Fixní částka',
        fixedCurrency: 'Měna fixní částky',
        source: 'Zdroj přiřazení',
    },

    finance: {
        memberAccountPageTitle: 'Finance',
        balance: 'Zůstatek',
        transactionHistory: 'Historie transakcí',
        noTransactions: 'Žádné transakce',
        deposit: 'Vložit',
        charge: 'Strhnout',
        reverse: 'Stornovat',
        confirmReverse: 'Potvrdit storno',
        cancelReverse: 'Zrušit',
        reverseConfirmTitle: 'Stornovat transakci',
        reverseConfirmDescription: 'Storno vytvoří novou transakci opačného znamínka. Tuto akci nelze vrátit zpět.',
        date: 'Datum',
        type: 'Typ',
        amount: 'Částka',
        description: 'Popis',
        reversed: 'Stornováno',
        reversal: 'Storno',
        navigateToReversal: 'Zobrazit storno',
        navigateToOriginal: 'Zobrazit původní transakci',
        typeDeposit: 'Vklad',
        typeOther: 'Výdaj',
        filterDateFrom: 'Datum od',
        filterDateTo: 'Datum do',
        filterType: 'Typ',
        filterTypeAll: 'Vše',
        currency: 'Kč',
        openMemberAccount: 'Účet',
        transactionDialogTitle: 'Vložit / Vybrat',
        tabDeposit: 'Připsání vkladu',
        tabCharge: 'Stržení částky',
        openTransactionDialogAriaLabel: 'Provést finanční transakci',
        amountRequired: 'Částka *',
        note: 'Poznámka',
        notePlaceholder: 'Volitelná poznámka...',
        noOperationAllowed: 'Žádná operace není povolena',
        recordedBy: 'Zaznamenal',
    },

    enums: {
        syncTrigger: {
            SCHEDULED: 'Plánovaný',
            LOCAL_CHANGE: 'Lokální změna',
            MANUAL: 'Ruční',
        },
        syncDirection: {
            INWARD: 'Do Klabisu',
            OUTWARD: 'Ven z Klabisu',
        },
        syncOutcome: {
            SUCCESS: 'Úspěch',
            CONFLICT: 'Konflikt',
            FAILED: 'Selhalo',
            OUTAGE: 'Výpadek',
            RESET: 'Resetováno',
            SKIPPED: 'Přeskočeno',
        },
        syncEntityType: {
            events: 'Akce',
            disciplines: 'Disciplína',
            members: 'Člen',
            'event-registrations': 'Přihláška na akci',
        },
        eventStatus: eventStatusNames,
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
        resolution: resolutionNames,
        siUsage: {
            NOT_USED: 'Nepoužívá se',
            OPTIONAL: 'Volitelný',
            REQUIRED: 'Povinný',
        },
        feeGroupStatus: {
            EDITABLE: 'Editovatelná',
            FROZEN: 'Zmrazená',
        },
        coParticipationRuleType: {
            PERCENTAGE: 'Procento',
            FIXED_AMOUNT: 'Fixní částka',
        },
        feeMemberSource: {
            MEMBER_CHOICE: 'Vlastní volba',
            ADMIN_ASSIGNMENT: 'Přiřazeno adminem',
        },
        syncStatus: syncStatusNames,
        missingDataItem: {
            EMAIL: 'e-mail',
            PHONE: 'telefon',
            BIRTH_NUMBER: 'rodné číslo',
            GUARDIAN: 'zákonný zástupce',
            ADDRESS: 'adresa',
        }
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
        loadDataFailed: 'Data se nepodařilo načíst.',
        unexpectedError: UNEXPECTED_ERROR_TEXT,
        savePermissionsFailed: 'Nepodařilo se uložit oprávnění. Zkuste to prosím znovu.',
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

    ui: {
        guardianContact: (guardianName?: string) =>
            guardianName ? `kontakt zákonného zástupce (${guardianName})` : 'kontakt zákonného zástupce',
        hide: 'Skrýt',
        reveal: 'Zobrazit',
        currentDeadline: 'aktuální',
        deadlineOrdinal: (n: number) => `${n}. termín:`,
        deadlineOrdinalShort: (n: number) => `${n}. termín`,
        notProvided: 'neuvedeno',
        yes: 'Ano',
        no: 'Ne',
        sharedTransportCount: (count: number) => `Společná doprava: ${count} ${czechPlural(count, 'člen', 'členové', 'členů')}`,
        sharedAccommodationCount: (count: number) => `Společné ubytování: ${count} ${czechPlural(count, 'člen', 'členové', 'členů')}`,
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
        noLegalGuardianGroups: 'Zatím neexistuje žádná skupina zákonných zástupců.',
        noMinorsInGroup: 'Skupina nemá žádné nezletilé.',
        noLegalGuardians: 'Bez zákonného zástupce',
        registrationMinorContactHint: 'Vlastní e-mail a telefon nezletilého jsou volitelné.',
        registrationGuardiansRequiredHint: 'Nezletilý člen musí mít alespoň jednoho zákonného zástupce.',
        registrationTakeoverHint: 'Pokud se dosavadní zákonný zástupce stává členem, vyberte ho; údaje se předvyplní z jeho profilu.',
        legalGuardianPickExisting: 'Vybrat existujícího zástupce',
        legalGuardianCreateNew: 'Založit nového zástupce',
        optionSearch: 'Hledat podle jména',
        clearSelection: 'Zrušit výběr',
        selectOption: 'Vyberte možnost',
        legalGuardianGroupMerged: 'Skupina byla sloučena s existující skupinou se stejnými zástupci.',
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
        mySchedule: 'Můj rozvrh',
        emailSent: 'Email byl odeslán',
        emailSentDescription: 'Pokud je váš účet stále čekající na aktivaci, obdržíte email s odkazem pro nastavení hesla. Zkontrolujte prosím svou schránku.',
        requestNewTokenTitle: 'Požádat o nový token',
        requestNewTokenDescription: 'Zadejte své registrační číslo a email. Odkaz pro nastavení hesla přijde pouze tehdy, pokud zadaný email odpovídá emailu vedenému v Klabisu u vás nebo u vašeho zákonného zástupce.',
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
        coordinator: 'Vedoucí',
        identityCardNumber: 'Číslo OP',
        identityCardValidityDate: 'Platnost OP',
        dateOfBirth: 'Datum narození',
        address: 'Adresa',
        eventType: 'Typ',
        sync: 'Synchronizace',
    },

    groupDelegation: {
        ownersMayPrefix: 'Vlastníci skupiny (i budoucí) mohou:',
        ownersGainNothing: 'Vlastníci skupiny nad členy nezískávají žádná oprávnění.',
        invitationGrantsPrefix: 'Přijetím pozvánky získají vlastníci skupiny (současní i budoucí) nad vámi oprávnění:',
        invitationGrantsNothing: 'Přijetím pozvánky nezískají vlastníci skupiny nad vámi žádná oprávnění.',
        fixedAtCreation: 'Nelze měnit po vytvoření skupiny.',
        noneDelegated: 'Žádná',
    },

    sections: {
        members: 'Členové',
        membersList: 'Seznam členů',
        events: 'Akce',
        eventsList: 'Seznam akcí',
        eventInfo: 'INFORMACE O AKCI',
        registrations: 'Přihlášky',
        deadlines: 'UZÁVĚRKY PŘIHLÁŠEK',
        accommodationList: 'Seznam pro ubytování',
        sharedServices: 'SPOLEČNÉ SLUŽBY',
        siPolicy: 'SI ČIPY',
        personalInfo: 'OSOBNÍ ÚDAJE',
        contact: 'KONTAKT',
        address: 'ADRESA',
        supplementary: 'DOPLŇKOVÉ INFORMACE',
        documentsAndLicenses: 'DOKLADY A LICENCE',
        deactivation: 'DEAKTIVACE',
        groups: 'Skupiny',
        groupsList: 'Moje skupiny',
        groupOwners: 'SPRÁVCI',
        groupMembers: 'ČLENOVÉ',
        pendingInvitations: 'ČEKAJÍCÍ POZVÁNKY',
        trainingGroupsList: 'Tréninkové skupiny',
        trainingGroupMembers: 'ČLENOVÉ',
        trainingGroupTrainers: 'TRENÉŘI',
        trainingGroup: 'TRÉNINKOVÁ SKUPINA',
        legalGuardianGroups: 'Zákonní zástupci',
        legalGuardians: 'ZÁKONNÍ ZÁSTUPCI',
        legalGuardianTakeover: 'PŘEVZETÍ ZÁKONNÉHO ZÁSTUPCE',
        legalGuardianGroupMinors: 'NEZLETILÍ',
        categoryPresetsList: 'Šablony kategorií',
        presetsListHeading: 'Seznam šablon',
        eventTypesList: 'Typy akcí',
        eventTypesListHeading: 'Seznam typů akcí',
        eventBasicInfo: 'ZÁKLADNÍ INFORMACE',
        eventCoordination: 'VEDENÍ AKCE',
        eventDeadlines: 'UZÁVĚRKY PŘIHLÁŠEK',
        eventCategories: 'KATEGORIE',
        membershipFeeTiersList: 'Katalog tierů',
        membershipFeeGroups: 'Skupiny',
        membershipFeeGroupMembers: 'ČLENOVÉ',
        feeSelectionCampaignsList: 'Kampaně volby členského příspěvku',
        activeCampaign: 'Aktivní kampaň',
        pastCampaigns: 'Minulé kampaně',
        coParticipationRules: 'PRAVIDLA SPOLUÚČASTI',
    },

    notifications: {
        pageTitle: 'Notifikace',
        devicesHeading: 'Zařízení s povolenými notifikacemi',
        noDevices: 'Zatím nemáte povolené notifikace na žádném zařízení.',
        currentDevice: 'Toto zařízení',
        enabledAt: 'Povoleno',
        enable: 'Povolit notifikace',
        enableOnThisDevice: 'Zapnout notifikace na tomto zařízení',
        disableOnThisDevice: 'Vypnout notifikace na tomto zařízení',
        disableFailed: 'Notifikace se nepodařilo vypnout. Zkuste to prosím znovu.',
        removeDevice: 'Odebrat',
        removeFailed: 'Zařízení se nepodařilo odebrat. Zkuste to prosím znovu.',
        enableFailed: 'Notifikace se nepodařilo zapnout. Zkuste to prosím znovu.',
        sendTest: 'Poslat testovací notifikaci',
        sendTestSuccess: 'Testovací notifikace byla odeslána.',
        sendTestFailed: 'Testovací notifikaci se nepodařilo odeslat. Zkuste to prosím znovu.',
        stateUnsupported: 'Tento prohlížeč nepodporuje push notifikace.',
        stateIosNeedsInstall: 'Na iPhonu a iPadu fungují notifikace jen u aplikace přidané na plochu. Otevřete v Safari nabídku Sdílet, zvolte „Přidat na plochu“ a aplikaci spusťte z plochy.',
        stateDenied: 'Notifikace jsou v tomto prohlížeči zablokované. Povolte je v nastavení prohlížeče u oprávnění webu a stránku načtěte znovu.',
        stateOff: 'Notifikace na tomto zařízení jsou vypnuté.',
        stateOn: 'Notifikace na tomto zařízení jsou zapnuté.',
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
        showAll: 'Zobrazit všechny',
        browseClubEvents: 'Prohlédnout nadcházející akce klubu',
        eventsPanelTitle: 'Akce',
        eventsPanelSubtitle: 'Všechny nadcházející akce',
        notificationsPanelTitle: 'Notifikace',
        notificationsPanelSubtitle: 'Nastavit upozornění na tomto zařízení',
        upcomingDeadlinesTitle: 'Končící přihlášky tento týden',
        deadlinePrefix: 'Uzávěrka:',
        registerForEvent: 'Přihlásit se',
        showAllDeadlines: 'Zobrazit všechny s končící uzávěrkou',
    },

    permissions: {
        'MEMBERS:MANAGE': {label: 'Správa členů', description: 'Registrace, úprava a mazání členů'},
        'MEMBERS:PERMISSIONS': {label: 'Správa oprávnění', description: 'Přidělování a odebírání oprávnění uživatelům'},
        'MEMBERS:EDIT_PROFILE': {label: 'Úprava údajů člena', description: 'Zobrazení a úprava údajů konkrétního člena kromě údajů vyhrazených správě'},
        'EVENTS:MANAGE': {label: 'Správa akcí', description: 'Vytváření a úprava akcí'},
        'CALENDAR:MANAGE': {label: 'Správa kalendáře', description: 'Vytváření a úprava kalendářních událostí'},
        'GROUPS:TRAINING': {label: 'Správa tréninkových skupin', description: 'Umožňuje vytvářet a spravovat tréninkové skupiny a jejich členy.'},
        'EVENTS:REGISTRATIONS': {label: 'Správa přihlášek', description: 'Editace přihlášek ostatních členů na akce'},
        'FINANCE:MANAGE': {label: 'Správa financí', description: 'Vklady, strhávání a storno transakcí na účtech členů'},
        'SYNC:MANAGE': {label: 'Správa synchronizace', description: 'Řešení konfliktů a řízení synchronizace s externími systémy'},
    },

    links: {
        trainingGroup: 'Tréninková skupina',
        legalGuardians: 'Zákonní zástupci',
    },

    events: {
        missingInOris: 'V ORISu nenalezena',
        registrationModal: {
            siChip: 'SI čip',
            siChipHelperPrefilled: 'Předvyplněno z vašeho profilu · 4–8 číslic',
            siChipHelper: '4–8 číslic',
            siChipRequired: 'Zadejte SI čip',
            siChipRequiredUnlessRenting: 'Zadejte SI čip, nebo zaškrtněte, že si čip chcete půjčit',
            rentSi: 'Chci si půjčit čip',
            siChipInvalidFormat: 'Zadejte 4–8 číslic',
            category: 'Kategorie',
            categoryRequired: 'Vyberte kategorii',
            savedWithTemplate: (what: string) => `${what} — úspěšně uloženo`,
            selectCategoryPlaceholder: 'Vyberte možnost',
            deadlinePrefix: (date: string) => `Přihlášky do ${date}`,
            confirmNew: 'Potvrdit přihlášku',
            confirmEdit: 'Uložit změny',
            prefillLoadError: 'Nepodařilo se načíst výchozí údaje přihlášky.',
            noRegistrationAffordance: 'Formulář se nepodařilo načíst – přihlášky pravděpodobně nejsou otevřené.',
        },
    },

    eventsFilter: {
        search: 'Hledat',
        searchPlaceholder: 'Hledat podle názvu nebo místa...',
        timeWindowLabel: 'Zobrazit',
        budouci: 'Budoucí',
        probehle: 'Proběhlé',
        vse: 'Vše',
        mojePřihlaskyLabel: 'Moje přihlášky',
        emptyState: 'Žádné akce neodpovídají zadaným filtrům.',
        eventTypeSelectPlaceholder: '— bez filtru —',
        eventTypeFilter: 'Typ akce',
        eventTypeAll: 'Vše',
        eventsFilterYear: 'Rok',
        noYear: '—',
        timeWindowDisabledTooltip: 'Není k dispozici pro jiný než aktuální rok',
    },

    membersFilter: {
        search: 'Hledat člena',
        searchPlaceholder: 'Hledat podle jména nebo registračního čísla',
        statusLabel: 'Stav členství',
        statusActive: 'Aktivní',
        statusInactive: 'Neaktivní',
        statusAll: 'Vše',
        emptyState: 'Žádní členové neodpovídají zadaným filtrům.',
        incompleteOnly: 'Jen neúplní',
        incompleteBadge: 'Neúplné údaje',
    },

    suspensionWarning: {
        title: 'Varování: Správce skupin',
        description: 'Člen je posledním správcem následujících skupin. Před ukončením členství je nutné určit nástupce nebo skupiny rozpustit.',
        designateSuccessor: 'Určit nástupce',
        dissolveGroup: 'Rozpustit skupinu',
        trainingGroupNote: 'U tréninkové skupiny je nutné určit nástupce.',
        groupTypeTrainingGroup: 'Tréninková skupina',
        groupTypeLegalGuardianGroup: 'Skupina zákonných zástupců',
        groupTypeFreeGroup: 'Volná skupina',
    },

    negativeBalanceWarning: {
        title: 'Záporný zůstatek',
        description: 'Pro ukončení členství musí být zůstatek nezáporný. Nejprve doplňte prostředky na účtu člena.',
        currentBalance: 'Aktuální zůstatek',
        openAccount: 'Otevřít účet',
        close: 'Zavřít',
    },

    changePassword: {
        sectionButtonLabel: 'Změnit heslo',
        dialogTitle: 'Změna hesla',
        currentPasswordLabel: 'Aktuální heslo',
        newPasswordLabel: 'Nové heslo',
        confirmPasswordLabel: 'Potvrzení nového hesla',
        submitButton: 'Změnit heslo',
    },

    accountActivation: {
        confirm: 'Na vlastní e-mail člena bude odeslán aktivační odkaz, přes který si vytvoří přístup do aplikace.',
        sent: 'Aktivační odkaz byl odeslán na e-mail člena.',
    },
    memberCompleteness: {
        missingDataWarning: (items: string) => `Chybí: ${items}`,
    },

    memberFee: {
        choicePageTitle: (year: number) => `Výběr tieru členského příspěvku pro rok ${year}`,
        sectionTitle: 'ČLENSKÝ PŘÍSPĚVEK',
        currentTier: 'Aktuální tier příspěvku',
        choiceTier: 'Výběr tieru příspěvku',
        deadlineExpired: 'Lhůta pro výběr uplynula',
        chooseTier: 'Zvolit tier',
        changeTier: 'Změnit tier',
        cancelChoice: 'Zrušit volbu',
        noChoiceMade: 'Výběr ještě nebyl proveden',
        viewHistory: 'Zobrazit historii příspěvků',
        feeTier: 'Tier členského příspěvku',
        annualFee: 'Roční poplatek',
        noChoiceWarning: 'Lhůta pro výběr příspěvku uplynula bez provedené volby.',
        choiceLockedInfo: 'Volba je uzamčena.',
        votingOpenChoiceExists: 'Máte zvolen tier příspěvku. Volbu je možné změnit.',
        votingOpenNoChoice: 'Probíhá volba tieru příspěvku. Zvolte si svůj tier.',
    },

    calendarFeed: {
        sectionTitle: 'KALENDÁŘOVÝ FEED',
        intro: 'Přihlaste se k odběru svých akcí v externím kalendáři.',
        createButton: 'Vytvořit kalendářový feed',
        regenerateButton: 'Vygenerovat nový token',
        copyButton: 'Kopírovat',
        copiedButton: 'Zkopírováno',
        generatedAt: 'Vygenerováno',
        confirmRegenerateTitle: 'Vygenerovat nový token?',
        confirmRegenerateMessage: 'Předchozí URL přestane fungovat. Všechna zařízení, která ji používají, se přestanou synchronizovat, dokud nezadáte novou URL.',
        confirmRegenerateAction: 'Vygenerovat',
        fullUrlLabel: 'URL kalendářového feedu',
        fullUrlNote: 'Tuto URL uvidíte pouze nyní. Uložte ji nebo přidejte do svého kalendáře. Pokud ji ztratíte, vygenerujte nový token.',
        maskedUrlLabel: 'Subscribe URL (zamaskovaná)',
        helpTitle: 'Jak přidat do kalendáře',
        helpText: 'Přidejte tuto URL do svého kalendáře — Google Calendar, Apple Calendar nebo Outlook. Kalendář se automaticky aktualizuje, jak budete přihlašovat nebo odhlašovat akce, nebo když vám organizátor přidá či odebere roli koordinátora.',
        helpMyScheduleNote: 'Feed obsahuje stejné akce jako filtr „Můj rozvrh" v aplikaci — akce, kde máte aktivní přihlášku nebo jste koordinátorem.',
        helpGoogle: 'Google Calendar: Jiné kalendáře → Přidat URL',
        helpApple: 'Apple Calendar: Soubor → Nový odběr kalendáře',
        helpOutlook: 'Outlook: Přidat kalendář → Odebírat z webu',
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

const templateFieldPrompts: Record<string, Record<string, string>> = {
    suspendMember: {reason: 'Důvod', note: 'Poznámka'},
};

export function withTemplateFieldPrompts<T extends {properties: Array<{name: string; prompt?: string}>}>(
    template: T,
    templateName: string,
): T {
    const prompts = templateFieldPrompts[templateName];
    if (!prompts) return template;
    return {
        ...template,
        properties: template.properties.map(p => (!p.prompt && prompts[p.name] ? {...p, prompt: prompts[p.name]} : p)),
    };
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

export function getPermissionInfo(authority: string): { label: string; description: string } | undefined {
    return (labels.permissions as Record<string, { label: string; description: string } | undefined>)[authority];
}

export function getEnumLabel(enumType: string, value: string): string {
    const enumGroups = labels.enums as Record<string, Record<string, string>>;
    return enumGroups[enumType]?.[value] ?? value;
}
