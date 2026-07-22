import type {Meta, StoryObj} from '@storybook/react-vite'
import {useState} from 'react'
import {CheckboxGroup} from './CheckboxField'

const disciplineOptions = [
    {value: 'sprint', label: 'Sprint'},
    {value: 'klasika', label: 'Klasická trať'},
    {value: 'stafeta', label: 'Štafeta'},
    {value: 'nocni', label: 'Noční orientační běh'},
]

const meta = {
    title: 'Components/Forms/CheckboxGroup',
    component: CheckboxGroup,
    tags: ['autodocs'],
    argTypes: {
        disabled: {control: 'boolean'},
        required: {control: 'boolean'},
        direction: {control: 'select', options: ['horizontal', 'vertical']},
    },
    args: {
        name: 'disciplines',
        label: 'Preferované disciplíny',
        options: disciplineOptions,
        value: ['sprint'],
    },
} satisfies Meta<typeof CheckboxGroup>

export default meta
type Story = StoryObj<typeof meta>

export const Default: Story = {}

export const Controlled: Story = {
    render: () => {
        const [value, setValue] = useState<(string | number)[]>(['sprint', 'stafeta'])
        return (
            <CheckboxGroup
                name="disciplines"
                label="Preferované disciplíny"
                options={disciplineOptions}
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
    args: {required: true, value: []},
}

export const WithHelpText: Story = {
    args: {
        helpText: 'Vyberte alespoň jednu disciplínu pro doporučení tréninkových skupin',
    },
}

export const WithError: Story = {
    args: {
        value: [],
        error: 'Vyberte alespoň jednu disciplínu',
    },
}

export const WithDisabledOption: Story = {
    args: {
        options: [
            {value: 'sprint', label: 'Sprint'},
            {value: 'klasika', label: 'Klasická trať'},
            {value: 'nocni', label: 'Noční orientační běh (mimo sezónu)', disabled: true},
        ],
    },
}

export const Disabled: Story = {
    args: {disabled: true, value: ['sprint', 'klasika']},
}
