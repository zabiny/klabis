import type {HalResponse} from '../api';
import {toHref} from '../api/hateoas';
import {useAuthorizedQuery} from './useAuthorizedFetch';

export interface DashboardData {
    upcomingRegistrationsHref: string | undefined;
}

function toDashboardData(response: HalResponse): DashboardData {
    const link = response._links?.upcomingRegistrations;
    return {
        upcomingRegistrationsHref: link ? toHref(link) : undefined,
    };
}

export function useDashboard() {
    return useAuthorizedQuery('/api/dashboard', {
        select: (data) => toDashboardData(data as HalResponse),
        retry: 1,
    });
}
