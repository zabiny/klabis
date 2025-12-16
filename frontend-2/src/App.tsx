import {Navigate, Route, Routes} from 'react-router-dom';
import {ThemeProvider} from '@mui/material/styles';
import CssBaseline from '@mui/material/CssBaseline';
import {AuthProvider, useAuth} from './contexts/AuthContext2';
import Layout from './pages/Layout';
import LoginPage from './pages/LoginPage';

import theme from './theme';
import {SandplacePage} from "./pages/HalNavigatorPage";
import {authConfig} from "./api/klabisUserManager.ts";

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
            <AuthProvider config={authConfig}>
                <Routes>
                    <Route path="/login" element={<LoginPage/>}/>
                    <Route path="/" element={
                        <ProtectedRoute>
                            <Layout/>
                        </ProtectedRoute>
                    }>
                        <Route index element={<SandplacePage/>}/>
                        <Route path="sandplace" element={<SandplacePage/>}/>
                        <Route path="*" element={<SandplacePage/>}/>
                    </Route>
                </Routes>
            </AuthProvider>
        </ThemeProvider>
    );
}

export default App;