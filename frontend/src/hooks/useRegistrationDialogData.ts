import {asLinkArray} from '../api/hateoas';
import type {GetEventResource, GetRegistrationResource, HalFormsTemplate, Link} from '../api';
import {useAuthorizedQuery} from './useAuthorizedFetch';

export type RegistrationDialogMode = 'new' | 'edit';

export interface RegistrationDialogInitialValues {
    siCardNumber: string;
    categoryId: string;
    wantsSharedTransport: boolean;
    wantsSharedAccommodation: boolean;
}

export interface RegistrationDialogEventContext {
    name?: string;
    eventDate?: string;
    location?: string | null;
    deadlines?: string[];
    sharedTransportEnabled?: boolean;
    sharedAccommodationEnabled?: boolean;
}

export interface RegistrationDialogData {
    mode: RegistrationDialogMode | undefined;
    template: HalFormsTemplate | undefined;
    memberName: string;
    initialValues: RegistrationDialogInitialValues;
    eventContext: RegistrationDialogEventContext | undefined;
    isLoading: boolean;
    error: unknown;
}

export const useRegistrationDialogData = (registration: Link | null): RegistrationDialogData => {
    const registrationQuery = useAuthorizedQuery<GetRegistrationResource>(registration?.href ?? '', {
        enabled: !!registration,
        retry: false,
    });

    const eventHref = asLinkArray(registrationQuery.data?._links?.event)[0]?.href;

    const eventQuery = useAuthorizedQuery<GetEventResource>(eventHref ?? '', {
        enabled: !!eventHref,
        retry: false,
    });

    const registrationData = registrationQuery.data;
    const registerForEvent = registrationData?._templates?.registerForEvent;
    const editRegistration = registrationData?._templates?.editRegistration;
    const template = registerForEvent ?? editRegistration;
    const mode = registerForEvent ? 'new' : editRegistration ? 'edit' : undefined;

    const event = eventQuery.data;
    const eventContext: RegistrationDialogEventContext | undefined = event
        ? {
            name: event.name,
            eventDate: event.eventDate,
            location: event.location,
            deadlines: event.deadlines,
            sharedTransportEnabled: event.sharedTransportEnabled,
            sharedAccommodationEnabled: event.sharedAccommodationEnabled,
        }
        : undefined;

    return {
        mode,
        template,
        memberName: [registrationData?.firstName, registrationData?.lastName].filter(Boolean).join(' ').trim(),
        initialValues: {
            siCardNumber: registrationData?.siCardNumber ?? '',
            categoryId: mode === 'edit' ? (registrationData?.category?.id ?? '') : '',
            wantsSharedTransport: registrationData?.wantsSharedTransport ?? false,
            wantsSharedAccommodation: registrationData?.wantsSharedAccommodation ?? false,
        },
        eventContext,
        isLoading: registrationQuery.isLoading || (!!eventHref && eventQuery.isLoading),
        error: registrationQuery.error ?? eventQuery.error,
    };
};
