import React from 'react';
import ReactDOM from 'react-dom/client';
import {BrowserRouter} from 'react-router-dom';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {ReactQueryDevtools} from '@tanstack/react-query-devtools'
import {AuthProvider} from './contexts/AuthContext2';
import {klabisAuthUserManager} from './api/klabisUserManager';
import App from './App';
import {HalRouteProvider} from './contexts/HalRouteContext';
import {registerServiceWorker} from './pwa/registerServiceWorker';
import {installStaleBundleReload} from './pwa/staleBundleReload';
import './index.css';

registerServiceWorker();
if (import.meta.env.PROD) {
    installStaleBundleReload();
}

// Create a client for react-query
const queryClient = new QueryClient({
    defaultOptions: {
        queries: {
            refetchOnWindowFocus: false,
            retry: 1,
        },
    },
});

ReactDOM.createRoot(document.getElementById('root') as HTMLElement).render(
    <React.StrictMode>
        <QueryClientProvider client={queryClient}>
            <BrowserRouter>
                <AuthProvider userManager={klabisAuthUserManager}>
                    <HalRouteProvider>
                        <App/>
                    </HalRouteProvider>
                </AuthProvider>
            </BrowserRouter>
<ReactQueryDevtools initialIsOpen={false}/>
        </QueryClientProvider>
    </React.StrictMode>
);