import {expect, type Page} from '@playwright/test';
import {fill, heading, openFromMenu, select} from './ui';

export const openEventList = async (page: Page, name: string) => {
    await openFromMenu(page, 'Akce');
    await select(page, 'Rok', '—');
    await fill(page, 'Hledat', name);
};

export const openEvent = async (page: Page, name: string) => {
    await openEventList(page, name);
    await page.getByRole('cell', {name}).first().click();
    await expect(heading(page, name)).toBeVisible();
};
