import {test as setup} from '@playwright/test';
import {type Role, roles} from './support/roles';
import {loginAndStoreSession} from './support/session';

for (const role of Object.keys(roles) as Role[]) {
    setup(`authenticate as ${role}`, async ({page}) => {
        await loginAndStoreSession(page, role);
    });
}
