import type {APIRequestContext, APIResponse} from '@playwright/test';
import {test as base} from 'playwright-bdd';
import {waitForApp} from './app';
import {personas, type Persona} from './personas';
import {readSession, type Role, type RoleSession} from './roles';
import {renewSession} from './session';

export type HalResource = {
    _links?: Record<string, {href: string}>;
    _templates?: Record<string, {target?: string}>;
};

export type ApiClient = {
    root: () => Promise<HalResource>;
    get: (path: string) => Promise<HalResource>;
    post: (path: string, body: unknown) => Promise<HalResource>;
    // Returns the path of the created resource from the Location header.
    create: (path: string, body: unknown) => Promise<string>;
    delete: (path: string) => Promise<HalResource>;
    follow: (resource: HalResource, rel: string) => Promise<HalResource>;
    templateTarget: (resource: HalResource, template: string) => string;
};

export type FeeScenarioState = { year?: number; closedYear?: number; campaignPath?: string };

type Fixtures = {
    feeState: FeeScenarioState;
    actAs: (persona: Persona) => Promise<void>;
    currentRole: { value?: Role };
};

type WorkerFixtures = {
    sessionOf: (role: Role) => Promise<RoleSession>;
    apiAs: (role: Role) => ApiClient;
};

const oidcUser = (session: RoleSession) => JSON.parse(session.oidc[0][1]) as {access_token: string; expires_at: number};

// Mirrors parseHalHref/serializeHalHref in src/api/hateoas.ts (not importable: generated types / window).
// HAL hrefs carry the backend origin; only path and query are kept so calls go through E2E_BASE_URL.
const toPath = (href: string) => {
    const url = new URL(href, 'http://placeholder');
    return url.pathname + url.search;
};

// The OIDC user lives in sessionStorage of the app origin, so it is written from a blank page served on that origin.
const SESSION_PAGE = '/e2e-session';

export const test = base.extend<Fixtures, WorkerFixtures>({
    // eslint-disable-next-line no-empty-pattern
    currentRole: async ({}, provide) => {
        await provide({});
    },
    // eslint-disable-next-line no-empty-pattern
    feeState: async ({}, provide) => {
        await provide({});
    },
    sessionOf: [async ({browser}, provide, workerInfo) => {
        // Calls are chained per role, so concurrent callers near expiry share one renewal.
        const sessions = new Map<Role, Promise<RoleSession>>();
        await provide((role) => {
            const next = (sessions.get(role) ?? Promise.resolve(readSession(role))).then((session) =>
                oidcUser(session).expires_at * 1000 - Date.now() < 60_000
                    ? renewSession(browser, workerInfo.project.use.baseURL, role)
                    : session);
            sessions.set(role, next);
            return next;
        });
    }, {scope: 'worker'}],
    // Worker-scoped so the API root and the request context are reused across scenarios; scenario data is never cached here.
    apiAs: [async ({playwright, sessionOf}, provide, workerInfo) => {
        const {baseURL} = workerInfo.project.use;
        const contexts = new Map<Role, {token: string; context: APIRequestContext}>();
        const clients = new Map<Role, ApiClient>();

        const clientFor = (role: Role): ApiClient => {
            const existing = clients.get(role);
            if (existing) return existing;

            const getContext = async () => {
                const token = oidcUser(await sessionOf(role)).access_token;
                const cached = contexts.get(role);
                if (cached?.token === token) return cached.context;
                await cached?.context.dispose();
                const context = await playwright.request.newContext({
                    baseURL,
                    ignoreHTTPSErrors: true,
                    extraHTTPHeaders: {
                        Authorization: `Bearer ${token}`,
                        Accept: 'application/prs.hal-forms+json',
                    },
                });
                contexts.set(role, {token, context});
                return context;
            };

            const send = async (method: 'GET' | 'POST' | 'DELETE', path: string, body?: unknown): Promise<APIResponse> => {
                const response = await (await getContext()).fetch(toPath(path), {method, data: body});
                if (!response.ok()) {
                    throw new Error(`API ${method} ${path} failed with ${response.status()}: ${await response.text()}`);
                }
                return response;
            };

            const call = async (method: 'GET' | 'POST' | 'DELETE', path: string, body?: unknown) => {
                const text = await (await send(method, path, body)).text();
                return text ? JSON.parse(text) : {};
            };

            let apiRoot: HalResource | undefined;

            const client: ApiClient = {
                root: async () => (apiRoot ??= await call('GET', '/api')),
                get: (path) => call('GET', path),
                post: (path, body) => call('POST', path, body),
                create: async (path, body) => {
                    const location = (await send('POST', path, body)).headers().location;
                    if (!location) throw new Error(`api fixture: POST ${path} returned no Location header`);
                    return toPath(location);
                },
                delete: (path) => call('DELETE', path),
                follow: (resource, rel) => {
                    const href = resource._links?.[rel]?.href;
                    if (!href) throw new Error(`api fixture: link "${rel}" missing, available: ${Object.keys(resource._links ?? {})}`);
                    return call('GET', href.replace(/\{[^}]*\}/g, ''));
                },
                templateTarget: (resource, template) => {
                    // Spring HATEOAS omits the target when it equals self.
                    const target = resource._templates?.[template]?.target ?? resource._links?.self?.href;
                    if (!target) throw new Error(`api fixture: template "${template}" missing, available: ${Object.keys(resource._templates ?? {})}`);
                    return target;
                },
            };
            clients.set(role, client);
            return client;
        };

        await provide(clientFor);
        await Promise.all([...contexts.values()].map(({context}) => context.dispose()));
    }, {scope: 'worker'}],
    // Switches the browser to the persona's session only when another role is signed in, so consecutive steps of
    // one persona continue on the current page. Restoring the OIDC user directly skips the sign-in redirect.
    actAs: async ({page, currentRole, sessionOf}, provide) => {
        await provide(async (persona) => {
            const role = personas[persona];
            if (currentRole.value === role) return;
            const session = await sessionOf(role);
            await page.context().clearCookies();
            await page.context().addCookies(session.cookies);
            await page.route(SESSION_PAGE, (route) => route.fulfill({contentType: 'text/html', body: '<!doctype html>'}), {times: 1});
            await page.goto(SESSION_PAGE);
            await page.evaluate((entries) => {
                sessionStorage.clear();
                for (const [key, value] of entries) sessionStorage.setItem(key, value);
            }, session.oidc);
            currentRole.value = role;
            await page.goto('/');
            await waitForApp(page);
        });
    },
});
