import {createBdd} from 'playwright-bdd';
import {test} from '../support/fixtures';
import type {Role} from '../support/roles';

const {Given} = createBdd(test);

Given('I am logged in as {string}', async ({loginAs}, role: string) => {
    await loginAs(role as Role);
});
