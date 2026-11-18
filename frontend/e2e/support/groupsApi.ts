import type {ApiClient} from './fixtures';
import {embeddedList} from './halApi';

export const createFreeGroup = async (owner: ApiClient, name: string) => {
    const groups = await owner.get('/api/groups');
    return owner.create(owner.templateTarget(groups, 'createGroup'), {name});
};

export const addGroupMember = async (owner: ApiClient, groupPath: string, member: ApiClient, memberId: string) => {
    await owner.post(`${groupPath}/invitations`, {memberId});
    const groupId = groupPath.split('/').pop();
    const invitation = embeddedList(await member.get('/api/invitations/pending')).find((pending) => pending.groupId === groupId);
    if (!invitation) throw new Error(`groupsApi: no pending invitation to group ${groupId}`);
    await member.post(invitation._links.accept.href, {});
};

export const deleteGroup = async (admin: ApiClient, groupPath: string) => {
    await admin.delete(groupPath);
};
