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
    Group as GroupIcon,
    Home as HomeIcon,
    Logout as LogoutIcon,
    Menu as MenuIcon,
    People as PeopleIcon,
} from '@mui/icons-material';
import {useAuth} from '../contexts/AuthContext2';

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
    const [drawerOpen, setDrawerOpen] = useState(false);
    const [userName, setUserName] = useState<string>('');
    const navigate = useNavigate();
    const {logout, getUser, isAuthenticated} = useAuth();

    useEffect(() => {
        const loadUserName = async () => {
            if (isAuthenticated) {
                try {
                    const user = await getUser();
                    if (user && user.profile) {
                        const userName = `${user?.profile.given_name} ${user?.profile.family_name} [${user?.profile.preferred_username}]`
                        setUserName(userName);
                    } else {
                        setUserName('!! uknown !!')
                    }
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
        setDrawerOpen(false);
    };

    const handleLogout = () => {
        logout();
        navigate('/login');
    };

    const menuItems = [
        {text: 'Domů', icon: <HomeIcon/>, path: '/'},
        {text: 'Členové', icon: <PeopleIcon/>, path: '/members'},
        {text: 'Akce', icon: <EventIcon/>, path: '/events'},
        {text: 'Skupiny', icon: <GroupIcon/>, path: '/groups'},
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
                    {userName && (
                        <Typography variant="body1" sx={{mr: 2, color: 'inherit'}}>
                            {userName}
                        </Typography>
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