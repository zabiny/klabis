import {useApiQuery, useApiMutation} from '../hooks/useApi';

// Types based on the API specification
export interface EventListItem {
    id: number;
    date: string;
    name: string;
    organizer: string;
    type: 'T' | 'S';
    web?: string;
    registrationDeadline?: string;
    coordinator?: string;
}

export interface EventsList {
    items: EventListItem[];
}

// Hooks for events API
export const useGetEvents = () => {
    return useApiQuery<EventsList>(['events'], '/events');
};

export const useGetEvent = (eventId: number) => {
    return useApiQuery<EventListItem>(['event', eventId.toString()], `/events/${eventId}`);
};