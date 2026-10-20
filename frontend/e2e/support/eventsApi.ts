import {localIsoDate} from './dates';
import type {ApiClient} from './fixtures';
import {embeddedList, type Linked, templateOptions} from './halApi';

const EVENT_DATE = localIsoDate(60);
const REGISTRATION_DEADLINE = localIsoDate(30);

const createEvent = async (admin: ApiClient, body: Record<string, unknown>) => {
    const events = await admin.follow(await admin.get('/api'), 'events');
    await admin.post(admin.templateTarget(events, 'createEvent'), {eventDate: EVENT_DATE, organizer: 'ZBM', siUsage: 'NOT_USED', ...body});
};

const findEvent = async (client: ApiClient, name: string) => {
    const events = await client.follow(await client.get('/api'), 'events');
    const found = embeddedList(await client.get(`${events._links!.self.href}?q=${encodeURIComponent(name)}`))
        .find((event) => event.name === name);
    if (!found) throw new Error(`eventsApi: event "${name}" not found`);
    return client.get(`/api/events/${found.id}`) as Promise<Linked>;
};

export const createDraftEvent = (admin: ApiClient, name: string) => createEvent(admin, {name});

export const createOpenEvent = async (admin: ApiClient, name: string) => {
    await createEvent(admin, {name, deadlines: [REGISTRATION_DEADLINE], categories: [{name: 'M21'}]});
    const event = await findEvent(admin, name);
    await admin.post(admin.templateTarget(event, 'publishEvent'), {});
};

export const registerForEvent = async (member: ApiClient, name: string) => {
    const event = await findEvent(member, name);
    const categoryId = templateOptions(event, 'registerForEvent', 'categoryId')[0].value;
    await member.post(member.templateTarget(event, 'registerForEvent'), {categoryId});
};
