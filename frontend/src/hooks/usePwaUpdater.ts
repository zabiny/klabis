import {useEffect, useState} from 'react';
import {useRegisterSW} from 'virtual:pwa-register/react';

const SW_UPDATE_CHECK_INTERVAL_MS = 60 * 60 * 1000;

export interface PwaUpdater {
    needRefresh: boolean;
    dismiss: () => void;
    update: () => void;
}

/**
 * Encapsulates service worker registration, periodic update checks, and the
 * "new version available" refresh prompt state.
 *
 * Must only be rendered when PWA is confirmed enabled — useRegisterSW registers
 * the service worker on mount, so this hook must not be called when PWA is off.
 */
export function usePwaUpdater(): PwaUpdater {
    const [registration, setRegistration] = useState<ServiceWorkerRegistration | null>(null);
    const {
        needRefresh: [needRefresh, setNeedRefresh],
        updateServiceWorker,
    } = useRegisterSW({
        onRegisteredSW(_swUrl, reg) {
            if (reg) setRegistration(reg);
        },
    });

    useEffect(() => {
        if (!registration) return;
        const id = setInterval(() => registration.update(), SW_UPDATE_CHECK_INTERVAL_MS);
        return () => clearInterval(id);
    }, [registration]);

    return {
        needRefresh,
        dismiss: () => setNeedRefresh(false),
        update: () => updateServiceWorker(true),
    };
}
