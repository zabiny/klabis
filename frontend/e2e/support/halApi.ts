import type {ApiClient, HalResource} from './fixtures';

export type Linked = HalResource & {
    _embedded?: Record<string, Array<Record<string, any>>>; // eslint-disable-line @typescript-eslint/no-explicit-any
    [key: string]: any; // eslint-disable-line @typescript-eslint/no-explicit-any
};

export const embeddedList = (resource: Linked) => Object.values(resource._embedded ?? {})[0] ?? [];

export const templateOptions = (resource: Linked, template: string, property: string): Array<{value: string; prompt: string}> =>
    (resource._templates![template] as any).properties // eslint-disable-line @typescript-eslint/no-explicit-any
        .find((candidate: {name: string}) => candidate.name === property).options.inline;

export type MemberIdentity = {id: string; firstName: string; lastName: string; registrationNumber: string};

// API clients live for the whole worker and a persona's identity never changes, so it is resolved once per client.
const identities = new WeakMap<ApiClient, MemberIdentity>();

export const memberIdentity = async (member: ApiClient): Promise<MemberIdentity> => {
    const cached = identities.get(member);
    if (cached) return cached;
    const profile = await member.get((await member.root())._links!.profile.href) as Linked;
    const identity = {
        id: profile._links!.self.href.split('/').pop()!,
        firstName: profile.firstName,
        lastName: profile.lastName,
        registrationNumber: profile.registrationNumber,
    };
    identities.set(member, identity);
    return identity;
};
