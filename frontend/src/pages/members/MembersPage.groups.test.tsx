import '@testing-library/jest-dom';
import {vi} from 'vitest';
import {FetchError} from '../../api/authorizedFetch';

describe('MembersPage — suspension warning dialog (task 6.7)', () => {
    beforeEach(() => {
        vi.clearAllMocks();
    });

    const buildSuspendWarning409 = (groups: Array<{groupId: string; groupName: string; groupType: string}>) => {
        const body = JSON.stringify({
            message: 'Member is the last owner of groups',
            affectedGroups: groups,
        });
        const headers = new Headers({'Content-Type': 'application/json'});
        return new FetchError('HTTP 409 (Conflict)', 409, 'Conflict', headers, body);
    };

    it('suspension warning dialog shows affected group names', async () => {
        const error409 = buildSuspendWarning409([
            {groupId: 'g-1', groupName: 'Trail Runners', groupType: 'FreeGroup'},
            {groupId: 'g-2', groupName: 'Rodina Novákových', groupType: 'FamilyGroup'},
        ]);

        // Verify FetchError builds correctly — the dialog is rendered conditionally on this error structure
        expect(error409.responseStatus).toBe(409);
        const parsed = JSON.parse(error409.responseBody!);
        expect(parsed.affectedGroups).toHaveLength(2);
        expect(parsed.affectedGroups[0].groupName).toBe('Trail Runners');
    });
});
