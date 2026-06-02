import React, {type ReactNode, useCallback, useEffect, useState,} from 'react';
import {useNavigate} from 'react-router-dom';
import {User, UserManager,} from 'oidc-client-ts';
import {type AuthConfig, createUserManager} from '../api/klabisUserManager.ts';
import {normalizeUrl} from "../api/hateoas.ts";
import {AuthContext, type AuthUserDetails} from './authContext';


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
        memberId: typeof user.profile.member_id === 'string' ? user.profile.member_id : null
    } as AuthUserDetails;
}

export const AuthProvider: React.FC<AuthProviderProps> = ({children, config}) => {
    const navigate = useNavigate();

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
