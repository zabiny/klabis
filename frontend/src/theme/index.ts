import {createTheme} from '@mui/material/styles';

// Define the color palette for orienteering club
const theme = createTheme({
    palette: {
        mode: 'dark',
        primary: {
            main: '#f44336', // červená
            contrastText: '#ffffff', // bílá
        },
        secondary: {
            main: '#424242', // tmavě šedá
            contrastText: '#ffffff', // bílá
        },
        background: {
            default: '#121212', // tmavé pozadí
            paper: '#1e1e1e', // tmavší pozadí pro komponenty
        },
        text: {
            primary: '#ffffff', // bílý text
            secondary: 'rgba(255, 255, 255, 0.7)', // průhledný bílý text
        },
        error: {
            main: '#f44336', // červená pro chyby
        },
        success: {
            main: '#4caf50', // zelená pro úspěch
        },
        warning: {
            main: '#ff9800', // oranžová pro varování
        },
        info: {
            main: '#2196f3', // modrá pro informace
        },
    },
    components: {
        MuiAppBar: {
            styleOverrides: {
                root: {
                    backgroundColor: '#424242', // tmavě šedá pro app bar
                },
            },
        },
        MuiCard: {
            styleOverrides: {
                root: {
                    backgroundColor: '#1e1e1e', // tmavé pozadí pro karty
                },
            },
        },
    },
});

export default theme;
