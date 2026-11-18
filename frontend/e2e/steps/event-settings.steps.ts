import {expect, type Page} from '@playwright/test';
import {createBdd, type DataTable} from 'playwright-bdd';
import {test} from '../support/fixtures';
import type {Persona} from '../support/personas';
import {scenarioPrefix} from '../support/parameters';
import {action, fill, heading, openAddress, openFromMenu, submit} from '../support/ui';

const {Given, When, Then} = createBdd(test);

const TAB_ADDRESSES: Record<string, string> = {
    'Disciplíny': 'disciplines',
    'Typy akcí': 'event-types',
    'Šablony kategorií': 'category-presets',
};

const tab = (page: Page, name: string) => page.getByRole('tab', {name, exact: true});

const openTab = async (page: Page, name: string) => {
    await openAddress(page, `/event-settings/${TAB_ADDRESSES[name]}`);
    await expect(tab(page, name)).toHaveAttribute('aria-selected', 'true');
};

Given('{persona} has opened event settings', async ({actAs, page}, persona: Persona) => {
    await actAs(persona);
    await openFromMenu(page, 'Nastavení akcí');
    await expect(tab(page, 'Disciplíny')).toBeVisible();
});

Given('{persona} has opened the tab {string} of event settings', async ({actAs, page}, persona: Persona, name: string) => {
    await actAs(persona);
    await openTab(page, name);
});

When('{persona} opens {string} from the main menu', async ({actAs, page}, persona: Persona, item: string) => {
    await actAs(persona);
    await openFromMenu(page, item);
});

When('{persona} selects the tab {string}', async ({page}, _persona: Persona, name: string) => {
    await tab(page, name).click();
});

When('{persona} reloads the page', async ({actAs, page}, persona: Persona) => {
    await actAs(persona);
    await page.reload();
});

When('{persona} creates event type {string} on the tab', async ({page}, _persona: Persona, name: string) => {
    await action(page, 'Přidat typ akce').first().click();
    await fill(page, 'Název', `${scenarioPrefix()}${name}`);
    await submit(page);
});

When('{persona} opens the saved link to the former {string} page', async ({actAs, page}, persona: Persona, name: string) => {
    await actAs(persona);
    await openAddress(page, `/${TAB_ADDRESSES[name]}`);
});

When('{persona} opens the event settings address directly', async ({actAs, page}, persona: Persona) => {
    await actAs(persona);
    await openAddress(page, '/event-settings/event-types');
});

Then('the event settings page shows the tabs:', async ({page}, tabs: DataTable) => {
    for (const [name] of tabs.raw()) {
        await expect(tab(page, name)).toBeVisible();
    }
});

Then('the event settings page is shown', async ({page}) => {
    await expect(heading(page, 'Nastavení akcí')).toBeVisible();
    await expect(page.getByRole('tab').first()).toBeVisible();
});

Then('the tab {string} is selected', async ({page}, name: string) => {
    await expect(tab(page, name)).toHaveAttribute('aria-selected', 'true');
});

Then('the tab {string} is open at its own address', async ({page}, name: string) => {
    await expect(page).toHaveURL(new RegExp(`/event-settings/${TAB_ADDRESSES[name]}$`));
});

Then('the tab {string} lists event type {string}', async ({page}, name: string, eventType: string) => {
    await expect(tab(page, name)).toHaveAttribute('aria-selected', 'true');
    await expect(page.getByRole('cell', {name: `${scenarioPrefix()}${eventType}`})).toBeVisible();
});

Then('access is denied', async ({page}) => {
    await expect(heading(page, 'Přístup odepřen')).toBeVisible();
    await expect(page.getByRole('tab')).toHaveCount(0);
});
