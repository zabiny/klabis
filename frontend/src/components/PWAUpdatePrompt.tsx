import {useEffect, useState} from 'react';
import {useRegisterSW} from 'virtual:pwa-register/react';

const SW_UPDATE_CHECK_INTERVAL_MS = 60 * 60 * 1000;

// Backend returns 404 on the manifest when the `pwa` Spring profile is off.
// useRegisterSW is a hook and cannot be called conditionally, so the probe gates
// the inner component instead of being pushed into the hook itself.
export function PWAUpdatePrompt() {
    const [pwaEnabled, setPwaEnabled] = useState<boolean | null>(null);

    useEffect(() => {
        const controller = new AbortController();
        fetch('/manifest.webmanifest', {method: 'HEAD', signal: controller.signal})
            .then(r => setPwaEnabled(r.ok))
            .catch(() => {
                if (!controller.signal.aborted) setPwaEnabled(false);
            });
        return () => controller.abort();
    }, []);

    if (!pwaEnabled) return null;
    return <PWAUpdatePromptInner/>;
}

function PWAUpdatePromptInner() {
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

    if (!needRefresh) return null;

    return (
        <div
            role="alert"
            className="fixed bottom-4 right-4 z-50 max-w-sm rounded-lg border border-teal-200 bg-white p-4 shadow-lg"
        >
            <p className="mb-3 text-sm text-gray-800">
                Je k dispozici nová verze aplikace.
            </p>
            <div className="flex justify-end gap-2">
                <button
                    type="button"
                    onClick={() => setNeedRefresh(false)}
                    className="rounded px-3 py-1 text-sm text-gray-600 hover:bg-gray-100"
                >
                    Později
                </button>
                <button
                    type="button"
                    onClick={() => updateServiceWorker(true)}
                    className="rounded bg-teal-600 px-3 py-1 text-sm text-white hover:bg-teal-700"
                >
                    Aktualizovat
                </button>
            </div>
        </div>
    );
}
