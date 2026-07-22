import type {Meta, StoryObj} from '@storybook/react-vite'
import {useState} from 'react'
import {PillGroup} from './PillGroup'

const meta = {
    title: 'Components/PillGroup',
    component: PillGroup,
    tags: ['autodocs'],
} satisfies Meta<typeof PillGroup<string>>

export default meta
type Story = StoryObj<typeof meta>

const categoryOptions = [
    {value: 'all', label: 'Vše'},
    {value: 'active', label: 'Aktivní'},
    {value: 'inactive', label: 'Neaktivní'},
]

export const Default: Story = {
    args: {
        options: categoryOptions,
        selectedValue: 'all',
        onChange: () => {},
        ariaLabel: 'Filtr stavu členství',
    },
    render: () => {
        const [value, setValue] = useState('all')
        return (
            <PillGroup
                options={categoryOptions}
                selectedValue={value}
                onChange={setValue}
                ariaLabel="Filtr stavu členství"
            />
        )
    },
}

export const RaceCategories: Story = {
    args: {
        options: [
            {value: 'h21', label: 'H21'},
            {value: 'd21', label: 'D21'},
            {value: 'h35', label: 'H35'},
            {value: 'd35', label: 'D35'},
        ],
        selectedValue: 'h21',
        onChange: () => {},
        ariaLabel: 'Výběr kategorie závodu',
    },
    render: () => {
        const [value, setValue] = useState('h21')
        const options = [
            {value: 'h21', label: 'H21'},
            {value: 'd21', label: 'D21'},
            {value: 'h35', label: 'H35'},
            {value: 'd35', label: 'D35'},
        ]
        return (
            <PillGroup
                options={options}
                selectedValue={value}
                onChange={setValue}
                ariaLabel="Výběr kategorie závodu"
            />
        )
    },
}

export const WithDisabledOption: Story = {
    args: {
        options: categoryOptions,
        selectedValue: 'all',
        onChange: () => {},
        ariaLabel: 'Filtr stavu členství',
        disabledValues: ['inactive'],
        disabledTooltip: 'Tento filtr je momentálně nedostupný',
    },
    render: () => {
        const [value, setValue] = useState('all')
        return (
            <PillGroup
                options={categoryOptions}
                selectedValue={value}
                onChange={setValue}
                ariaLabel="Filtr stavu členství"
                disabledValues={['inactive']}
                disabledTooltip="Tento filtr je momentálně nedostupný"
            />
        )
    },
}
