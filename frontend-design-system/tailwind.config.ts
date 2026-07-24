import type {Config} from 'tailwindcss'
import preset from './tailwind-preset'

const config: Config = {
    presets: [preset],
    content: [
        './src/**/*.{js,ts,jsx,tsx}',
        './.storybook/**/*.{js,ts,jsx,tsx}',
    ],
}

export default config
