import {useEffect, useState} from 'react';
import {Outlet, useNavigate} from 'react-router-dom';
import {styled} from '@mui/material/styles';
import {AppBar, Box, Button, Toolbar, Typography,} from '@mui/material';
import {Logout as LogoutIcon,} from '@mui/icons-material';
import {type AuthUserDetails, useAuth} from '../contexts/AuthContext2';

const drawerWidth = 0;

const Main = styled('main', {shouldForwardProp: (prop) => prop !== 'open'})<{
    open?: boolean;
}>(({theme, open}) => ({
    flexGrow: 1,
    padding: theme.spacing(3),
    transition: theme.transitions.create('margin', {
        easing: theme.transitions.easing.sharp,
        duration: theme.transitions.duration.leavingScreen,
    }),
    marginLeft: 0,
    ...(open && {
        transition: theme.transitions.create('margin', {
            easing: theme.transitions.easing.easeOut,
            duration: theme.transitions.duration.enteringScreen,
        }),
        marginLeft: `${drawerWidth}px`,
    }),
}));

const Layout = () => {
    const navigate = useNavigate();
    const {logout, getUser, isAuthenticated} = useAuth();
    const [userDetails, setUserDetails] = useState<AuthUserDetails | null>(null)
    useEffect(() => {
        const loadUserName = async () => {
            if (isAuthenticated) {
                try {
                    const user = await getUser();
                    setUserDetails(user)
                } catch (error) {
                    console.error('Error loading user name:', error);
                }
            }
        };

        loadUserName();
    }, [isAuthenticated, getUser]);

    const handleLogout = () => {
        logout();
        navigate('/login');
    };

    const handleUserNameClick = () => {
        if (userDetails) {
            navigate(`members/${userDetails.id}`);
        }
    };

    return (
        <Box sx={{display: 'flex'}}>
            <AppBar position="fixed" sx={{zIndex: (theme) => theme.zIndex.drawer + 1}}>
                <Toolbar>
                    <Typography variant="h6" noWrap component="div" sx={{flexGrow: 1}}>
                        Klabis - Členská sekce
                    </Typography>
                    {userDetails && (
                        <Button
                            color="inherit"
                            onClick={handleUserNameClick}
                            sx={{
                                mr: 2,
                                textTransform: 'none',
                                '&:hover': {
                                    backgroundColor: 'rgba(255, 255, 255, 0.1)'
                                }
                            }}
                        >
                            {userDetails.firstName} {userDetails.lastName} [{userDetails.registrationNumber}]
                        </Button>
                    )}
                    <Button color="inherit" onClick={handleLogout} startIcon={<LogoutIcon/>}>
                        Odhlásit
                    </Button>
                </Toolbar>
            </AppBar>
            <Main>
                <Toolbar/>
                <Outlet/>
            </Main>
        </Box>
    );
};

export default Layout;