import {useEffect, useState} from 'react';

/**
 * Probes the manifest endpoint to determine whether the PWA Spring profile is active.
 * Returns null while the probe is in flight, then true/false once resolved.
 *
 * Backend returns 404 on /manifest.webmanifest when the `pwa` Spring profile is off,
 * so a failed probe means PWA is disabled — not a transient network error.
 */
export function usePwaEnabled(): boolean | null {
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

    return pwaEnabled;
}
