import {Navigate, Route, Routes} from 'react-router-dom';
import {ErrorBoundary} from 'react-error-boundary';
import {AuthProvider, useAuth} from './contexts/AuthContext2';
import Layout from './pages/Layout';
import LoginPage from './pages/LoginPage';
import {authConfig} from "./api/klabisUserManager.ts";
import {ThemeProvider} from "./theme/ThemeContext.tsx";
import ErrorFallback from './components/ErrorFallback';
import {GenericHalPage} from "./pages/GenericHalPage.tsx";
import {MemberDetailsPage} from "./pages/MemberDetailsPage.tsx";
import HomePage from "./pages/HomePage.tsx";
import CalendarPage from "./pages/CalendarPage.tsx";
import {SandplacePage} from "./pages/HalNavigatorPage.tsx";

// Protected route component
const ProtectedRoute = ({children}: { children: React.ReactNode }) => {
    const {isAuthenticated, isLoading} = useAuth();

    if (isLoading) {
        return <div>Loading...</div>;
    }

    if (!isAuthenticated) {
        return <Navigate to="/login"/>;
    }

    return <>{children}</>;
};

function App() {
    return (
        <ErrorBoundary FallbackComponent={ErrorFallback}>
            <AuthProvider config={authConfig}>
                <ThemeProvider>
                    <Routes>
                        <Route path="/login" element={<LoginPage/>}/>
                        <Route path="/" element={
                            <ProtectedRoute>
                                <Layout/>
                            </ProtectedRoute>
                        }>
                            <Route index path="/" element={<HomePage/>}/>
                            <Route path="/sandplace" element={<SandplacePage/>}/>
                            <Route path="/calendar-items" element={<CalendarPage/>}/>
                            <Route path="/members/:id" element={<MemberDetailsPage/>}/>
                            <Route path="*" element={<GenericHalPage/>}/>
                        </Route>
                    </Routes>
                </ThemeProvider>
            </AuthProvider>
        </ErrorBoundary>
    );
}

export default App;