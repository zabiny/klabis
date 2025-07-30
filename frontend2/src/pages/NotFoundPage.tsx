import {Box, Typography, Button, Paper} from '@mui/material';
import {Link as RouterLink} from 'react-router-dom';
import {Home as HomeIcon} from '@mui/icons-material';

const NotFoundPage = () => {
    return (
        <Box
            sx={{
                display: 'flex',
                justifyContent: 'center',
                alignItems: 'center',
                minHeight: '80vh',
            }}
        >
            <Paper
                elevation={3}
                sx={{
                    p: 5,
                    display: 'flex',
                    flexDirection: 'column',
                    alignItems: 'center',
                    maxWidth: 500,
                }}
            >
                <Typography variant="h1" color="primary" sx={{mb: 2, fontSize: '6rem'}}>
                    404
                </Typography>
                <Typography variant="h4" component="h1" gutterBottom>
                    Stránka nenalezena
                </Typography>
                <Typography variant="body1" align="center" paragraph>
                    Omlouváme se, ale stránka, kterou hledáte, neexistuje nebo byla přesunuta.
                </Typography>
                <Button
                    component={RouterLink}
                    to="/"
                    variant="contained"
                    color="primary"
                    startIcon={<HomeIcon/>}
                    sx={{mt: 2}}
                >
                    Zpět na úvodní stránku
                </Button>
            </Paper>
        </Box>
    );
};

export default NotFoundPage;