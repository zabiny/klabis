import type {Meta, StoryObj} from '@storybook/react-vite'
import {useState} from 'react'
import {RadioGroup} from './RadioGroup'

const categoryOptions = [
    {value: 'h21', label: 'H21'},
    {value: 'd21', label: 'D21'},
    {value: 'h35', label: 'H35'},
    {value: 'd35', label: 'D35'},
]

const meta = {
    title: 'Components/Forms/RadioGroup',
    component: RadioGroup,
    tags: ['autodocs'],
    argTypes: {
        disabled: {control: 'boolean'},
        required: {control: 'boolean'},
        direction: {control: 'select', options: ['horizontal', 'vertical']},
    },
    args: {
        name: 'category',
        label: 'Kategorie',
        options: categoryOptions,
        value: 'h21',
    },
} satisfies Meta<typeof RadioGroup>

export default meta
type Story = StoryObj<typeof meta>

export const Default: Story = {}

export const Controlled: Story = {
    render: () => {
        const [value, setValue] = useState<string | number>('h21')
        return (
            <RadioGroup
                name="category"
                label="Kategorie"
                options={categoryOptions}
                value={value}
                onChange={setValue}
            />
        )
    },
}

export const Horizontal: Story = {
    args: {direction: 'horizontal'},
}

export const Required: Story = {
    args: {required: true, value: undefined},
}

export const WithHelpText: Story = {
    args: {
        helpText: 'Kategorie určuje délku a náročnost tratě',
    },
}

export const WithError: Story = {
    args: {
        value: undefined,
        error: 'Vyberte kategorii',
    },
}

export const WithDisabledOption: Story = {
    args: {
        options: [
            {value: 'h21', label: 'H21'},
            {value: 'h35', label: 'H35 (obsazeno)', disabled: true},
            {value: 'h45', label: 'H45'},
        ],
    },
}

export const Disabled: Story = {
    args: {disabled: true},
}
