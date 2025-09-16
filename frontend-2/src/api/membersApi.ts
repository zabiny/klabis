import {useApiPostMutation, useApiPutMutation, useApiQuery} from '../hooks/useApi';
import {useQueryClient} from '@tanstack/react-query';
import type {QueryClient} from "@tanstack/query-core";
import type {KlabisHateoasObject} from '../hooks/klabisJsonUtils'

// Types based on the API specification
export interface Member extends KlabisHateoasObject {
    id: number;
    firstName: string;
    lastName: string;
    registrationNumber: string;
    birthCertificateNumber?: string;
    identityCard?: {
        number?: string;
        expiryDate?: string;
    };
    address: {
        streetAndNumber: string;
        city: string;
        postalCode: string;
        country: string;
    };
    dateOfBirth: string;
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
    nationality: string;
    sex: 'male' | 'female';
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

export interface MembersList extends KlabisHateoasObject {
    content: Member[];
    page: {
        size: number;
        totalElements: number;
        totalPages: number;
        number: number;
    };
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

export interface GetMembersParams {
    suspended?: boolean;
    view?: 'SUMMARY' | 'DETAILED';
    page?: number;
    size?: number;
    sort?: string[];
}

// Hooks for members API
export const useGetMembers = (params: GetMembersParams = {}) => {
    const {
        suspended = false,
        view = 'DETAILED',
        page = 0,
        size = 20,
        sort = []
    } = params;

    // Build query parameters
    const queryParams = new URLSearchParams();
    queryParams.append('suspended', suspended.toString());
    queryParams.append('view', view);
    queryParams.append('page', page.toString());
    queryParams.append('size', size.toString());

    // Add sort parameters
    sort.forEach(sortParam => {
        queryParams.append('sort', sortParam);
    });

    const queryString = queryParams.toString();
    const queryKey = ['members', suspended.toString(), view, page.toString(), size.toString(), ...sort];

    return useApiQuery<MembersList>(queryKey, `/members?${queryString}`);
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

const invalidateMemberData = (memberId: number, queryClient: QueryClient): void => {
    queryClient.invalidateQueries({queryKey: ['members']});
    queryClient.invalidateQueries({queryKey: ['member', memberId.toString()]});
    queryClient.invalidateQueries({queryKey: ['memberEditForm', memberId.toString()]});
}

export const useUpdateMyDetails = (memberId: number) => {
    const queryClient = useQueryClient();

    return useApiPutMutation<EditMyDetailsForm, void>(
        `/members/${memberId}/editOwnMemberInfoForm`,
        {
            onSuccess: () => {
                invalidateMemberData(memberId, queryClient)
            }
        }
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

export interface EditAnotherMemberDetailsForm {
    firstName: string;
    lastName: string;
    dateOfBirth: string;
    birthCertificateNumber?: string;
    nationality: string;
    sex: 'male' | 'female';
}

export interface MemberRegistrationForm {
    firstName: string;
    lastName: string;
    sex: 'male' | 'female';
    dateOfBirth: string;
    birthCertificateNumber?: string;
    nationality: string;
    address: {
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
    registrationNumber?: string;
    orisId?: number;
}

// --- REGISTRACE NOVÉHO ČLENA ---
// GET - načte šablonu formuláře pro registraci
export const useGetMemberRegistrationForm = () => {
    return useApiQuery<MemberRegistrationForm>(
        ['memberRegistrationForm'],
        `/memberRegistrations`
    );
};

// POST - odešle data nové registrace
export const useRegisterMember = () => {
    const queryClient = useQueryClient();
    // Případně můžeme invalidovat seznam členů
    return useApiPostMutation<MemberRegistrationForm, void>(
        `/memberRegistrations`,
        {
            onSuccess: () => {
                queryClient.invalidateQueries({queryKey: ['members']});
            }
        }
    );
};

export const useGetSuspendMembershipForm = (memberId: number) => {
    return useApiQuery<MembershipSuspensionInfo>(
        ['suspendMembershipForm', memberId.toString()],
        `/members/${memberId}/suspendMembershipForm`
    );
};

export const useSuspendMembership = (memberId: number, force: boolean = false) => {
    const queryClient = useQueryClient();
    return useApiPutMutation<void, void>(
        `/members/${memberId}/suspendMembershipForm?force=${force}`,
        {
            onSuccess: () => {
                invalidateMemberData(memberId, queryClient)
            }
        }
    );
};

// Types for permissions/grants
export type GrantType = 'members:register' | 'members:edit' | 'members:suspendMembership' | 'members:permissions';

export interface GlobalGrantDetail extends KlabisHateoasObject {
    grant: GrantType;
    description: string;
}

export interface GetAllGrants extends KlabisHateoasObject {
    grants: GlobalGrantDetail[];
}

export interface MemberGrantsForm extends KlabisHateoasObject {
    grants: GrantType[];
}

// Hooks for grants/permissions API
export const useGetAllGrants = () => {
    return useApiQuery<GetAllGrants>(['allGrants'], `/grants`);
};

export const useGetMemberGrants = (memberId: number) => {
    return useApiQuery<MemberGrantsForm>(
        ['memberGrants', memberId.toString()],
        `/members/${memberId}/changeGrantsForm`
    );
};

export const useUpdateMemberGrants = (memberId: number) => {
    const queryClient = useQueryClient();

    return useApiPutMutation<MemberGrantsForm, void>(
        `/members/${memberId}/changeGrantsForm`,
        {
            onSuccess: () => {
                queryClient.invalidateQueries({queryKey: ['memberGrants', memberId.toString()]});
                queryClient.invalidateQueries({queryKey: ['allGrants']});
            }
        }
    );
};