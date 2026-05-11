import type {HalResponse} from '../api';
import {toHref} from '../api/hateoas';
import {useAuthorizedQuery} from './useAuthorizedFetch';

export interface DashboardData {
    upcomingRegistrationsHref: string | undefined;
    upcomingDeadlinesHref: string | undefined;
}

// TODO: replace with HAL link from backend once /api/dashboard exposes upcomingDeadlines link
//       (analogous to DashboardUpcomingRegistrationsLinkProcessor — trivial ~10-line backend change)
const UPCOMING_DEADLINES_HREF = '/api/events?status=ACTIVE&deadlineWithin=P7D&notRegisteredBy=me&size=5&sort=registrationDeadline,asc';

function toDashboardData(response: HalResponse): DashboardData {
    const registrationsLink = response._links?.upcomingRegistrations;
    const deadlinesLink = response._links?.upcomingDeadlines;
    const hasMemberProfile = !!registrationsLink;
    return {
        upcomingRegistrationsHref: registrationsLink ? toHref(registrationsLink) : undefined,
        upcomingDeadlinesHref: deadlinesLink
            ? toHref(deadlinesLink)
            : hasMemberProfile ? UPCOMING_DEADLINES_HREF : undefined,
    };
}

export function useDashboard() {
    return useAuthorizedQuery('/api/dashboard', {
        select: (data) => toDashboardData(data as HalResponse),
    });
}
