import {useEffect} from 'react';
import {Outlet, Route, Routes} from 'react-router-dom';
import {ErrorBoundary} from 'react-error-boundary';
import {AuthProvider, useAuth} from './contexts/AuthContext2';
import Layout from './pages/Layout';
import LoginPage from './pages/LoginPage';
import {authConfig} from "./api/klabisUserManager.ts";
import {ThemeProvider} from "./theme/ThemeContext.tsx";
import {AdminModeProvider, useAdminMode} from "./contexts/AdminModeContext.tsx";
import ErrorFallback from './components/ErrorFallback';
import {GenericHalPage} from "./pages/GenericHalPage.tsx";
import HomePage from "./pages/HomePage.tsx";
import CalendarPage from "./pages/calendar/CalendarPage.tsx";
import {SandplacePage} from "./pages/HalNavigatorPage.tsx";
import {EventsPage} from "./pages/events/EventsPage.tsx";
import {MemberFinancePage} from "./pages/finances/FinancesPage.tsx";
import {MembersPage} from "./pages/members/MembersPage.tsx";
import {MemberDetailPage} from "./pages/members/MemberDetailPage.tsx";
import {MemberRegistrationPage} from "./pages/members/MemberRegistrationPage.tsx";
import PasswordSetupPage from "./pages/PasswordSetupPage";
import PasswordExpiredPage from "./pages/PasswordExpiredPage";

// Protected route component
const ProtectedRoute = ({children}: { children: React.ReactNode }) => {
    const {isAuthenticated, isLoading, login} = useAuth();

    useEffect(() => {
        if (!isAuthenticated && !isLoading) {
            // Trigger OIDC authorization code flow — Spring will redirect to /login (React SPA)
            // after storing the OAuth2 authorization request state in session
            login();
        }
    }, [isAuthenticated, isLoading, login]);

    if (isLoading || !isAuthenticated) {
        return null;
    }

    return <>{children}</>;
};

// Wrapper component that checks admin mode and redirects to GenericHalPage if needed
const AdminModeAwareRoute = ({ allowedInAdminMode }: { allowedInAdminMode: boolean }) => {
    const { isAdminMode } = useAdminMode();

    if (isAdminMode && !allowedInAdminMode) {
        // In admin mode and route is not allowed - show GenericHalPage instead
        return <GenericHalPage />;
    }

    return <Outlet />;
};

function App() {
    return (
        <ErrorBoundary FallbackComponent={ErrorFallback}>
            <AuthProvider config={authConfig}>
                <ThemeProvider>
                    <AdminModeProvider>
                        <Routes>
                            {/* Public routes - no authentication required */}
                            <Route path="/password-setup" element={<PasswordSetupPage />} />
                            <Route path="/password-setup/request" element={<PasswordExpiredPage />} />
                            <Route path="/login" element={<LoginPage/>}/>

                            {/* Protected routes - authentication required */}
                            <Route path="/" element={
                                <ProtectedRoute>
                                    <Layout/>
                                </ProtectedRoute>
                            }>
                                {/* Vždy povolené trasy */}
                                <Route index path="/" element={<HomePage/>}/>
                                <Route path="/index.html" element={<HomePage/>}/>
                                <Route path="/sandplace" element={<SandplacePage/>}/>

                                {/* Trasy zakázané v admin módu */}
                                <Route element={<AdminModeAwareRoute allowedInAdminMode={false} />}>
                                    <Route path="/calendar-items" element={<CalendarPage/>}/>
                                    <Route path="/member/:memberId/finance-account" element={<MemberFinancePage/>}/>
                                    <Route path="/members" element={<MembersPage/>}/>
                                    <Route path="/members/new" element={<MemberRegistrationPage/>}/>
                                    <Route path="/members/:id" element={<MemberDetailPage/>}/>
                                    <Route path="/events" element={<EventsPage/>}/>
                                </Route>

                                {/* Catch-all - vždy povoleno (zachytí i ignorované routes v admin módu) */}
                                <Route path="*" element={<GenericHalPage/>}/>
                            </Route>
                        </Routes>
                    </AdminModeProvider>
                </ThemeProvider>
            </AuthProvider>
        </ErrorBoundary>
    );
}

export default App;