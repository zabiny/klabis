import {useState} from 'react';
import {Outlet} from 'react-router-dom';
import {styled} from '@mui/material/styles';
import {
    AppBar,
    Box,
    Drawer,
    IconButton,
    List,
    ListItem,
    ListItemButton,
    ListItemIcon,
    ListItemText,
    Toolbar,
    Typography,
    Divider,
    Button,
} from '@mui/material';
import {
    Menu as MenuIcon,
    Home as HomeIcon,
    People as PeopleIcon,
    Event as EventIcon,
    Group as GroupIcon,
    Logout as LogoutIcon,
} from '@mui/icons-material';
import {useNavigate} from 'react-router-dom';
import {useAuth} from '../contexts/AuthContext';

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
    const navigate = useNavigate();
    const {logout} = useAuth();

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