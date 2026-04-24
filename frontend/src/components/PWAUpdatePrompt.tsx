import {usePwaEnabled} from '../hooks/usePwaEnabled';
import {usePwaUpdater} from '../hooks/usePwaUpdater';

// Backend returns 404 on the manifest when the `pwa` Spring profile is off.
// useRegisterSW (inside usePwaUpdater) registers the service worker on mount and
// cannot be called conditionally, so the probe gates the inner component instead
// of being inlined into a single hook.
export function PWAUpdatePrompt() {
    const pwaEnabled = usePwaEnabled();

    if (!pwaEnabled) return null;
    return <PWAUpdatePromptInner/>;
}

function PWAUpdatePromptInner() {
    const {needRefresh, dismiss, update} = usePwaUpdater();

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
                    onClick={dismiss}
                    className="rounded px-3 py-1 text-sm text-gray-600 hover:bg-gray-100"
                >
                    Později
                </button>
                <button
                    type="button"
                    onClick={update}
                    className="rounded bg-teal-600 px-3 py-1 text-sm text-white hover:bg-teal-700"
                >
                    Aktualizovat
                </button>
            </div>
        </div>
    );
}
