import type {Meta, StoryObj} from '@storybook/react-vite'
import {useState} from 'react'
import {TextAreaField} from './TextField'

const meta = {
    title: 'Components/Forms/TextAreaField',
    component: TextAreaField,
    tags: ['autodocs'],
    argTypes: {
        disabled: {control: 'boolean'},
        required: {control: 'boolean'},
        rows: {control: 'number'},
    },
    args: {
        label: 'Poznámka',
        placeholder: 'Doplňující informace k členovi…',
    },
} satisfies Meta<typeof TextAreaField>

export default meta
type Story = StoryObj<typeof meta>

export const Default: Story = {}

export const WithValue: Story = {
    render: () => {
        const [value, setValue] = useState('Člen má zdravotní omezení, nesmí startovat na dlouhých tratích.')
        return (
            <TextAreaField
                label="Poznámka"
                value={value}
                onChange={(e) => setValue(e.target.value)}
            />
        )
    },
}

export const Required: Story = {
    args: {label: 'Důvod zrušení registrace', required: true},
}

export const WithHelpText: Story = {
    args: {
        label: 'Popis akce',
        helpText: 'Zobrazí se v detailu akce a v kalendáři',
        rows: 5,
    },
}

export const WithError: Story = {
    args: {
        label: 'Popis akce',
        value: '',
        error: 'Popis je povinný',
    },
}

export const Disabled: Story = {
    args: {label: 'Poznámka', value: 'Archivováno, nelze upravovat.', disabled: true},
}
