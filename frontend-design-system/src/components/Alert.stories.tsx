import type {Meta, StoryObj} from '@storybook/react-vite'
import {useState} from 'react'
import {Alert} from './Alert'

const meta = {
    title: 'Components/Alert',
    component: Alert,
    tags: ['autodocs'],
    argTypes: {
        severity: {
            control: 'select',
            options: ['success', 'error', 'warning', 'info'],
        },
        children: {control: 'text'},
    },
    args: {
        severity: 'info',
        children: 'Vaše žádost o členství byla úspěšně odeslána.',
    },
} satisfies Meta<typeof Alert>

export default meta
type Story = StoryObj<typeof meta>

export const Default: Story = {}

export const Success: Story = {
    args: {
        severity: 'success',
        children: 'Platba členského příspěvku byla přijata.',
    },
}

export const Error: Story = {
    args: {
        severity: 'error',
        children: 'Registrační číslo ZBM1234 nebylo nalezeno.',
    },
}

export const Warning: Story = {
    args: {
        severity: 'warning',
        children: 'Platnost lékařské prohlídky vyprší za 14 dní.',
    },
}

export const Info: Story = {
    args: {
        severity: 'info',
        children: 'Uzávěrka přihlášek na závod je 30. 6. 2026.',
    },
}

export const WithCloseButton: Story = {
    render: (args) => {
        const [visible, setVisible] = useState(true)
        if (!visible) {
            return <button onClick={() => setVisible(true)}>Zobrazit znovu</button>
        }
        return (
            <Alert {...args} onClose={() => setVisible(false)}/>
        )
    },
    args: {
        severity: 'warning',
        children: 'Oddílový příspěvek za rok 2026 dosud nebyl uhrazen.',
    },
}
