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
                // Neutral base colors (using CSS variables for theme support)
                black: 'var(--color-black)',
                dark: 'var(--color-dark)',
                surface: {
                    DEFAULT: 'var(--color-surface-raised)',
                    base: 'var(--color-surface-base)',
                    raised: 'var(--color-surface-raised)',
                },
                // Primary action color (blue)
                primary: {
                    DEFAULT: 'var(--color-primary)',
                    light: 'var(--color-primary-light)',
                    dark: 'var(--color-primary-dark)',
                },
                // Secondary action color
                secondary: {
                    DEFAULT: 'var(--color-secondary)',
                    light: 'var(--color-secondary-light)',
                    dark: 'var(--color-secondary-dark)',
                },
                // Semantic colors
                feedback: {
                    success: 'var(--color-feedback-success)',
                    warning: 'var(--color-feedback-warning)',
                    error: 'var(--color-feedback-error)',
                    info: 'var(--color-feedback-info)',
                },
                // Focus/highlight accent
                accent: {
                    DEFAULT: 'var(--color-accent)',
                    light: 'var(--color-accent-light)',
                    dark: 'var(--color-accent-dark)',
                },
                // Text colors
                text: {
                    primary: 'var(--color-text-primary)',
                    secondary: 'var(--color-text-secondary)',
                    tertiary: 'var(--color-text-tertiary)',
                },
                // Border colors
                border: {
                    DEFAULT: 'var(--color-border)',
                    light: 'var(--color-border-light)',
                    dark: 'var(--color-border-dark)',
                },
                // Alert backgrounds (theme-aware via CSS variables)
                alert: {
                    success: 'var(--color-alert-success)',
                    warning: 'var(--color-alert-warning)',
                    error: 'var(--color-alert-error)',
                    info: 'var(--color-alert-info)',
                },
                // Alert text colors
                'alert-text': {
                    success: 'var(--color-alert-text-success)',
                    warning: 'var(--color-alert-text-warning)',
                    error: 'var(--color-alert-text-error)',
                    info: 'var(--color-alert-text-info)',
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
