import React, {type ReactNode, useCallback, useEffect, useRef, useState,} from 'react';
import {useNavigate} from 'react-router-dom';
import {User, UserManager,} from 'oidc-client-ts';
import {normalizeUrl} from "../api/hateoas.ts";
import {AuthContext, type AuthUserDetails} from './authContext';
import {unsubscribeCurrentDevice} from '../pwa/pushSubscription';
import {clearRegisteredEndpoint} from '../pwa/pushSubscriptionStore';

const PUSH_UNSUBSCRIBE_TIMEOUT_MS = 1500;

async function unsubscribePushBestEffort(): Promise<void> {
    if (typeof PushManager === 'undefined') {
        clearRegisteredEndpoint();
        return;
    }
    let timer: ReturnType<typeof setTimeout> | undefined;
    const timeout = new Promise<void>((resolve) => {
        timer = setTimeout(resolve, PUSH_UNSUBSCRIBE_TIMEOUT_MS);
    });
    try {
        await Promise.race([unsubscribeCurrentDevice(), timeout]);
    } finally {
        clearTimeout(timer);
    }
}


interface AuthProviderProps {
    children: ReactNode;
    userManager: UserManager;
}

function createAuthUserDetails(user: User): AuthUserDetails {
    return {
        firstName: user.profile.given_name,
        lastName: user.profile.family_name,
        id: parseInt(user.profile.sub),
        userName: user.profile.user_name || user.profile.preferred_username,
        memberId: typeof user.profile.member_id === 'string' ? user.profile.member_id : null
    } as AuthUserDetails;
}

export const AuthProvider: React.FC<AuthProviderProps> = ({children, userManager}) => {
    const navigate = useNavigate();

    const [isLoading, setLoading] = useState(true);

    const [authUserDetails, setAuthUserDetails] = useState<AuthUserDetails | null>(null);

    useEffect(() => {
        const onUserLoaded = (user: User) => setValidUser(user);
        const onUserUnloaded = () => setValidUser(null);
        userManager.events.addUserLoaded(onUserLoaded);
        userManager.events.addUserUnloaded(onUserUnloaded);
        return () => {
            userManager.events.removeUserLoaded(onUserLoaded);
            userManager.events.removeUserUnloaded(onUserUnloaded);
        };
    }, [userManager]);

    function setValidUser(user: User | null): void {
        if (user != null && !user.expired) {
            setAuthUserDetails(createAuthUserDetails(user));
        } else {
            setAuthUserDetails(null);
        }
    }

    function isCurrentLocationSameAsRedirectUri(redirectUri: string): boolean {
        return new URL(normalizeUrl(redirectUri)).pathname === window.location.pathname;
    }

    // StrictMode runs effects twice on mount; the authorization code can be exchanged only once.
    const initialized = useRef(false);

    useEffect(() => {
        if (initialized.current) return;
        initialized.current = true;
        // Handle the redirect callback on app load
        if (isCurrentLocationSameAsRedirectUri(userManager.settings.redirect_uri)) {
            // handling callback from OAuth server
            userManager
                .signinRedirectCallback()
                .then((user) => {
                    setValidUser(user);
                    setLoading(false);
                    navigate('/', {replace: true});
                })
                .catch((err) => {
                    console.error('Signin redirect callback error:', err);
                    setLoading(false);
                });
        } else {
            userManager
                .getUser()
                .then((user) => {
                    setValidUser(user);
                })
                .catch(console.error)
                .finally(() => setLoading(false));
        }
    }, [userManager]);

    // signoutRedirect clears the stored user, which makes ProtectedRoute call login(); a signin navigation would supersede the signout one.
    const logoutInProgress = useRef(false);

    const login = useCallback(() => {
        if (logoutInProgress.current) return;
        userManager.signinRedirect();
    }, [userManager]);

    const logout = useCallback(async () => {
        if (!userManager) return;
        logoutInProgress.current = true;
        await unsubscribePushBestEffort();
        try {
            await userManager.signoutRedirect();
        } catch (err) {
            logoutInProgress.current = false;
            console.error('Logout error:', err);
        }
    }, [userManager]);

    const getUser = (): AuthUserDetails | null => authUserDetails;

    return (
        <AuthContext.Provider
            value={{isAuthenticated: authUserDetails !== null, isLoading, login, logout, getUser}}
        >
            {children}
        </AuthContext.Provider>
    );
};
