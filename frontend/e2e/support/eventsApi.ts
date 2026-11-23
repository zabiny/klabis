import {localIsoDate} from './dates';
import type {ApiClient} from './fixtures';
import {embeddedList, type Linked, templateOptions} from './halApi';

const EVENT_DATE = localIsoDate(60);
const REGISTRATION_DEADLINE = localIsoDate(30);

const createEvent = async (admin: ApiClient, body: Record<string, unknown>) => {
    const events = await admin.follow(await admin.root(), 'events');
    await admin.post(admin.templateTarget(events, 'createEvent'), {eventDate: EVENT_DATE, organizer: 'ZBM', siUsage: 'NOT_USED', ...body});
};

const findEvent = async (client: ApiClient, name: string) => {
    const events = await client.follow(await client.root(), 'events');
    const found = embeddedList(await client.get(`${events._links!.self.href}?q=${encodeURIComponent(name)}`))
        .find((event) => event.name === name);
    if (!found) throw new Error(`eventsApi: event "${name}" not found`);
    return client.get(`/api/events/${found.id}`) as Promise<Linked>;
};

export const createDraftEvent = (admin: ApiClient, name: string, coordinators: string[] = []) =>
    createEvent(admin, {name, coordinators});

export const createOpenEvent = async (admin: ApiClient, name: string, {coordinators = [], categories = ['M21']}: {coordinators?: string[]; categories?: string[]} = {}) => {
    await createEvent(admin, {name, coordinators, deadlines: [REGISTRATION_DEADLINE], categories: categories.map((category) => ({name: category}))});
    const event = await findEvent(admin, name);
    await admin.post(admin.templateTarget(event, 'publishEvent'), {});
};

export const eventAppPath = async (client: ApiClient, name: string) =>
    new URL((await findEvent(client, name))._links!.self.href, 'http://placeholder').pathname.replace(/^\/api/, '');

export const registerForEvent = async (member: ApiClient, name: string) => {
    const event = await findEvent(member, name);
    const categoryId = templateOptions(event, 'registerForEvent', 'categoryId')[0].value;
    await member.post(member.templateTarget(event, 'registerForEvent'), {categoryId});
};

export const createCategoryPreset = async (admin: ApiClient, name: string, categories: string[]) => {
    const presets = await admin.follow(await admin.root(), 'category-presets');
    await admin.post(admin.templateTarget(presets, 'createCategoryPreset'), {name, categories});
};

export const registerMemberForEvent = async (admin: ApiClient, name: string, memberId: string, category: string) => {
    const event = await findEvent(admin, name);
    const categoryId = (event.categories as Array<{id: string; name: string}>).find((candidate) => candidate.name === category)?.id;
    if (!categoryId) throw new Error(`eventsApi: category "${category}" not found in event "${name}"`);
    const self = event._links?.self?.href;
    if (!self) throw new Error(`eventsApi: event "${name}" has no self link`);
    await admin.post(`${self}/registrations/${memberId}`, {categoryId});
};
