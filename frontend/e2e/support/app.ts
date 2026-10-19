import type {Page} from '@playwright/test';

export const waitForApp = (page: Page) =>
    page.getByRole('navigation').getByRole('link').first().waitFor();
