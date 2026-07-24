import type {Meta, StoryObj} from '@storybook/react-vite'
import {useState} from 'react'
import {TextField} from './TextField'

const meta = {
    title: 'Components/Forms/TextField',
    component: TextField,
    tags: ['autodocs'],
    argTypes: {
        type: {
            control: 'select',
            options: ['text', 'email', 'password', 'number', 'date', 'datetime-local', 'url', 'tel'],
        },
        disabled: {control: 'boolean'},
        required: {control: 'boolean'},
    },
    args: {
        label: 'Jméno',
        placeholder: 'Jan Novák',
    },
} satisfies Meta<typeof TextField>

export default meta
type Story = StoryObj<typeof meta>

export const Default: Story = {}

export const WithValue: Story = {
    render: () => {
        const [value, setValue] = useState('Jan Novák')
        return (
            <TextField
                label="Jméno"
                value={value}
                onChange={(e) => setValue(e.target.value)}
            />
        )
    },
}

export const Required: Story = {
    args: {label: 'Registrační číslo', required: true, placeholder: 'ZBM1234'},
}

export const WithHelpText: Story = {
    args: {
        label: 'E-mail',
        type: 'email',
        placeholder: 'jan.novak@klabis.cz',
        helpText: 'Použije se pro zasílání pozvánek na závody',
    },
}

export const WithError: Story = {
    args: {
        label: 'E-mail',
        type: 'email',
        value: 'neplatny-email',
        error: 'Zadejte platnou e-mailovou adresu',
    },
}

export const Disabled: Story = {
    args: {label: 'Registrační číslo', value: 'ZBM1234', disabled: true},
}

export const Types: Story = {
    render: () => (
        <div className="flex flex-col gap-4 w-80">
            <TextField label="Text" type="text" placeholder="Jan Novák"/>
            <TextField label="E-mail" type="email" placeholder="jan.novak@klabis.cz"/>
            <TextField label="Heslo" type="password" placeholder="••••••••"/>
            <TextField label="Číslo" type="number" placeholder="21"/>
            <TextField label="Datum narození" type="date"/>
        </div>
    ),
}
