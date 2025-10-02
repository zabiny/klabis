import {useEffect, useState} from 'react';
import {Outlet, useNavigate} from 'react-router-dom';
import {styled} from '@mui/material/styles';
import {
    AppBar,
    Box,
    Button,
    Divider,
    Drawer,
    IconButton,
    List,
    ListItem,
    ListItemButton,
    ListItemIcon,
    ListItemText,
    Toolbar,
    Typography,
} from '@mui/material';
import {
    Event as EventIcon,
    Home as HomeIcon,
    Logout as LogoutIcon,
    Menu as MenuIcon,
    People as PeopleIcon,
    Settings,
} from '@mui/icons-material';
import {type AuthUserDetails, useAuth} from '../contexts/AuthContext2';

const drawerWidth = 240;

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
    const [drawerOpen, setDrawerOpen] = useState(true);
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

    const handleDrawerToggle = () => {
        setDrawerOpen(!drawerOpen);
    };

    const handleNavigation = (path: string) => {
        navigate(path);
    };

    const handleLogout = () => {
        logout();
        navigate('/login');
    };

    const handleUserNameClick = () => {
        if (userDetails) {
            navigate(`members/${userDetails.id}`);
        }
    };

    const menuItems = [
        {text: 'Domů', icon: <HomeIcon/>, path: '/'},
        {text: 'Členové', icon: <PeopleIcon/>, path: '/members'},
        {text: 'Akce', icon: <EventIcon/>, path: '/events'},
        //{text: 'Skupiny', icon: <GroupIcon/>, path: '/groups'},
        {text: 'Hal Navigator', icon: <Settings/>, path: '/sandplace'},
    ];

    return (
        <Box sx={{display: 'flex'}}>
            <AppBar position="fixed" sx={{zIndex: (theme) => theme.zIndex.drawer + 1}}>
                <Toolbar>
                    <IconButton
                        color="inherit"
                        aria-label="open drawer"
                        edge="start"
                        onClick={handleDrawerToggle}
                        sx={{mr: 2}}
                    >
                        <MenuIcon/>
                    </IconButton>
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
            <Drawer
                variant="persistent"
                anchor="left"
                open={drawerOpen}
                sx={{
                    width: drawerWidth,
                    flexShrink: 0,
                    '& .MuiDrawer-paper': {
                        width: drawerWidth,
                        boxSizing: 'border-box',
                    },
                }}
            >
                <Toolbar/>
                <Box sx={{overflow: 'auto'}}>
                    <List>
                        {menuItems.map((item) => (
                            <ListItem key={item.text} disablePadding>
                                <ListItemButton onClick={() => handleNavigation(item.path)}>
                                    <ListItemIcon>{item.icon}</ListItemIcon>
                                    <ListItemText primary={item.text}/>
                                </ListItemButton>
                            </ListItem>
                        ))}
                    </List>
                    <Divider/>
                </Box>
            </Drawer>
            <Main open={drawerOpen}>
                <Toolbar/>
                <Outlet/>
            </Main>
        </Box>
    );
};

export default Layout;