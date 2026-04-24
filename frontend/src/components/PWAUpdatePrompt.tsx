import {useEffect, useState} from 'react';
import {useRegisterSW} from 'virtual:pwa-register/react';

/**
 * Renders the SW registration only when the backend exposes a manifest.
 * Backend hides the manifest (404) when the `pwa` Spring profile is off,
 * so this component must skip SW registration entirely in that case.
 */
export function PWAUpdatePrompt() {
    const [pwaEnabled, setPwaEnabled] = useState<boolean | null>(null);

    useEffect(() => {
        fetch('/manifest.webmanifest', {method: 'HEAD'})
            .then(r => setPwaEnabled(r.ok))
            .catch(() => setPwaEnabled(false));
    }, []);

    if (!pwaEnabled) return null;
    return <PWAUpdatePromptInner/>;
}

function PWAUpdatePromptInner() {
    const {
        needRefresh: [needRefresh, setNeedRefresh],
        updateServiceWorker,
    } = useRegisterSW({
        onRegisteredSW(_swUrl, registration) {
            if (registration) {
                setInterval(() => registration.update(), 60 * 60 * 1000);
            }
        },
    });

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
