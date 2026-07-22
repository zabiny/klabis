import type {StorybookConfig} from '@storybook/react-vite'

import {dirname} from 'path'
import {fileURLToPath} from 'url'

function getAbsolutePath(value: string) {
    return dirname(fileURLToPath(import.meta.resolve(`${value}/package.json`)))
}

const config: StorybookConfig = {
    stories: [
        '../src/**/*.mdx',
        '../src/**/*.stories.@(js|jsx|mjs|ts|tsx)',
    ],
    addons: [
        getAbsolutePath('@storybook/addon-docs'),
        getAbsolutePath('@storybook/addon-themes'),
    ],
    framework: getAbsolutePath('@storybook/react-vite'),
}

export default config
