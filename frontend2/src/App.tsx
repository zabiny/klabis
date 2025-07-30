import {useState, useEffect} from 'react';
import {Routes, Route, Navigate} from 'react-router-dom';
import {ThemeProvider, createTheme} from '@mui/material/styles';
import CssBaseline from '@mui/material/CssBaseline';
import {AuthProvider, useAuth} from './contexts/AuthContext';
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
            <AuthProvider>
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