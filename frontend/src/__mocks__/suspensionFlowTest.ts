import {vi} from 'vitest';
import {useAuthorizedMutation} from '../hooks/useAuthorizedFetch';
import {ApiError} from '../api/authorizedFetch';
import {mockHalFormsTemplate} from './halData';
import type {HalResponse} from '../api';

export const SUSPENSION_GROUP_URL = 'http://localhost/api/training-groups/g-1';

export const blockingTrainingGroup = {
    groupId: 'g-1',
    groupName: 'U10 Závoďáci',
    groupType: 'TRAINING',
    groupLink: SUSPENSION_GROUP_URL,
};

export const blocked409 = (groups: unknown, debt: unknown = null) => new ApiError(409, {params: {groups, debt}});

export const trainingGroupResource = (): HalResponse => ({
    _links: {self: {href: SUSPENSION_GROUP_URL}},
    _templates: {
        addTrainer: mockHalFormsTemplate({method: 'POST', target: '/api/training-groups/g-1/trainers', title: 'Přidat trenéra', properties: []}),
    },
});

export type SuspendOutcome = {error?: unknown};

export const installSuspendMutation = (suspendUrl: string, suspendResponses: SuspendOutcome[]) => {
    const calls: Array<{url: string; data: unknown}> = [];
    let suspendCall = 0;
    const mutate = vi.fn((
        vars: {url: string; data: unknown},
        opts: {onError?: (e: unknown) => void; onSuccess?: (r: unknown) => void},
    ) => {
        calls.push(vars);
        const outcome = vars.url === suspendUrl ? suspendResponses[suspendCall++] ?? {} : {};
        if (outcome.error) opts.onError?.(outcome.error);
        else opts.onSuccess?.({data: null, location: null});
    });
    vi.mocked(useAuthorizedMutation).mockReturnValue(
        {mutate, isPending: false, error: null} as unknown as ReturnType<typeof useAuthorizedMutation>,
    );
    return {calls};
};
