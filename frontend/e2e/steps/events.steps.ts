import {createBdd} from 'playwright-bdd';
import {test} from '../support/fixtures';

const {Given} = createBdd(test);

Given('a draft event {string} exists', async ({api, resolve}, name: string) => {
    const root = await api.get('/api');
    const events = await api.follow(root, 'events');
    await api.post(api.templateTarget(events, 'createEvent'), {
        name: resolve(name),
        eventDate: resolve('{date+60}'),
        organizer: 'ZBM',
        siUsage: 'NOT_USED',
    });
});
