import {test as base} from 'playwright-bdd';
import {readSession, type Role} from './roles';

type HalResource = {
    _links?: Record<string, {href: string}>;
    _templates?: Record<string, {target?: string}>;
};

type ApiClient = {
    get: (path: string) => Promise<HalResource>;
    post: (path: string, body: unknown) => Promise<HalResource>;
    follow: (resource: HalResource, rel: string) => Promise<HalResource>;
    templateTarget: (resource: HalResource, template: string) => string;
};

type Fixtures = {
    api: ApiClient;
    loginAs: (role: Role) => Promise<void>;
    currentRole: { value?: Role };
};

export const test = base.extend<Fixtures>({
    // eslint-disable-next-line no-empty-pattern
    currentRole: async ({}, provide) => {
        await provide({});
    },
    api: async ({playwright, currentRole, baseURL}, provide) => {
        let context: Awaited<ReturnType<typeof playwright.request.newContext>> | undefined;
        const getContext = async () => {
            if (context) return context;
            if (!currentRole.value) throw new Error('api fixture: no role logged in; use "Given I am logged in as" first');
            const [, raw] = readSession(currentRole.value).oidc[0];
            context = await playwright.request.newContext({
                baseURL,
                ignoreHTTPSErrors: true,
                extraHTTPHeaders: {
                    Authorization: `Bearer ${JSON.parse(raw).access_token}`,
                    Accept: 'application/prs.hal-forms+json',
                },
            });
            return context;
        };

        // Mirrors parseHalHref/serializeHalHref in src/api/hateoas.ts (not importable: generated types / window).
        // HAL hrefs carry the backend origin; only path and query are kept so calls go through E2E_BASE_URL.
        const toPath = (href: string) => {
            const url = new URL(href, 'http://placeholder');
            return url.pathname + url.search;
        };

        const call = async (method: 'GET' | 'POST', path: string, body?: unknown) => {
            const response = await (await getContext()).fetch(toPath(path), {method, data: body});
            const text = await response.text();
            if (!response.ok()) {
                throw new Error(`API ${method} ${path} failed with ${response.status()}: ${text}`);
            }
            return text ? JSON.parse(text) : {};
        };

        await provide({
            get: (path) => call('GET', path),
            post: (path, body) => call('POST', path, body),
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
        });
        await context?.dispose();
    },
    loginAs: async ({page, currentRole}, provide) => {
        await provide(async (role) => {
            const {cookies} = readSession(role);
            currentRole.value = role;
            await page.context().addCookies(cookies);
        });
    },
});
