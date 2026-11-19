import {expect} from '@playwright/test';
import {createBdd} from 'playwright-bdd';
import {test} from '../support/fixtures';
import {addCatalogRule, changeCatalogYearlyFee, ensureCatalogLevel} from '../support/membershipFeesApi';
import {catalogTable, openCatalogLevel, openFeesPage, ruleRow} from '../support/membershipFeesPages';
import type {Contribution} from '../support/parameters';
import {expectActing, type Persona} from '../support/personas';
import {action, click, fill, select, submit} from '../support/ui';

const {Given, When, Then} = createBdd(test);

Given('a catalog fee type {name} exists', async ({apiAs}, name: string) => {
    await ensureCatalogLevel(apiAs('admin'), name);
});

Given('a catalog fee type {name} with yearly fee {int} CZK exists', async ({apiAs}, name: string, yearlyFee: number) => {
    await ensureCatalogLevel(apiAs('admin'), name, yearlyFee);
});

Given('catalog type {name} includes a {contribution} rule for races of ranking {name}',
    async ({apiAs}, name: string, contribution: Contribution, ranking: string) => {
        await addCatalogRule(apiAs('admin'), name, contribution, ranking);
    });

Given('the catalog price of type {name} is changed to {int} CZK', async ({apiAs}, name: string, yearlyFee: number) => {
    await changeCatalogYearlyFee(apiAs('admin'), name, yearlyFee);
});

When('{persona} creates fee type {name} with yearly fee {int} CZK', async ({page, actAs}, persona: Persona, name: string, yearlyFee: number) => {
    await actAs(persona);
    await openFeesPage(page);
    await click(page, 'Přidat typ členského příspěvku');
    await fill(page, 'Název', name);
    await fill(page, 'Roční poplatek', String(yearlyFee));
    await submit(page);
});

When('{persona} changes the yearly fee of catalog type {name} to {int} CZK', async ({page, actAs}, persona: Persona, name: string, yearlyFee: number) => {
    await actAs(persona);
    await openCatalogLevel(page, name);
    await click(page, 'Upravit');
    await fill(page, 'Roční poplatek', String(yearlyFee));
    await submit(page, 'Uložit změny');
});

When('{persona} adds a {contribution} rule for races of ranking {name} to catalog type {name}',
    async ({page, actAs}, persona: Persona, contribution: Contribution, ranking: string, name: string) => {
        await actAs(persona);
        await openCatalogLevel(page, name);
        await click(page, 'Přidat pravidlo…');
        await select(page, 'Typ závodu', 'Závod');
        await fill(page, 'Žebříček', ranking);
        if (contribution.ruleType === 'PERCENTAGE') {
            await select(page, 'Typ pravidla', 'Procento');
            await fill(page, 'Procento spoluúčasti (%)', String(contribution.value));
        } else {
            await select(page, 'Typ pravidla', 'Fixní částka');
            await fill(page, 'Fixní částka', String(contribution.value));
            await fill(page, 'Měna fixní částky', 'CZK');
        }
        await submit(page);
    });

Then('the catalog lists type {name} with yearly fee {int} CZK', async ({page}, name: string, yearlyFee: number) => {
    await openFeesPage(page);
    const yearlyFeeCell = catalogTable(page).getByRole('cell', {name, exact: true}).locator('xpath=following-sibling::*[1]');
    await expect(yearlyFeeCell).toHaveText(String(yearlyFee));
});

Then('the membership fees page lists catalog type {name}', async ({page}, name: string) => {
    await openFeesPage(page);
    await expect(catalogTable(page).getByRole('cell', {name, exact: true})).toBeVisible();
});

Then('catalog type {name} has no payment rules', async ({page}, name: string) => {
    await openCatalogLevel(page, name);
    await expect(page.getByText('Žádná pravidla spoluúčasti')).toBeVisible();
});

Then('catalog type {name} has a {contribution} rule for races of ranking {name}',
    async ({page}, name: string, contribution: Contribution, ranking: string) => {
        await openCatalogLevel(page, name);
        await expect(ruleRow(page, contribution, ranking)).toBeVisible();
    });

Then('catalog type {name} has no {contribution} rule for races of ranking {name}',
    async ({page}, name: string, contribution: Contribution, ranking: string) => {
        await openCatalogLevel(page, name);
        await expect(page.getByRole('cell', {name: ranking, exact: true}).first()).toBeVisible();
        await expect(ruleRow(page, contribution, ranking)).toHaveCount(0);
    });

Then('the rule for races of ranking {name} is refused as a duplicate of an existing one', async ({page}, ranking: string) => {
    const message = `Pravidlo pro typ akce „Závod“ a žebříček „${ranking}“ již v tomto typu členského příspěvku existuje.`;
    await expect(page.getByRole('alert').filter({hasText: message}).first()).toBeVisible();
});

Then('{persona} is not offered to add a fee type', async ({page, currentRole}, persona: Persona) => {
    expectActing(currentRole, persona);
    await openFeesPage(page);
    await expect(action(page, 'Přidat typ členského příspěvku')).toHaveCount(0);
});

Then('{persona} is not offered to manage catalog type {name}', async ({page, currentRole}, persona: Persona, name: string) => {
    expectActing(currentRole, persona);
    await openCatalogLevel(page, name);
    for (const label of ['Upravit', 'Smazat typ členského příspěvku', 'Přidat pravidlo…']) {
        await expect(action(page, label)).toHaveCount(0);
    }
});
