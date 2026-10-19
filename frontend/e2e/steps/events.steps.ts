import {createBdd} from 'playwright-bdd';
import {test} from '../support/fixtures';

const {Given} = createBdd(test);

Given('a draft event {text} exists', async ({api}, name: string) => {
    const root = await api.get('/api');
    const events = await api.follow(root, 'events');
    await api.post(api.templateTarget(events, 'createEvent'), {
        name,
        eventDate: new Date(Date.now() + 60 * 86_400_000).toISOString().slice(0, 10),
        organizer: 'ZBM',
        siUsage: 'NOT_USED',
    });
});
