import {useApiPutMutation, useApiQuery} from '../hooks/useApi';

// Types based on the API specification
export interface Member {
    id: number;
    firstName: string;
    lastName: string;
    registrationNumber: string;
    sex?: 'male' | 'female';
    dateOfBirth?: string;
    nationality?: string;
    address?: {
        streetAndNumber: string;
        city: string;
        postalCode: string;
        country: string;
    };
    contact?: {
        email?: string;
        phone?: string;
        note?: string;
    };
    legalGuardians?: Array<{
        firstName: string;
        lastName: string;
        contact: {
            email?: string;
            phone?: string;
            note?: string;
        };
        note?: string;
    }>;
    siCard?: number;
    licences?: {
        ob?: {
            licence: string;
        };
        referee?: {
            licence: string;
            expiryDate: string;
        };
        trainer?: {
            licence: string;
            expiryDate: string;
        };
    };
    bankAccount?: string;
    dietaryRestrictions?: string;
    drivingLicence?: string[];
    medicCourse?: boolean;
}

export interface MembersList {
    items: Member[];
}

export interface EditMyDetailsForm {
    nationality: string;
    address: {
        streetAndNumber: string;
        city: string;
        postalCode: string;
        country: string;
    };
    identityCard?: {
        number?: string;
        expiryDate?: string;
    };
    contact?: {
        email?: string;
        phone?: string;
        note?: string;
    };
    guardians?: Array<{
        firstName: string;
        lastName: string;
        contact: {
            email?: string;
            phone?: string;
            note?: string;
        };
        note?: string;
    }>;
    siCard?: number;
    bankAccount?: string;
    dietaryRestrictions?: string;
    drivingLicence?: string[];
    medicCourse?: boolean;
}

// Hooks for members API
export const useGetMembers = (view: 'full' | 'compact' = 'compact', suspended: boolean = false) => {
    return useApiQuery<MembersList>(['members', view, suspended.toString()], `/members?view=${view}&suspended=${suspended}`);
};

export const useGetMember = (memberId: number) => {
    return useApiQuery<Member>(['member', memberId.toString()], `/members/${memberId}`);
};

export const useGetEditMyDetailsForm = (memberId: number) => {
    return useApiQuery<EditMyDetailsForm>(
        ['memberEditForm', memberId.toString()],
        `/members/${memberId}/editOwnMemberInfoForm`
    );
};

export const useUpdateMyDetails = (memberId: number) => {
    return useApiPutMutation<EditMyDetailsForm, void>(
        `/members/${memberId}/editOwnMemberInfoForm`
    );
};

export interface MembershipSuspensionInfo {
    isSuspended: boolean;
    canSuspend: boolean;
    details: {
        finance: {
            status: boolean;
        };
    };
}

export const useGetSuspendMembershipForm = (memberId: number) => {
    return useApiQuery<MembershipSuspensionInfo>(
        ['suspendMembershipForm', memberId.toString()],
        `/members/${memberId}/suspendMembershipForm`
    );
};

export const useSuspendMembership = (memberId: number, force: boolean = false) => {
    return useApiPutMutation<void, void>(
        `/members/${memberId}/suspendMembershipForm?force=${force}`
    );
};