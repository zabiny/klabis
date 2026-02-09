/**
 * Klabis API Client
 * Handles OAuth2 Authorization Code flow and backend API communication
 */

class KlabisApiClient {
    constructor(config = {}) {
        // UI is served from same origin as backend, so use relative URLs by default
        const origin = window.location.origin;  // e.g., https://localhost:8443
        this.baseUrl = config.baseUrl || origin;
        this.apiBaseUrl = config.apiBaseUrl || `${origin}/api`;
        this.clientId = config.clientId || 'klabis-web';
        this.clientSecret = config.clientSecret || 'test-secret-123';
        this.redirectUri = config.redirectUri || `${origin}/mock/auth/callback.html`;

        // Try to restore token from sessionStorage
        this.accessToken = sessionStorage.getItem('access_token') || null;
        this.refreshToken = sessionStorage.getItem('refresh_token') || null;
        const expiresAt = sessionStorage.getItem('token_expires_at');
        this.tokenExpiresAt = expiresAt ? parseInt(expiresAt) : null;

        console.log('[API Client] Constructor - Restored from sessionStorage:', {
            hasAccessToken: !!this.accessToken,
            hasRefreshToken: !!this.refreshToken,
            tokenExpiresAt: this.tokenExpiresAt ? new Date(this.tokenExpiresAt).toISOString() : 'null',
            isExpired: this.tokenExpiresAt ? Date.now() > this.tokenExpiresAt : 'unknown'
        });
    }

    /**
     * Exchange authorization code for access token
     */
    async exchangeCodeForToken(code, redirectUri) {
        const tokenEndpoint = `${this.baseUrl}/oauth2/token`;
        const basicAuth = btoa(`${this.clientId}:${this.clientSecret}`);

        const params = new URLSearchParams({
            grant_type: 'authorization_code',
            code: code,
            redirect_uri: redirectUri
        });

        try {
            const response = await fetch(tokenEndpoint, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/x-www-form-urlencoded',
                    'Authorization': `Basic ${basicAuth}`
                },
                body: params.toString()
            });

            if (!response.ok) {
                const errorData = await response.json().catch(() => ({}));
                throw new Error(errorData.error_description || `Failed to exchange code: ${response.status} ${response.statusText}`);
            }

            const data = await response.json();
            this.storeTokens(data);

