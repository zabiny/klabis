import {useEffect} from 'react';
import {useNavigate} from 'react-router-dom';
import {Box, Button, Container, Paper, Typography} from '@mui/material';
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

    const handleLogin = () => {
        login();
    };

    if (isLoading) {
        return (
            <Container maxWidth="sm">
                <Box sx={{mt: 8, display: 'flex', flexDirection: 'column', alignItems: 'center'}}>
                    <Typography variant="h5">Načítání...</Typography>
                </Box>
            </Container>
        );
    }

    return (
        <Container maxWidth="sm">
            <Box sx={{mt: 8, display: 'flex', flexDirection: 'column', alignItems: 'center'}}>
                <Paper sx={{p: 4, width: '100%', maxWidth: 500}}>
                    <Typography variant="h4" component="h1" gutterBottom align="center">
                        Klabis - Členská sekce
                    </Typography>
                    <Typography variant="body1" paragraph align="center">
                        Vítejte v členské sekci klubu orientačního běhu. Pro pokračování se prosím přihlaste.
                    </Typography>
                    <Box sx={{mt: 3, display: 'flex', justifyContent: 'center'}}>
                        <Button
                            variant="contained"
                            color="primary"
                            size="large"
                            onClick={handleLogin}
                            disabled={isLoading}
                        >
                            Přihlásit se
                        </Button>
                    </Box>
                </Paper>
            </Box>
        </Container>
    );
};

export default LoginPage;