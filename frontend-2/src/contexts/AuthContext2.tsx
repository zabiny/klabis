import React, {createContext, type ReactNode, useContext, useEffect, useState,} from 'react';
import {User, UserManager,} from 'oidc-client-ts';
import {type AuthConfig, createUserManager, normalizeUrl} from "./auth";

// Your required interface
interface AuthContextType {
    isAuthenticated: boolean;
    login: () => void;
    logout: () => void;
    isLoading: boolean;
    getUser: () => Promise<AuthUserDetails | null>;
}

export interface AuthUserDetails {
    firstName: string,
    lastName: string,
    id: number,
    registrationNumber: string
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

interface AuthProviderProps {
    children: ReactNode;
    config: AuthConfig;
}

const createAuthUserDetails = (user: User | null | undefined): AuthUserDetails | null => {
    if (user === null || user === undefined) {
        return null;
    }

    if (user.expired) {
        return null;
    }

    return {
        firstName: user.profile.given_name,
        lastName: user.profile.family_name,
        id: parseInt(user.profile.sub),
        registrationNumber: user.profile.preferred_username
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
            onAuthorizationCompleted: () => setLoading(false),
            ...config
        });

        setUserManager(userManager);
    }, [config]);

    const setValidUser = (user: User | null): void => {
        if (user != null && !user.expired) {
            setAuthUserDetails(createAuthUserDetails(user));
        } else {
            setAuthUserDetails(null);
        }
    }

    const isCurrentLocationSameAsRedirectUri = (config: { redirect_uri: string }): boolean => {
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
                        console.log('Signin redirect callback success:', user);
                        setValidUser(user);
                        setLoading(false);
                        // Clean URL after processing
                        window.history.replaceState({}, document.title, '/');
                    })
                    .catch((err) => {
                        console.error('Signin redirect callback error:', err);
                    });
            } else {
                // Try to get existing user from storage
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

    const login = () => {
        userManager?.signinRedirect();
    };

    const logout = () => {
        userManager?.signoutRedirect();
    };

    const getUser = async (): Promise<AuthUserDetails | null> => {
        return authUserDetails;
    };

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
