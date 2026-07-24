import type {Meta, StoryObj} from '@storybook/react-vite'
import {useState} from 'react'
import {SelectField} from './SelectField'

const meta = {
    title: 'Components/Forms/SelectField',
    component: SelectField,
    tags: ['autodocs'],
    argTypes: {
        disabled: {control: 'boolean'},
        required: {control: 'boolean'},
        multiple: {control: 'boolean'},
    },
    args: {
        label: 'Klub',
        placeholder: 'Vyberte klub',
        options: [
            {value: 'zbm', label: 'SK Žabovřesky'},
            {value: 'lokomotiva', label: 'Lokomotiva Brno'},
            {value: 'usk', label: 'USK Praha'},
        ],
    },
} satisfies Meta<typeof SelectField>

export default meta
type Story = StoryObj<typeof meta>

export const Default: Story = {}

export const WithValue: Story = {
    render: () => {
        const [value, setValue] = useState('zbm')
        return (
            <SelectField
                label="Klub"
                value={value}
                onChange={(e) => setValue(e.target.value)}
                options={[
                    {value: 'zbm', label: 'SK Žabovřesky'},
                    {value: 'lokomotiva', label: 'Lokomotiva Brno'},
                    {value: 'usk', label: 'USK Praha'},
                ]}
            />
        )
    },
}

export const Required: Story = {
    args: {label: 'Kategorie', required: true},
}

export const WithHelpText: Story = {
    args: {
        label: 'Kategorie závodu',
        helpText: 'Kategorie určuje délku a náročnost tratě',
        options: [
            {value: 'h21', label: 'H21'},
            {value: 'd21', label: 'D21'},
            {value: 'h35', label: 'H35'},
        ],
    },
}

export const WithError: Story = {
    args: {
        label: 'Klub',
        error: 'Vyberte klub, jinak nelze pokračovat',
    },
}

export const Disabled: Story = {
    args: {label: 'Klub', value: 'zbm', disabled: true},
}

export const WithDisabledOption: Story = {
    args: {
        label: 'Kategorie',
        options: [
            {value: 'h21', label: 'H21'},
            {value: 'h35', label: 'H35 (obsazeno)', disabled: true},
            {value: 'h45', label: 'H45'},
        ],
    },
}

export const Multiple: Story = {
    render: () => {
        const [value, setValue] = useState(['zbm'])
        return (
            <SelectField
                label="Kluby k porovnání"
                multiple
                value={value}
                onChange={(e) =>
                    setValue(Array.from(e.target.selectedOptions, (o) => o.value))
                }
                options={[
                    {value: 'zbm', label: 'SK Žabovřesky'},
                    {value: 'lokomotiva', label: 'Lokomotiva Brno'},
                    {value: 'usk', label: 'USK Praha'},
                ]}
            />
        )
    },
}
