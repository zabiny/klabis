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

export const authFile = (role: Role) => `e2e/.auth/${role}.json`;
export const oidcSessionFile = (role: Role) => `e2e/.auth/${role}.oidc.json`;
