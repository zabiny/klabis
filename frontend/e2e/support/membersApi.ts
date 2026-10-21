import type {ApiClient} from './fixtures';
import {embeddedList, type Linked} from './halApi';

export const REGISTERED_FIRST_NAME = 'Karel';

let sequence = 0;

// A Czech birth number is valid when the ten digits are divisible by 11; the digits after the date are unique per call.
const birthNumber = () => {
    const serial = (Date.now() + sequence++) % 1000;
    const base = Number(`900517${String(serial).padStart(3, '0')}`);
    return `900517/${String(serial).padStart(3, '0')}${base % 11 % 10}`;
};

export const registrationBody = (lastName: string) => ({
    firstName: REGISTERED_FIRST_NAME,
    lastName,
    dateOfBirth: '1990-05-17',
    gender: 'MALE',
    nationality: 'CZ',
    birthNumber: birthNumber(),
    email: `${lastName.toLowerCase()}@example.com`,
    phone: '+420 777 111 222',
    address: {street: 'Hlavní 1', city: 'Brno', postalCode: '60200', country: 'CZ'},
});

export const registerMember = async (admin: ApiClient, lastName: string) => {
    const members = await admin.follow(await admin.root(), 'members');
    return admin.create(admin.templateTarget(members, 'registerMember'), registrationBody(lastName));
};

export const memberAccount = async (admin: ApiClient, memberPath: string) =>
    admin.follow(await admin.get(memberPath), 'account') as Promise<Linked>;

export const depositTo = async (admin: ApiClient, memberPath: string, amount: number) => {
    const account = await memberAccount(admin, memberPath);
    await admin.post(admin.templateTarget(account, 'deposit'), {amount});
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
