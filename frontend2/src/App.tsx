import {Navigate, Route, Routes} from 'react-router-dom';
import {createTheme, ThemeProvider} from '@mui/material/styles';
import CssBaseline from '@mui/material/CssBaseline';
import {AuthProvider, useAuth} from './contexts/AuthContext2';
import Layout from './components/Layout';
import HomePage from './pages/HomePage';
import MembersPage from './pages/MembersPage';
import MemberDetailPage from './pages/MemberDetailPage';
import EventsPage from './pages/EventsPage';
import GroupsPage from './pages/GroupsPage';
import LoginPage from './pages/LoginPage';
import NotFoundPage from './pages/NotFoundPage';

// Create a theme instance
const theme = createTheme({
    palette: {
        primary: {
            main: '#1976d2',
        },
        secondary: {
            main: '#dc004e',
        },
    },
});

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
        <ThemeProvider theme={theme}>
            <CssBaseline/>
            <AuthProvider config={{
                authority: 'http://localhost:8080',
                client_id: 'frontend',
                client_secret: 'fesecret',
                redirect_uri: 'http://localhost:3000/auth/callback', // must match OIDC config
                post_logout_redirect_uri: 'http://localhost:8080/oauth/logout',
            }}>
                <Routes>
                    <Route path="/login" element={<LoginPage/>}/>
                    <Route path="/" element={
                        <ProtectedRoute>
                            <Layout/>
                        </ProtectedRoute>
                    }>
                        <Route index element={<HomePage/>}/>
                        <Route path="members" element={<MembersPage/>}/>
                        <Route path="members/:memberId" element={<MemberDetailPage/>}/>
                        <Route path="events" element={<EventsPage/>}/>
                        <Route path="groups" element={<GroupsPage/>}/>
                        <Route path="*" element={<NotFoundPage/>}/>
                    </Route>
                </Routes>
            </AuthProvider>
        </ThemeProvider>
    );
}

export default App;