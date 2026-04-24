export interface OrisEvent {
    id: number;
    name: string;
    date: string;
    location: string | null;
    organizer: string | null;
}

export const ORIS_REGION_KEYS = ['JIHOMORAVSKA', 'MORAVA', 'CR'] as const;
export type OrisRegionKey = typeof ORIS_REGION_KEYS[number];
