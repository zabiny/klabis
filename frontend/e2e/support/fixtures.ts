import type {APIRequestContext} from '@playwright/test';
import {test as base} from 'playwright-bdd';
import {readSession, type Role} from './roles';
import {renewSession} from './session';

export type HalResource = {
    _links?: Record<string, {href: string}>;
    _templates?: Record<string, {target?: string}>;
};

export type ApiClient = {
    get: (path: string) => Promise<HalResource>;
    post: (path: string, body: unknown) => Promise<HalResource>;
    delete: (path: string) => Promise<HalResource>;
    follow: (resource: HalResource, rel: string) => Promise<HalResource>;
    templateTarget: (resource: HalResource, template: string) => string;
};

type FeeScenarioState = { year?: number; closedYear?: number };

type Fixtures = {
    api: ApiClient;
    apiAs: (role: Role) => ApiClient;
    feeState: FeeScenarioState;
    loginAs: (role: Role) => Promise<void>;
    currentRole: { value?: Role };
};

export const test = base.extend<Fixtures>({
    // eslint-disable-next-line no-empty-pattern
    currentRole: async ({}, provide) => {
        await provide({});
    },
    // eslint-disable-next-line no-empty-pattern
    feeState: async ({}, provide) => {
        await provide({});
    },
    apiAs: async ({playwright, browser, baseURL}, provide) => {
        const contexts = new Map<Role, APIRequestContext>();
        const clients = new Map<Role, ApiClient>();

        // Mirrors parseHalHref/serializeHalHref in src/api/hateoas.ts (not importable: generated types / window).
        // HAL hrefs carry the backend origin; only path and query are kept so calls go through E2E_BASE_URL.
        const toPath = (href: string) => {
            const url = new URL(href, 'http://placeholder');
            return url.pathname + url.search;
        };

        const clientFor = (role: Role): ApiClient => {
            const existing = clients.get(role);
            if (existing) return existing;

            const getContext = async () => {
                const cached = contexts.get(role);
                if (cached) return cached;
                let session = readSession(role);
                const expiresAt = JSON.parse(session.oidc[0][1]).expires_at as number;
                if (expiresAt * 1000 - Date.now() < 60_000) {
                    session = await renewSession(browser, baseURL, role);
                }
                const [, raw] = session.oidc[0];
                const created = await playwright.request.newContext({
                    baseURL,
                    ignoreHTTPSErrors: true,
                    extraHTTPHeaders: {
                        Authorization: `Bearer ${JSON.parse(raw).access_token}`,
                        Accept: 'application/prs.hal-forms+json',
                    },
                });
                contexts.set(role, created);
                return created;
            };

            const call = async (method: 'GET' | 'POST' | 'DELETE', path: string, body?: unknown) => {
                const response = await (await getContext()).fetch(toPath(path), {method, data: body});
                const text = await response.text();
                if (!response.ok()) {
                    throw new Error(`API ${method} ${path} failed with ${response.status()}: ${text}`);
                }
                return text ? JSON.parse(text) : {};
            };

            const client: ApiClient = {
                get: (path) => call('GET', path),
                post: (path, body) => call('POST', path, body),
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
        await Promise.all([...contexts.values()].map((context) => context.dispose()));
    },
    api: async ({apiAs, currentRole}, provide) => {
        const client = () => {
            if (!currentRole.value) throw new Error('api fixture: no role logged in; use "Given I am logged in as" first');
            return apiAs(currentRole.value);
        };
        await provide({
            get: (path) => client().get(path),
            post: (path, body) => client().post(path, body),
            delete: (path) => client().delete(path),
            follow: (resource, rel) => client().follow(resource, rel),
            templateTarget: (resource, template) => client().templateTarget(resource, template),
        });
    },
    loginAs: async ({page, currentRole}, provide) => {
        await provide(async (role) => {
            const {cookies} = readSession(role);
            currentRole.value = role;
            await page.context().addCookies(cookies);
        });
    },
});
