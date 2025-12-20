import type {Config} from 'tailwindcss'
import defaultTheme from 'tailwindcss/defaultTheme'

const config: Config = {
    content: [
        "./index.html",
        "./src/**/*.{js,ts,jsx,tsx}",
    ],
    darkMode: 'class',
    theme: {
        extend: {
            colors: {
                primary: {
                    DEFAULT: '#f44336',
                    dark: '#d32f2f',
                    light: '#ef5350',
                },
                secondary: {
                    DEFAULT: '#424242',
                    dark: '#212121',
                    light: '#616161',
                },
                background: {
                    DEFAULT: '#121212',
                    paper: '#1e1e1e',
                },
                surface: {
                    DEFAULT: '#ffffff',
                    dark: '#1e1e1e',
                },
                text: {
                    primary: '#ffffff',
                    secondary: 'rgba(255, 255, 255, 0.7)',
                },
                feedback: {
                    success: '#4caf50',
                    warning: '#ff9800',
                    info: '#2196f3',
                    error: '#f44336',
                },
            },
            fontFamily: {
                sans: ['Inter', ...defaultTheme.fontFamily.sans],
                mono: ['Fira Code', ...defaultTheme.fontFamily.mono],
            },
            spacing: {
                '4px': '0.25rem',
                '8px': '0.5rem',
                '12px': '0.75rem',
                '16px': '1rem',
            },
            borderRadius: {
                'sm': '4px',
                'base': '8px',
                'md': '12px',
                'lg': '16px',
            },
            boxShadow: {
                'sm': '0 1px 2px 0 rgba(0, 0, 0, 0.05)',
                'base': '0 1px 3px 0 rgba(0, 0, 0, 0.1), 0 1px 2px 0 rgba(0, 0, 0, 0.06)',
                'md': '0 4px 6px -1px rgba(0, 0, 0, 0.1), 0 2px 4px -1px rgba(0, 0, 0, 0.06)',
                'lg': '0 10px 15px -3px rgba(0, 0, 0, 0.1), 0 4px 6px -2px rgba(0, 0, 0, 0.05)',
                'dark-sm': '0 1px 2px 0 rgba(0, 0, 0, 0.3)',
                'dark-md': '0 4px 6px -1px rgba(0, 0, 0, 0.3)',
                'dark-lg': '0 10px 15px -3px rgba(0, 0, 0, 0.4)',
            },
            transitionDuration: {
                'fast': '150ms',
                'base': '200ms',
                'slow': '300ms',
            },
        },
    },
    plugins: [
        require('@tailwindcss/forms'),
        require('@tailwindcss/typography'),
    ],
}

export default config
