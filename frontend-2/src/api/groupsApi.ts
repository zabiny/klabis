import {useApiDeleteMutation, useApiPostMutation, useApiPutMutation, useApiQuery} from '../hooks/useApi';

// Types based on the API specification
export interface MembersGroupListItem {
    id: number;
    name: string;
    description?: string;
    address?: string;
}

export interface MembersGroupList {
    items: MembersGroupListItem[];
}

export interface MembersGroupPermission {
    permission: string;
    grantedToOwner?: boolean;
    grantedToMember?: boolean;
}

export interface MembersGroup {
    id: number;
    name: string;
    description?: string;
    address?: string;
    permissions?: MembersGroupPermission[];
}

// Hooks for groups API
export const useGetGroups = () => {
    return useApiQuery<MembersGroupList>(['groups'], '/memberGroups');
};

export const useGetGroup = (groupId: number) => {
    return useApiQuery<MembersGroup>(['group', groupId.toString()], `/memberGroups/${groupId}`);
};

export const useCreateGroup = () => {
    return useApiPostMutation<MembersGroup, MembersGroup>('/memberGroups');
};

export const useUpdateGroup = (groupId: number) => {
    return useApiPutMutation<MembersGroup, MembersGroup>(`/memberGroups/${groupId}`);
};

export const useDeleteGroup = (groupId: number) => {
    return useApiDeleteMutation<void>(`/memberGroups/${groupId}`);
};