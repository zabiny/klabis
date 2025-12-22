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
                // Neutral base colors
                black: '#0f0f0f',
                dark: '#1a1a1a',
                surface: {
                    DEFAULT: '#242424',
                    base: '#1a1a1a',
                    raised: '#242424',
                },
                // Primary action color (blue)
                primary: {
                    DEFAULT: '#3b82f6',
                    light: '#60a5fa',
                    dark: '#1e40af',
                },
                // Secondary action color
                secondary: {
                    DEFAULT: '#6366f1',
                    light: '#818cf8',
                    dark: '#4f46e5',
                },
                // Semantic colors
                feedback: {
                    success: '#10b981',
                    warning: '#f59e0b',
                    error: '#ef4444',
                    info: '#06b6d4',
                },
                // Focus/highlight accent
                accent: {
                    DEFAULT: '#8b5cf6',
                    light: '#a78bfa',
                    dark: '#7c3aed',
                },
                // Text colors
                text: {
                    primary: '#f5f5f5',
                    secondary: '#a0a0a0',
                    tertiary: '#707070',
                },
                // Border colors
                border: {
                    DEFAULT: '#2a2a2a',
                    light: '#3a3a3a',
                    dark: '#1a1a1a',
                },
                // Alert backgrounds (for dark mode)
                alert: {
                    success: '#064e3b',
                    warning: '#78350f',
                    error: '#7f1d1d',
                    info: '#164e63',
                },
                // Alert text colors
                'alert-text': {
                    success: '#d1fae5',
                    warning: '#fef3c7',
                    error: '#fee2e2',
                    info: '#cffafe',
                },
            },
            fontFamily: {
                sans: ['Geist', 'Sohne', ...defaultTheme.fontFamily.sans],
                display: ['Sohne', ...defaultTheme.fontFamily.sans],
                mono: ['Fira Code', ...defaultTheme.fontFamily.mono],
            },
            spacing: {
                'xs': '0.125rem', // 2px
                'sm': '0.25rem',  // 4px
                'md': '0.5rem',   // 8px
                'lg': '0.75rem',  // 12px
                'xl': '1rem',     // 16px
                '2xl': '1.5rem',  // 24px
                '3xl': '2rem',    // 32px
                '4xl': '3rem',    // 48px
            },
            borderRadius: {
                'sm': '6px',
                'base': '8px',
                'md': '8px',
                'lg': '12px',
                'pill': '999px',
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
            keyframes: {
                'fade-in': {
                    '0%': {opacity: '0'},
                    '100%': {opacity: '1'},
                },
                'slide-up': {
                    '0%': {transform: 'translateY(1rem)', opacity: '0'},
                    '100%': {transform: 'translateY(0)', opacity: '1'},
                },
                'slide-down': {
                    '0%': {transform: 'translateY(-1rem)', opacity: '0'},
                    '100%': {transform: 'translateY(0)', opacity: '1'},
                },
                'scale-in': {
                    '0%': {transform: 'scale(0.95)', opacity: '0'},
                    '100%': {transform: 'scale(1)', opacity: '1'},
                },
                'bounce-gentle': {
                    '0%, 100%': {transform: 'translateY(0)'},
                    '50%': {transform: 'translateY(-4px)'},
                },
                'shake': {
                    '0%, 100%': {transform: 'translateX(0)'},
                    '25%': {transform: 'translateX(-3px)'},
                    '75%': {transform: 'translateX(3px)'},
                },
                'shimmer': {
                    '0%': {backgroundPosition: '-1000px 0'},
                    '100%': {backgroundPosition: '1000px 0'},
                },
                'spin-slow': {
                    '0%': {transform: 'rotate(0deg)'},
                    '100%': {transform: 'rotate(360deg)'},
                },
            },
            animation: {
                'fade-in': 'fade-in 200ms ease-out',
                'slide-up': 'slide-up 300ms cubic-bezier(0.4, 0, 0.2, 1)',
                'slide-down': 'slide-down 300ms cubic-bezier(0.4, 0, 0.2, 1)',
                'scale-in': 'scale-in 300ms cubic-bezier(0.4, 0, 0.2, 1)',
                'bounce-gentle': 'bounce-gentle 400ms ease-in-out',
                'shake': 'shake 300ms ease-in-out',
                'shimmer': 'shimmer 2s infinite',
                'spin-slow': 'spin-slow 2s linear infinite',
            },
        },
    },
    plugins: [
        require('@tailwindcss/forms'),
        require('@tailwindcss/typography'),
    ],
}

export default config
