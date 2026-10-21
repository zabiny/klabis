import type {ApiClient} from './fixtures';
import {minorDateOfBirth} from './dates';
import {embeddedList, type Linked} from './halApi';

export const REGISTERED_FIRST_NAME = 'Karel';

let sequence = 0;

// A Czech birth number is valid when the ten digits are divisible by 11; the digits after the date are unique per call.
const birthNumber = (dateOfBirth: string) => {
    const [year, month, day] = dateOfBirth.split('-');
    const datePart = `${year.slice(2)}${month}${day}`;
    const serial = String((Math.floor(Math.random() * 1000) + sequence++) % 1000).padStart(3, '0');
    return `${datePart}/${serial}${Number(`${datePart}${serial}`) % 11 % 10}`;
};

// Registration numbers allow 100 members per birth year and nothing is cleaned up, so adults are spread over
// birth years 1954-2005 (ten-digit birth numbers, always adult).
export const adultDateOfBirth = () => `${1954 + Math.floor(Math.random() * 52)}-05-17`;

export const registrationBody = (lastName: string, dateOfBirth = adultDateOfBirth()) => ({
    firstName: REGISTERED_FIRST_NAME,
    lastName,
    dateOfBirth,
    gender: 'MALE',
    nationality: 'CZ',
    birthNumber: birthNumber(dateOfBirth),
    email: `${lastName.toLowerCase()}@example.com`,
    phone: '+420 777 111 222',
    address: {street: 'Hlavní 1', city: 'Brno', postalCode: '60200', country: 'CZ'},
});

export const registerMember = async (admin: ApiClient, lastName: string) => {
    const members = await admin.follow(await admin.root(), 'members');
    return admin.create(admin.templateTarget(members, 'registerMember'), registrationBody(lastName));
};

export const guardianEmail = (guardianLastName: string) => `${guardianLastName.toLowerCase()}@example.com`;

export const registerMinorWithNewGuardian = async (admin: ApiClient, lastName: string, guardianLastName: string) => {
    const members = await admin.follow(await admin.root(), 'members');
    return admin.create(admin.templateTarget(members, 'registerMember'), {
        ...registrationBody(lastName, minorDateOfBirth()),
        email: undefined,
        phone: undefined,
        legalGuardians: [{firstName: REGISTERED_FIRST_NAME, lastName: guardianLastName, email: guardianEmail(guardianLastName), phone: '+420 777 333 444'}],
    });
};

export const memberAccount = async (admin: ApiClient, memberPath: string) =>
    admin.follow(await admin.get(memberPath), 'account') as Promise<Linked>;

export const depositTo = async (admin: ApiClient, memberPath: string, amount: number) => {
    const account = await memberAccount(admin, memberPath);
    await admin.post(admin.templateTarget(account, 'deposit'), {amount});
};

export const chargeFrom = async (admin: ApiClient, memberPath: string, amount: number) => {
    const account = await memberAccount(admin, memberPath);
    await admin.post(admin.templateTarget(account, 'charge'), {amount});
};

// Age ranges of training groups must not overlap and nothing is cleaned up, so every group gets a one-year range
// no real age reaches that is unique per call.
let ageSequence = 0;

export const createTrainingGroup = async (admin: ApiClient, name: string, trainerPath: string) => {
    const age = 1000 + ((Date.now() + ageSequence++) % 1_000_000_000);
    const groups = await admin.follow(await admin.root(), 'training-groups');
    await admin.create(admin.templateTarget(groups, 'createTrainingGroup'), {
        name,
        trainerId: trainerPath.split('/').pop(),
        minAge: age,
        maxAge: age,
    });
};

export const memberBalance = async (admin: ApiClient, memberPath: string) =>
    (await memberAccount(admin, memberPath)).balance as number;

export const findMemberPath = async (admin: ApiClient, lastName: string) => {
    const members = await admin.follow(await admin.root(), 'members');
    const found = embeddedList(await admin.get(`${members._links!.self.href.replace(/\{[^}]*\}/g, "")}?q=${encodeURIComponent(lastName)}`))
        .find((member) => member.lastName === lastName);
    if (!found) throw new Error(`membersApi: member "${lastName}" not found`);
    return `/api/members/${found.id}`;
};
