import type {Preview} from '@storybook/react-vite'
import {withThemeByClassName} from '@storybook/addon-themes'

import './preview.css'

const preview: Preview = {
    parameters: {
        controls: {
            matchers: {
                color: /(background|color)$/i,
                date: /Date$/i,
            },
        },
        backgrounds: {
            disable: true,
        },
    },
    decorators: [
        withThemeByClassName({
            themes: {
                light: '',
                dark: 'dark',
            },
            defaultTheme: 'light',
            parentSelector: 'html',
        }),
        (Story) => (
            <div className="bg-bg-base text-text-primary min-h-screen p-6">
                <Story/>
            </div>
        ),
    ],
}

export default preview
