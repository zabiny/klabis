import {createContext, useContext} from 'react';

export interface AuthUserDetails {
    firstName: string,
    lastName: string,
    id: number,
    userName: string,
    memberId: string | null
}

interface AuthContextType {
    isAuthenticated: boolean;
    login: () => void;
    logout: () => void;
    isLoading: boolean;
    getUser: () => AuthUserDetails | null;
}

export const AuthContext = createContext<AuthContextType | undefined>(undefined);

export const useAuth = (): AuthContextType => {
    const context = useContext(AuthContext);
    if (!context) {
        throw new Error('useAuth must be used within an AuthProvider');
    }
    return context;
};
