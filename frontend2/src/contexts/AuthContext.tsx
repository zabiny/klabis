import React, {createContext, useContext, useState, useEffect, ReactNode} from 'react';
import {AuthProvider as OAuth2Provider, useAuth as useOAuth2} from 'react-oauth2-pkce';

// Define the auth context type
interface AuthContextType {
    isAuthenticated: boolean;
    isLoading: boolean;
    login: () => void;
    logout: () => void;
    getAccessToken: () => string | null;
}

// Create the auth context
const AuthContext = createContext<AuthContextType | undefined>(undefined);

// Auth provider props
interface AuthProviderProps {
    children: ReactNode;
}

// Auth provider component
export const AuthProvider: React.FC<AuthProviderProps> = ({children}) => {
    // OAuth2 configuration
    const authConfig = {
        clientId: 'klabis-frontend',
        authorizationEndpoint: 'https://klabis-auth.polach.cloud/oauth2/authorize',
        tokenEndpoint: 'https://klabis-auth.polach.cloud/oauth2/token',
        redirectUri: window.location.origin,
        scope: 'openid',
        autoLogin: false,
        onRefreshTokenExpire: (event: any) => window.confirm('Your session has expired. Do you want to login again?') && event.login(),
    };

    return (
        <OAuth2Provider authConfig={authConfig}>
            <AuthProviderContent>{children}</AuthProviderContent>
        </OAuth2Provider>
    );
};

// Auth provider content component
const AuthProviderContent: React.FC<AuthProviderProps> = ({children}) => {
    const {login, logout, token, tokenData, isLoading} = useOAuth2();
    const [isAuthenticated, setIsAuthenticated] = useState<boolean>(false);

    // Check if the user is authenticated
    useEffect(() => {
        setIsAuthenticated(!!token);
    }, [token]);

    // Get the access token
    const getAccessToken = (): string | null => {
        return token || null;
    };

    // Create the auth context value
    const authContextValue: AuthContextType = {
        isAuthenticated,
        isLoading,
        login,
        logout,
        getAccessToken,
    };

    return (
        <AuthContext.Provider value={authContextValue}>
            {children}
        </AuthContext.Provider>
    );
};

// Custom hook to use the auth context
export const useAuth = (): AuthContextType => {
    const context = useContext(AuthContext);
    if (context === undefined) {
        throw new Error('useAuth must be used within an AuthProvider');
    }
    console.log(context);
    return context;
};