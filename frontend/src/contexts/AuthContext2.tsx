import React, {createContext, type ReactNode, useCallback, useContext, useEffect, useState,} from 'react';
import {User, UserManager,} from 'oidc-client-ts';
import {type AuthConfig, createUserManager} from '../api/klabisUserManager.ts';
import {normalizeUrl} from "../api/hateoas.ts";

interface AuthContextType {
    isAuthenticated: boolean;
    login: () => void;
    logout: () => void;
    isLoading: boolean;
    getUser: () => AuthUserDetails | null;
}

export interface AuthUserDetails {
    firstName: string,
    lastName: string,
    id: number,
    userName: string,
    isMember: boolean
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

interface AuthProviderProps {
    children: ReactNode;
    config: AuthConfig;
}

function createAuthUserDetails(user: User): AuthUserDetails {
    return {
        firstName: user.profile.given_name,
        lastName: user.profile.family_name,
        id: parseInt(user.profile.sub),
        userName: user.profile.user_name || user.profile.preferred_username,
        isMember: user.profile.is_member === true
    } as AuthUserDetails;
}

export const AuthProvider: React.FC<AuthProviderProps> = ({children, config}) => {

    const [userManager, setUserManager] = useState<UserManager>();
    const [isLoading, setLoading] = useState(true);

    const [authUserDetails, setAuthUserDetails] = useState<AuthUserDetails | null>(null);

    useEffect(() => {
        const userManager = createUserManager({
            onUserLoaded: (user) => setValidUser(user),
            onUserUnloaded: () => setValidUser(null),
            ...config
        });

        setUserManager(userManager);
    }, [config]);

    function setValidUser(user: User | null): void {
        if (user != null && !user.expired) {
            setAuthUserDetails(createAuthUserDetails(user));
        } else {
            setAuthUserDetails(null);
        }
    }

    function isCurrentLocationSameAsRedirectUri(config: { redirect_uri: string }): boolean {
        return new URL(normalizeUrl(config.redirect_uri)).pathname === window.location.pathname;
    }

    useEffect(() => {
        if (userManager != null) {
            // Handle the redirect callback on app load
            if (isCurrentLocationSameAsRedirectUri(config)) {
                // handling callback from OAuth server
                userManager
                    .signinRedirectCallback()
                    .then((user) => {
                        setValidUser(user);
                        setLoading(false);
                        // Clean URL after processing
                        window.history.replaceState({}, document.title, '/');
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

        }
    }, [userManager, config]);

    const login = useCallback(() => {
        userManager?.signinRedirect();
    }, [userManager]);

    const logout = useCallback(async () => {
        if (!userManager) return;
        try {
            const user = await userManager.getUser();
            await userManager.removeUser();
            await userManager.signoutRedirect({ id_token_hint: user?.id_token });
        } catch (err) {
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

export const useAuth = (): AuthContextType => {
    const context = useContext(AuthContext);
    if (!context) {
        throw new Error('useAuth must be used within an AuthProvider');
    }
    return context;
};