            return this.accessToken;
        } catch (error) {
            console.error('Error exchanging code for token:', error);
            throw error;
        }
    }

    /**
     * Store tokens in sessionStorage
     */
    storeTokens(tokenData) {
        this.accessToken = tokenData.access_token;
        this.refreshToken = tokenData.refresh_token || null;

        // Set expiry time (subtract 60 seconds buffer)
        const expiresIn = tokenData.expires_in || 43199; // Default ~12 hours
        this.tokenExpiresAt = Date.now() + ((expiresIn - 60) * 1000);

        console.log('[API Client] Tokens stored:', {
            accessToken: this.accessToken ? '***' + this.accessToken.slice(-10) : 'null',
            refreshToken: this.refreshToken ? 'exists' : 'null',
            expiresIn,
            expiresAt: new Date(this.tokenExpiresAt).toISOString(),
            tokenExpiresAt: this.tokenExpiresAt,
            now: Date.now()
        });

        // Persist to sessionStorage
        sessionStorage.setItem('access_token', this.accessToken);
        if (this.refreshToken) {
            sessionStorage.setItem('refresh_token', this.refreshToken);
        }
        sessionStorage.setItem('token_expires_at', this.tokenExpiresAt.toString());
    }

    /**
     * Get OAuth2 access token
     * Returns stored token if valid, otherwise attempts refresh
     */
    async getAccessToken() {
        console.log('[API Client] getAccessToken called:', {
            hasAccessToken: !!this.accessToken,
            hasExpiresAt: !!this.tokenExpiresAt,
            tokenExpiresAt: this.tokenExpiresAt ? new Date(this.tokenExpiresAt).toISOString() : 'null',
            now: new Date().toISOString(),
            isValid: this.accessToken && this.tokenExpiresAt && Date.now() < this.tokenExpiresAt
        });

        // Check if we have a valid token
        if (this.accessToken && this.tokenExpiresAt && Date.now() < this.tokenExpiresAt) {
            console.log('[API Client] Token is valid, returning cached token');
            return this.accessToken;
        }

        // Token is expired, try to refresh if we have a refresh token
        if (this.refreshToken) {
            console.log('[API Client] Token expired, attempting refresh...');
            try {
                await this.refreshAccessToken();
                console.log('[API Client] Token refreshed successfully');
                return this.accessToken;
            } catch (error) {
                console.error('Token refresh failed:', error);
                // Clear tokens and force re-login
                this.clearTokens();
                throw new Error('Session expired. Please login again.');
            }
        }

        // No valid token available
        console.log('[API Client] No valid token available');
        throw new Error('No valid access token. Please login first.');
    }

    /**
     * Refresh access token using refresh token
     */
    async refreshAccessToken() {
        const tokenEndpoint = `${this.baseUrl}/oauth2/token`;
        const basicAuth = btoa(`${this.clientId}:${this.clientSecret}`);

        const params = new URLSearchParams({
            grant_type: 'refresh_token',
            refresh_token: this.refreshToken
        });

        const response = await fetch(tokenEndpoint, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/x-www-form-urlencoded',
                'Authorization': `Basic ${basicAuth}`
            },
            body: params.toString()
        });

        if (!response.ok) {
            throw new Error(`Failed to refresh token: ${response.status} ${response.statusText}`);
        }

        const data = await response.json();
        this.storeTokens(data);

        return this.accessToken;
    }

    /**
     * Make authenticated API call
     */
    async apiCall(endpoint, options = {}) {
        const token = await this.getAccessToken();

        const url = endpoint.startsWith('http') ? endpoint : `${this.apiBaseUrl}${endpoint}`;

        const defaultOptions = {
            headers: {
                'Authorization': `Bearer ${token}`,
                'Content-Type': 'application/json',
                'Accept': 'application/prs.hal-forms+json'
            }
        };

        const mergedOptions = {
            ...defaultOptions,
            ...options,
            headers: {
                ...defaultOptions.headers,
                ...options.headers
            }
        };

        try {
            const response = await fetch(url, mergedOptions);

            if (!response.ok) {
                // Handle error responses
                const errorData = await response.json().catch(() => ({}));
                throw new ApiError(response.status, response.statusText, errorData);
            }

            return await response.json();
        } catch (error) {
            if (error instanceof ApiError) {
                throw error;
            }
            console.error('API call error:', error);
            throw error;
        }
    }

    /**
     * Get members list with pagination and sorting
     */
    async getMembers(params = {}) {
        const {page = 0, size = 10, sort = 'lastName,asc'} = params;
        const queryParams = new URLSearchParams({
            page: page.toString(),
            size: size.toString(),
            sort: sort
        });

        return this.apiCall(`/members?${queryParams}`);
    }

    /**
     * Get member by ID
     */
    async getMember(id) {
        return this.apiCall(`/members/${id}`);
    }

    /**
     * Create new member
     */
    async createMember(memberData) {
        return this.apiCall('/members', {
            method: 'POST',
            body: JSON.stringify(memberData)
        });
    }

    /**
     * Update member using PATCH
     */
    async updateMember(id, memberData) {
        return this.apiCall(`/members/${id}`, {
            method: 'PATCH',
            body: JSON.stringify(memberData)
        });
    }

    /**
     * Get user permissions
     */
    async getUserPermissions(userId) {
        return this.apiCall(`/users/${userId}/permissions`);
    }

    /**
     * Update user permissions
     */
    async updateUserPermissions(userId, authorities) {
        return this.apiCall(`/users/${userId}/permissions`, {
            method: 'PUT',
            body: JSON.stringify({authorities})
        });
    }

    // ============ EVENTS API ============

    /**
     * Get events list with pagination and filters
     */
    async getEvents(params = {}) {
        const {page = 0, size = 10, status = '', organizer = '', eventDateFrom = '', eventDateTo = ''} = params;
        const queryParams = new URLSearchParams();
        queryParams.append('page', page.toString());
        queryParams.append('size', size.toString());
        if (status) queryParams.append('status', status);
        if (organizer) queryParams.append('organizer', organizer);
        if (eventDateFrom) queryParams.append('eventDateFrom', eventDateFrom);
        if (eventDateTo) queryParams.append('eventDateTo', eventDateTo);

        return this.apiCall(`/events?${queryParams}`);
    }

    /**
     * Get event by ID
     */
    async getEvent(id) {
        return this.apiCall(`/events/${id}`);
    }

    /**
     * Create new event
     */
    async createEvent(eventData) {
        return this.apiCall('/events', {
            method: 'POST',
            body: JSON.stringify(eventData)
        });
    }

    /**
     * Update event using PATCH
     */
    async updateEvent(id, eventData) {
        return this.apiCall(`/events/${id}`, {
            method: 'PATCH',
            body: JSON.stringify(eventData)
        });
    }

    /**
     * Publish event (DRAFT -> ACTIVE)
     */
    async publishEvent(id) {
        return this.apiCall(`/events/${id}/publish`, {
            method: 'POST',
            body: '{}'
        });
    }

    /**
     * Cancel event (DRAFT/ACTIVE -> CANCELLED)
     */
    async cancelEvent(id) {
        return this.apiCall(`/events/${id}/cancel`, {
            method: 'POST',
            body: '{}'
        });
    }

    /**
     * Finish event (ACTIVE -> FINISHED)
     */
    async finishEvent(id) {
        return this.apiCall(`/events/${id}/finish`, {
            method: 'POST',
            body: '{}'
        });
    }

    // ============ EVENT REGISTRATIONS API ============

    /**
     * Get registrations for an event (without SI card numbers)
     */
    async getEventRegistrations(eventId) {
        return this.apiCall(`/events/${eventId}/registrations`);
    }

    /**
     * Get own registration for an event (includes SI card number)
     */
    async getMyEventRegistration(eventId) {
        return this.apiCall(`/events/${eventId}/registrations/me`);
    }

    /**
     * Register for an event
     */
    async registerForEvent(eventId, registrationData) {
        return this.apiCall(`/events/${eventId}/registrations`, {
            method: 'POST',
            body: JSON.stringify(registrationData)
        });
    }

    /**
     * Unregister from an event
     */
    async unregisterFromEvent(eventId) {
        return this.apiCall(`/events/${eventId}/registrations`, {
            method: 'DELETE'
        });
    }

    /**
     * Clear stored tokens (for logout)
     */
    clearTokens() {
        this.accessToken = null;
        this.refreshToken = null;
        this.tokenExpiresAt = null;
        sessionStorage.removeItem('access_token');
        sessionStorage.removeItem('refresh_token');
        sessionStorage.removeItem('token_expires_at');
    }

    /**
     * Clear token (for logout) - deprecated, use clearTokens
     */
    clearToken() {
        this.clearTokens();
    }
}

/**
 * Custom API Error class
 */
class ApiError extends Error {
    constructor(status, statusText, details = {}) {
        super(`API Error: ${status} ${statusText}`);
        this.status = status;
        this.statusText = statusText;
        this.details = details;
    }

    isUnauthorized() {
        return this.status === 401;
    }

    isForbidden() {
        return this.status === 403;
    }

    isNotFound() {
        return this.status === 404;
    }

    isValidationError() {
        return this.status === 400;
    }

    isConflict() {
        return this.status === 409;
    }

    getErrorMessage() {
        if (this.details.detail) {
            return this.details.detail;
        }
        if (this.details.message) {
            return this.details.message;
        }
        return this.statusText;
    }
}

// Create global API client instance
const apiClient = new KlabisApiClient();

// Export for use in other scripts
if (typeof module !== 'undefined' && module.exports) {
    module.exports = {KlabisApiClient, ApiError, apiClient};
}
