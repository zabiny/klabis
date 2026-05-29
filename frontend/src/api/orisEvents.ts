export interface OrisEvent {
    id: number;
    name: string;
    date: string;
    location: string | null;
    organizer: string | null;
}

export const ORIS_REGION_KEYS = ['JIHOMORAVSKA', 'MORAVA', 'CR'] as const;
export type OrisRegionKey = typeof ORIS_REGION_KEYS[number];

export interface BulkImportResultItem {
    orisId: number;
    name: string | null;
    date: string | null;
    status: 'IMPORTED' | 'FAILED';
    error?: string;
}

export interface BulkImportResult {
    totalProcessed: number;
    successCount: number;
    failureCount: number;
    results: BulkImportResultItem[];
}
