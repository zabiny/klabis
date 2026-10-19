import {readFileSync} from 'node:fs';
import {test as base} from 'playwright-bdd';
import {authFile, oidcSessionFile, type Role} from './roles';

// The authorization server rotates JSESSIONID on concurrent bearer-authenticated API calls,
// so a stored cookie alone is not a reliable way back in. The OIDC user is restored into
// sessionStorage before the app boots.
export type HalResource = {
    _links?: Record<string, {href: string}>;
    _templates?: Record<string, {target?: string}>;
    _location?: string;
};

export type ApiClient = {
    get: (path: string) => Promise<HalResource>;
    post: (path: string, body: unknown) => Promise<HalResource>;
    follow: (resource: HalResource, rel: string) => Promise<HalResource>;
    templateTarget: (resource: HalResource, template: string) => string;
};

type Fixtures = {
    api: ApiClient;
    loginAs: (role: Role) => Promise<void>;
    resolve: (text: string) => string;
};

export const test = base.extend<Fixtures>({
    // eslint-disable-next-line no-empty-pattern
    resolve: async ({}, provide, testInfo) => {
        const prefix = `E2E-${process.env.E2E_RUN_ID}-${testInfo.workerIndex}-`;
        await provide((text) => text
            .replaceAll('{prefix}', prefix)
            .replace(/\{date\+(\d+)\}/g, (_, days) => {
                const date = new Date();
                date.setDate(date.getDate() + Number(days));
                return date.toISOString().slice(0, 10);
            }));
    },
    api: async ({page, request}, provide) => {
        const accessToken = async () => {
            const raw = await page.evaluate(() => {
                const key = Object.keys(sessionStorage).find((k) => k.startsWith('oidc.user:'));
                return key ? sessionStorage.getItem(key) : null;
            });
            if (!raw) throw new Error('api fixture: no oidc.user:* entry in sessionStorage; log in first');
            return JSON.parse(raw).access_token as string;
        };

        // HAL hrefs are absolute and carry the backend origin; only path and query are kept so the call goes through the E2E_BASE_URL target.
        const toPath = (href: string) => {
            const url = new URL(href, 'http://placeholder');
            return url.pathname + url.search;
        };

        const call = async (method: 'GET' | 'POST', path: string, body?: unknown) => {
            const response = await request.fetch(toPath(path), {
                method,
                headers: {
                    Authorization: `Bearer ${await accessToken()}`,
                    Accept: 'application/prs.hal-forms+json',
                    ...(body === undefined ? {} : {'Content-Type': 'application/json'}),
                },
                data: body,
            });
            const text = await response.text();
            if (!response.ok()) {
                throw new Error(`API ${method} ${path} failed with ${response.status()}: ${text}`);
            }
            return text ? JSON.parse(text) : {_location: response.headers()['location']};
        };

        const client: ApiClient = {
            get: (path) => call('GET', path),
            post: (path, body) => call('POST', path, body),
            follow: (resource, rel) => {
                const href = resource?._links?.[rel]?.href;
                if (!href) throw new Error(`api fixture: link "${rel}" missing, available: ${Object.keys(resource?._links ?? {})}`);
                return call('GET', href.replace(/\{[^}]*\}/g, ''));
            },
            templateTarget: (resource, template) => {
                const target = resource?._templates?.[template]?.target;
                if (!target) throw new Error(`api fixture: template "${template}" missing, available: ${Object.keys(resource?._templates ?? {})}`);
                return target;
            },
        };
        await provide(client);
    },
    loginAs: async ({page}, provide) => {
        await provide(async (role) => {
            const {cookies} = JSON.parse(readFileSync(authFile(role), 'utf-8'));
            const oidcEntries: [string, string][] = JSON.parse(readFileSync(oidcSessionFile(role), 'utf-8'));
            await page.context().addCookies(cookies);
            await page.addInitScript((entries) => {
                if (sessionStorage.getItem('e2e.oidcRestored')) return;
                entries.forEach(([key, value]) => sessionStorage.setItem(key, value));
                sessionStorage.setItem('e2e.oidcRestored', '1');
            }, oidcEntries);
            await page.goto('/');
            await page.locator('nav:visible').first().waitFor();
        });
    },
});
