import {
    AlertTriangle,
    Archive,
    Check,
    CircleDot,
    RotateCw,
    XCircle,
} from 'lucide-react';
import type {GetSyncStateResource} from '../../api';

export type SyncStatus = GetSyncStateResource['status'];
export type SyncStatusVariant = 'info' | 'success' | 'warning' | 'error' | 'default';

export const SYNC_STATUS_MAP: Record<SyncStatus, {
    variant: SyncStatusVariant;
    Icon: typeof CircleDot;
    iconName: string;
}> = {
    NEW: {variant: 'info', Icon: CircleDot, iconName: 'CircleDot'},
    IN_SYNC: {variant: 'success', Icon: Check, iconName: 'Check'},
    RETRYING: {variant: 'warning', Icon: RotateCw, iconName: 'RotateCw'},
    CONFLICT: {variant: 'error', Icon: AlertTriangle, iconName: 'AlertTriangle'},
    FAILED: {variant: 'error', Icon: XCircle, iconName: 'XCircle'},
    RETIRED: {variant: 'default', Icon: Archive, iconName: 'Archive'},
};
