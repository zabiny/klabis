const KEY = 'klabis.push.registeredEndpoint';

export function getRegisteredEndpoint(): string | null {
    try {
        return localStorage.getItem(KEY);
    } catch {
        return null;
    }
}

export function setRegisteredEndpoint(endpoint: string): void {
    try {
        localStorage.setItem(KEY, endpoint);
    } catch {
        // storage unavailable: re-registration after a subscription change is then skipped
    }
}

export function clearRegisteredEndpoint(): void {
    try {
        localStorage.removeItem(KEY);
    } catch {
        // see setRegisteredEndpoint
    }
}
