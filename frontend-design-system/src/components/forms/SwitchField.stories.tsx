import type {Meta, StoryObj} from '@storybook/react-vite'
import {useState} from 'react'
import {SwitchField} from './SwitchField'

const meta = {
    title: 'Components/Forms/SwitchField',
    component: SwitchField,
    tags: ['autodocs'],
    argTypes: {
        disabled: {control: 'boolean'},
        required: {control: 'boolean'},
        checked: {control: 'boolean'},
    },
    args: {
        name: 'active',
        label: 'Aktivní členství',
        checked: true,
    },
} satisfies Meta<typeof SwitchField>

export default meta
type Story = StoryObj<typeof meta>

export const Default: Story = {}

export const Controlled: Story = {
    render: () => {
        const [checked, setChecked] = useState(true)
        return (
            <SwitchField
                name="active"
                label="Aktivní členství"
                checked={checked}
                onChange={setChecked}
            />
        )
    },
}

export const Off: Story = {
    args: {checked: false},
}

export const WithHelpText: Story = {
    args: {
        name: 'notifications',
        label: 'E-mailová upozornění',
        checked: true,
        helpText: 'Upozornění na blížící se termín platby členského příspěvku',
    },
}

export const WithError: Story = {
    args: {
        name: 'terms',
        label: 'Souhlas s podmínkami',
        checked: false,
        error: 'Bez souhlasu nelze pokračovat',
    },
}

export const Disabled: Story = {
    args: {checked: true, disabled: true},
}
