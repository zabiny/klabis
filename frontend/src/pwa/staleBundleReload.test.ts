import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';
import {installStaleBundleReload} from './staleBundleReload';

const CURRENT_BUNDLE = '/assets/index-OLD.js';
const NEW_BUNDLE = '/assets/index-NEW.js';
const HIDDEN_THRESHOLD_MS = 15 * 60 * 1000;

const flushPromises = () => new Promise<void>((resolve) => setTimeout(resolve, 0));

function servedIndexHtml(bundleUrl: string): string {
    return `<!DOCTYPE html><html><head><script type="module" src="${bundleUrl}"></script></head></html>`;
}

describe('staleBundleReload', () => {
    let reload: ReturnType<typeof vi.fn>;
    let fetchMock: ReturnType<typeof vi.fn>;
    let visibilityChange: (() => void) | null;
    let now: number;

    beforeEach(() => {
        reload = vi.fn();
        fetchMock = vi.fn();
        visibilityChange = null;
        now = 0;
        vi.spyOn(Date, 'now').mockImplementation(() => now);
        vi.stubGlobal('fetch', fetchMock);
        vi.spyOn(document, 'addEventListener').mockImplementation((type, listener) => {
            if (type === 'visibilitychange') {
                visibilityChange = listener as unknown as () => void;
            }
        });
        document.head.innerHTML = `<script type="module" src="${CURRENT_BUNDLE}"></script>`;
        installStaleBundleReload(reload);
    });

    afterEach(() => {
        vi.unstubAllGlobals();
        vi.restoreAllMocks();
    });

    function resumeAfter(hiddenForMs: number) {
        Object.defineProperty(document, 'visibilityState', {configurable: true, value: 'hidden'});
        visibilityChange?.();
        now += hiddenForMs;
        Object.defineProperty(document, 'visibilityState', {configurable: true, value: 'visible'});
        visibilityChange?.();
    }

    it('does not check for a new bundle when hidden for less than 15 minutes', async () => {
        resumeAfter(HIDDEN_THRESHOLD_MS - 1);

        await flushPromises();
        expect(fetchMock).not.toHaveBeenCalled();
        expect(reload).not.toHaveBeenCalled();
    });

    it('reloads when the served index.html references a different bundle', async () => {
        fetchMock.mockResolvedValue({ok: true, text: () => Promise.resolve(servedIndexHtml(NEW_BUNDLE))});

        resumeAfter(HIDDEN_THRESHOLD_MS);

        await vi.waitFor(() => expect(reload).toHaveBeenCalledOnce());
        expect(fetchMock).toHaveBeenCalledWith('/index.html', {cache: 'no-store'});
    });

    it('does not reload when the served bundle matches the current one', async () => {
        fetchMock.mockResolvedValue({ok: true, text: () => Promise.resolve(servedIndexHtml(CURRENT_BUNDLE))});

        resumeAfter(HIDDEN_THRESHOLD_MS);

        await flushPromises();
        expect(reload).not.toHaveBeenCalled();
    });

    it('does not reload when the bundle check fails', async () => {
        fetchMock.mockRejectedValue(new Error('offline'));

        resumeAfter(HIDDEN_THRESHOLD_MS);

        await flushPromises();
        expect(reload).not.toHaveBeenCalled();
    });
});
