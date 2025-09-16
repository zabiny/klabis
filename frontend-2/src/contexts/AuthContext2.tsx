import React, {createContext, ReactNode, useContext, useEffect, useState,} from 'react';
import {User, UserManager, WebStorageStateStore,} from 'oidc-client-ts';

export interface AuthUserDetails {
    firstName: string,
    lastName: string,
    id: number,
    registrationNumber: string
}

// Your required interface
interface AuthContextType {
    isAuthenticated: boolean;
    isLoading: boolean;
    login: () => void;
    logout: () => void;
    getAccessToken: () => Promise<string | null>;
    getUser: () => Promise<AuthUserDetails | null>;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

interface AuthProviderProps {
    children: ReactNode;
    config: {
        authority: string;
        client_id: string;
        client_secret?: string;
        redirect_uri: string;
        post_logout_redirect_uri: string;
        response_type?: string;
        scope?: string;
    };
}

let userManager: UserManager;
export const AuthProvider: React.FC<AuthProviderProps> = ({children, config}) => {
    const [user, setUser] = useState<User | null>(null);
    const [isLoading, setIsLoading] = useState(true);
    const [isAuthenticated, setIsAuthenticated] = useState(false);

    useEffect(() => {
        userManager = new UserManager({
            ...config,
            response_type: config.response_type ?? 'code',
            scope: config.scope ?? 'openid profile email',
            userStore: new WebStorageStateStore({store: window.sessionStorage}),
            automaticSilentRenew: true,
            silent_redirect_uri: config.redirect_uri, // Required for silent renew
        });

        // Handle the redirect callback on app load
        if (window.location.pathname === new URL(config.redirect_uri).pathname) {
            userManager
                .signinRedirectCallback()
                .then((user) => {
                    console.log('Signin redirect callback success:', user);
                    setUser(user);
                    setIsAuthenticated(true);
                    setIsLoading(false);
                    // Clean URL after processing
                    window.history.replaceState({}, document.title, '/');
                })
                .catch((err) => {
                    console.error('Signin redirect callback error:', err);
                    setIsLoading(false);
                });
        } else {
            // Try to get existing user from storage
            userManager
                .getUser()
                .then((user) => {
                    if (user && !user.expired) {
                        setUser(user);
                        setIsAuthenticated(true);
                    }
                })
                .catch(console.error)
                .finally(() => setIsLoading(false));
        }

        userManager.events.addUserLoaded((user) => {
            setUser(user);
            setIsAuthenticated(true);
        });

        userManager.events.addUserUnloaded(() => {
            setUser(null);
            setIsAuthenticated(false);
        });

        userManager.events.addAccessTokenExpired(() => {
            console.warn('Access token expired');
            userManager.signinSilent().catch((err) => {
                console.error('Silent renew failed', err);
                logout();
            });
        });

        userManager.events.addSilentRenewError((err) => {
            console.error('Silent renew error:', err);
        });

        userManager.events.addUserSignedOut(() => {
            console.log('User signed out');
            logout();
        });
    }, []);

    const login = () => {
        userManager.signinRedirect();
    };

    const logout = () => {
        userManager.signoutRedirect();
    };

    const getAccessToken = async (): Promise<string | null> => {
        try {
            const currentUser = await userManager.getUser();
            if (currentUser && !currentUser.expired) {
                return currentUser.access_token;
            }

            // Try silent renewal
            const refreshedUser = await userManager.signinSilent();
            if (refreshedUser) {
                setUser(refreshedUser);
                setIsAuthenticated(true);
                return refreshedUser.access_token;
            } else {
                console.warn('Didnt receive any refreshed user details')
            }
        } catch (err) {
            console.error('Error retrieving access token:', err);
            logout();
            return null;
        }
    };

    const getUser = async (): Promise<AuthUserDetails | null> => {
        try {
            const currentUser = await userManager.getUser();
            if (currentUser) {
                return {
                    firstName: currentUser.profile.given_name,
                    lastName: currentUser.profile.family_name,
                    id: parseInt(currentUser.profile.sub),
                    registrationNumber: currentUser.profile.preferred_username
                } as AuthUserDetails;
            }
            return null;
        } catch (err) {
            console.error('Error retrieving user:', err);
            return null;
        }
    };

    return (
        <AuthContext.Provider
            value={{isAuthenticated, isLoading, login, logout, getAccessToken, getUser}}
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
