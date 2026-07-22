import type {Meta, StoryObj} from '@storybook/react-vite'
import {useState} from 'react'
import {CheckboxField} from './CheckboxField'

const meta = {
    title: 'Components/Forms/CheckboxField',
    component: CheckboxField,
    tags: ['autodocs'],
    argTypes: {
        disabled: {control: 'boolean'},
        required: {control: 'boolean'},
        checked: {control: 'boolean'},
    },
    args: {
        label: 'Souhlasím se zpracováním osobních údajů',
        checked: false,
    },
} satisfies Meta<typeof CheckboxField>

export default meta
type Story = StoryObj<typeof meta>

export const Default: Story = {}

export const Controlled: Story = {
    render: () => {
        const [checked, setChecked] = useState(false)
        return (
            <CheckboxField
                label="Souhlasím se zpracováním osobních údajů"
                checked={checked}
                onChange={setChecked}
            />
        )
    },
}

export const Checked: Story = {
    args: {checked: true},
}

export const Required: Story = {
    args: {label: 'Souhlasím s podmínkami členství', required: true},
}

export const WithHelpText: Story = {
    args: {
        label: 'Zasílat novinky e-mailem',
        helpText: 'Nejvýše jednou týdně, odhlásit se lze kdykoliv',
    },
}

export const WithError: Story = {
    args: {
        label: 'Souhlasím s podmínkami',
        checked: false,
        error: 'Bez souhlasu nelze registraci dokončit',
    },
}

export const Disabled: Story = {
    args: {label: 'Aktivní členství', checked: true, disabled: true},
}
