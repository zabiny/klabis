import {useContext} from 'react';
import {AuthContext} from '../contexts/authContext';

/**
 * True once the user is authenticated and auth state is resolved. Outside an AuthProvider
 * (e.g. isolated component tests) there is nothing to wait for, so it reports ready.
 */
export function useIsAuthReady(): boolean {
    const auth = useContext(AuthContext);
    return auth === undefined || (auth.isAuthenticated && !auth.isLoading);
}
