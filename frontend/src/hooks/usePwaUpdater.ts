import {useEffect, useState} from 'react';
import {useRegisterSW} from 'virtual:pwa-register/react';

const SW_UPDATE_CHECK_INTERVAL_MS = 15 * 60 * 1000;

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
        const checkForUpdate = () => registration.update();

        const intervalId = setInterval(checkForUpdate, SW_UPDATE_CHECK_INTERVAL_MS);
        // Check immediately when the tab regains focus or the device comes online,
        // so a release shipped while the tab was idle is picked up without waiting
        // for the next interval tick.
        const onVisibilityChange = () => {
            if (document.visibilityState === 'visible') checkForUpdate();
        };
        document.addEventListener('visibilitychange', onVisibilityChange);
        window.addEventListener('online', checkForUpdate);

        return () => {
            clearInterval(intervalId);
            document.removeEventListener('visibilitychange', onVisibilityChange);
            window.removeEventListener('online', checkForUpdate);
        };
    }, [registration]);

    return {
        needRefresh,
        dismiss: () => setNeedRefresh(false),
        update: () => updateServiceWorker(true),
    };
}
