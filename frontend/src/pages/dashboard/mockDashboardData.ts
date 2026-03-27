export interface DashboardStats {
    activeMembersCount: number;
    upcomingEventsCount: number;
    totalGroupsCount: number;
}

export interface UpcomingEvent {
    id: string;
    name: string;
    eventDate: string;
    location: string;
    organizer: string;
}

export interface MyEvent {
    id: string;
    name: string;
    eventDate: string;
    location: string;
    registeredAt: string;
}

// Mock data - will be replaced by API calls in a future dashboard endpoint
export const mockStats: DashboardStats = {
    activeMembersCount: 42,
    upcomingEventsCount: 3,
    totalGroupsCount: 5,
};

export const mockUpcomingEvents: UpcomingEvent[] = [
    {id: '1', name: 'Jarní závod', eventDate: '2026-04-15', location: 'Praha', organizer: 'ZBM'},
    {id: '2', name: 'Letní kemp', eventDate: '2026-06-20', location: 'Brno', organizer: 'TBM'},
    {id: '3', name: 'Podzimní turnaj', eventDate: '2026-09-10', location: 'Ostrava', organizer: 'ZBM'},
];

export const mockMyEvents: MyEvent[] = [
    {id: '1', name: 'Jarní závod', eventDate: '2026-04-15', location: 'Praha', registeredAt: '2026-03-20T10:30:00Z'},
];
