import {expect} from '@playwright/test';
import type {Role} from './roles';

// Feature files speak about named personas; each one acts through the seeded user of its role.
export const personas = {
    Jan: 'admin',
    Eva: 'member',
} as const satisfies Record<string, Role>;

export type Persona = keyof typeof personas;

// Steps that inspect the page the persona just opened must not silently look at another persona's session.
export const expectActing = (currentRole: {value?: Role}, persona: Persona) =>
    expect(currentRole.value, `${persona} is not the acting persona`).toBe(personas[persona]);
