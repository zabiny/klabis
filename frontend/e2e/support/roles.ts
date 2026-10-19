import {readFileSync} from 'node:fs';
import type {BrowserContext} from '@playwright/test';

export type Role = 'admin' | 'member';

export const roles: Record<Role, { user: string; password: string }> = {
    admin: {
        user: process.env.E2E_ADMIN_USER ?? 'ZBM9000',
        password: process.env.E2E_ADMIN_PASSWORD ?? 'password',
    },
    member: {
        user: process.env.E2E_MEMBER_USER ?? 'ZBM9500',
        password: process.env.E2E_MEMBER_PASSWORD ?? 'password',
    },
};

export type RoleSession = {
    cookies: Parameters<BrowserContext['addCookies']>[0];
    oidc: [string, string][];
};

export const sessionFile = (role: Role) => `e2e/.auth/${role}.json`;

export const readSession = (role: Role): RoleSession =>
    JSON.parse(readFileSync(sessionFile(role), 'utf-8'));
