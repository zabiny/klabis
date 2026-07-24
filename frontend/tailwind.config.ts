import type {Config} from 'tailwindcss'
import designSystemPreset from '@klabis/design-system/tailwind-preset'

const config: Config = {
    presets: [designSystemPreset],
    content: [
        "./index.html",
        "./src/**/*.{js,ts,jsx,tsx}",
        "../frontend-design-system/src/**/*.{js,ts,jsx,tsx}",
    ],
    darkMode: 'class',
}

export default config
