import {expect, type Page} from '@playwright/test';
import type {FeeScenarioState} from './fixtures';
import type {Contribution} from './parameters';
import {heading, menuEntry, navigateInApp, openFromMenu, section} from './ui';

export const CATALOG = 'Katalog tierů';
export const ACTIVE_CAMPAIGN = 'Aktivní kampaň';
export const PAST_CAMPAIGNS = 'Minulé kampaně';
export const LEVELS = 'Skupiny';
const MEMBERS = 'ČLENOVÉ';
const LEVEL_DETAIL = 'Základní informace';

export const catalogTable = (page: Page) => page.getByRole('table', {name: 'Tabulka dat'});

// Members have no menu entry for the page, so they reach it through the SPA router.
export const openFeesPage = async (page: Page) => {
    if (await menuEntry(page, 'Členské příspěvky').count()) {
        await openFromMenu(page, 'Členské příspěvky');
    } else {
        await navigateInApp(page, '/membership-fee-tiers');
    }
    await expect(heading(page, CATALOG)).toBeVisible();
};

export const openCatalogLevel = async (page: Page, name: string) => {
    await openFeesPage(page);
    await catalogTable(page).getByRole('cell', {name, exact: true}).click();
    await expect(heading(page, LEVEL_DETAIL)).toBeVisible();
};

export const openListedClosedCampaign = async (page: Page, feeState: FeeScenarioState) => {
    await section(page, PAST_CAMPAIGNS).getByRole('link', {name: String(feeState.closedYear), exact: true}).first().click();
    await expect(heading(page, LEVELS)).toBeVisible();
};

export const openClosedCampaign = async (page: Page, feeState: FeeScenarioState) => {
    await openFeesPage(page);
    await openListedClosedCampaign(page, feeState);
};

export const openListedLevel = async (page: Page, name: string) => {
    await section(page, LEVELS).getByRole('link', {name, exact: true}).click();
    await expect(heading(page, LEVEL_DETAIL)).toBeVisible();
};

// The scenario's campaign is shown inline while active and in the past campaigns once closed.
export const openPublishedLevel = async (page: Page, feeState: FeeScenarioState, name: string) => {
    if (feeState.closedYear === feeState.year) {
        await openClosedCampaign(page, feeState);
    } else {
        await openFeesPage(page);
    }
    await openListedLevel(page, name);
};

export const levelDetail = (page: Page) => heading(page, LEVEL_DETAIL).locator('xpath=../..');

export const ruleRow = (page: Page, contribution: Contribution, ranking: string) =>
    page.getByRole('row')
        .filter({has: page.getByRole('cell', {name: ranking, exact: true})})
        .filter({has: page.getByRole('cell', {name: contribution.display, exact: true})});

const membersSection = (page: Page) => heading(page, MEMBERS).locator('xpath=../..');

export const memberRow = (page: Page, registrationNumber: string) =>
    membersSection(page).getByRole('row').filter({hasText: registrationNumber});

export const expectMembersLoaded = (page: Page) =>
    expect(page.getByText('Žádní členové ve skupině.').or(membersSection(page).locator('tbody tr').first())).toBeVisible();
