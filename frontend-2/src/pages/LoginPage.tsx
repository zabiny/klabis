import {useEffect} from 'react';
import {useNavigate} from 'react-router-dom';
import {Box, Container, Typography} from '@mui/material';
import {useAuth} from '../contexts/AuthContext2';

const LoginPage = () => {
    const {login, isAuthenticated, isLoading} = useAuth();
    const navigate = useNavigate();

    // Redirect to home if already authenticated
    useEffect(() => {
        if (isAuthenticated) {
            navigate('/');
        }
    }, [isAuthenticated, navigate]);

    // // Automaticky zahájí login, pokud není autentizováno a není načítání
    useEffect(() => {
        if (!isAuthenticated && !isLoading) {
            login();
        }
    }, [isAuthenticated, isLoading, login]);

    if (!isAuthenticated && !isLoading) {
        return (
            <Container maxWidth="sm">
                <Box sx={{mt: 8, display: 'flex', flexDirection: 'column', alignItems: 'center'}}>
                    <Typography variant="h5">
                        <button onClick={e => login()}>Login back</button>
                    </Typography>
                </Box>
            </Container>
        );
    }

    // Po přihlášení bude automaticky přesměrován, takže zde není potřeba žádné další UI
    return null;
};

export default LoginPage;