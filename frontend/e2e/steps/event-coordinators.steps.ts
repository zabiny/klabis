import {expect, type Page} from '@playwright/test';
import {createBdd} from 'playwright-bdd';
import {createCategoryPreset, createDraftEvent, createOpenEvent} from '../support/eventsApi';
import {openEvent, openEventList} from '../support/eventsPages';
import {test} from '../support/fixtures';
import {memberIdentity} from '../support/halApi';
import {scenarioPrefix} from '../support/parameters';
import {expectActing, personas, type Persona} from '../support/personas';
import type {Role} from '../support/roles';
import {action, click, heading, select, submit} from '../support/ui';

const forbiddenResponses = new WeakMap<Page, string[]>();

const {Given, When, Then} = createBdd(test);

const memberId = async (apiAs: (role: Role) => Parameters<typeof memberIdentity>[0], persona: Persona) =>
    (await memberIdentity(apiAs(personas[persona]))).id;

Given('{persona} coordinates the draft event {name}', async ({apiAs}, persona: Persona, name: string) => {
    await createDraftEvent(apiAs('admin'), name, [await memberId(apiAs, persona)]);
});

Given('{persona} coordinates the open event {name}', async ({apiAs}, persona: Persona, name: string) => {
    await createOpenEvent(apiAs('admin'), name, [await memberId(apiAs, persona)]);
});

Given('category preset {name} offers categories {string}', async ({apiAs}, name: string, categories: string) => {
    await createCategoryPreset(apiAs('admin'), name, categories.split(', '));
});

const openCoordinatedEventList = async (page: Parameters<typeof openEventList>[0]) => {
    await openEventList(page, scenarioPrefix());
};

const displayedName = async (apiAs: Parameters<typeof memberId>[0], persona: Persona) => {
    const {firstName, lastName} = await memberIdentity(apiAs(personas[persona]));
    return new RegExp(`${firstName} ${lastName}|${lastName} ${firstName}`);
};

const filterByCoordinator = async ({page, actAs, apiAs}: any, persona: Persona, coordinator: Persona) => { // eslint-disable-line @typescript-eslint/no-explicit-any
    await actAs(persona);
    await openCoordinatedEventList(page);
    const label = await displayedName(apiAs, coordinator);
    const option = page.getByLabel('Vedoucí').locator('option').filter({hasText: label});
    await page.getByLabel('Vedoucí').selectOption({label: (await option.first().textContent())!});
};

When('{persona} opens the event list', async ({page, actAs}, persona: Persona) => {
    await actAs(persona);
    await openCoordinatedEventList(page);
});

When('{persona} opens the detail of event {name}', async ({page, actAs}, persona: Persona, name: string) => {
    await actAs(persona);
    await openEvent(page, name);
});

When('{persona} applies category preset {name} to event {name}', async ({page, actAs}, persona: Persona, preset: string, name: string) => {
    await actAs(persona);
    await openEvent(page, name);
    await click(page, 'Upravit');
    await click(page, 'Vybrat ze šablon');
    await page.getByRole('dialog').getByRole('button', {name: preset, exact: true}).click();
    await submit(page, 'Uložit změny');
});

When('{persona} filters the event list by coordinator {persona}', filterByCoordinator);

Given('{persona} has filtered the event list by coordinator {persona}', filterByCoordinator);

When('{persona} clears the coordinator filter', async ({page, currentRole}, persona: Persona) => {
    expectActing(currentRole, persona);
    await select(page, 'Vedoucí', '—');
});

Then('the event list does not contain {name}', async ({page}, name: string) => {
    await expect(page.getByRole('table').locator('tbody tr').first()).toBeVisible();
    await expect(page.getByRole('cell', {name})).toHaveCount(0);
});

Then('event {name} is active', async ({page}, name: string) => {
    await openEvent(page, name);
    await expect(page.getByText('Aktivní', {exact: true}).first()).toBeVisible();
});

Then('{persona} is not offered to publish or cancel event {name}', async ({page, currentRole}, persona: Persona, name: string) => {
    expectActing(currentRole, persona);
    await expect(heading(page, name)).toBeVisible();
    await expect(action(page, 'Publikovat')).toHaveCount(0);
    await expect(action(page, 'Zrušit akci')).toHaveCount(0);
});

Then('event {name} offers categories {string}', async ({page}, name: string, categories: string) => {
    await openEvent(page, name);
    for (const category of categories.split(', ')) {
        await expect(page.getByText(category, {exact: true}).first()).toBeVisible();
    }
});

When('{persona} opens the category presets of the edit form of event {name}', async ({page, actAs}, persona: Persona, name: string) => {
    await actAs(persona);
    const forbidden: string[] = [];
    forbiddenResponses.set(page, forbidden);
    page.on('response', (response) => {
        if (response.status() === 403) forbidden.push(response.url());
    });
    await openEvent(page, name);
    await click(page, 'Upravit');
    await click(page, 'Vybrat ze šablon');
    await expect(page.getByRole('dialog')).toBeVisible();
});

Then('the application made no forbidden requests', async ({page}) => {
    expect(forbiddenResponses.get(page)).toEqual([]);
});
