import type {Meta, StoryObj} from '@storybook/react-vite'
import {useState} from 'react'
import {FulltextSearchInput} from './FulltextSearchInput'

const meta = {
    title: 'Components/FulltextSearchInput',
    component: FulltextSearchInput,
    tags: ['autodocs'],
    argTypes: {
        placeholder: {control: 'text'},
        ariaLabel: {control: 'text'},
        minChars: {control: 'number'},
        debounceMs: {control: 'number'},
    },
    args: {
        value: '',
        onChange: () => {},
        placeholder: 'Hledat člena podle jména…',
        ariaLabel: 'Vyhledávání členů',
        minChars: 2,
        debounceMs: 250,
    },
} satisfies Meta<typeof FulltextSearchInput>

export default meta
type Story = StoryObj<typeof meta>

const InteractiveTemplate = (args: React.ComponentProps<typeof FulltextSearchInput>) => {
    const [value, setValue] = useState('')
    return (
        <div className="w-96 flex flex-col gap-2">
            <FulltextSearchInput {...args} value={value} onChange={setValue}/>
            <p className="text-sm text-text-secondary">
                Odeslaná hodnota (po debounci): <span className="font-mono">{value || '—'}</span>
            </p>
        </div>
    )
}

export const Default: Story = {
    render: (args) => <InteractiveTemplate {...args} />,
}

export const Prefilled: Story = {
    render: (args) => <InteractiveTemplate {...args} />,
    args: {
        placeholder: 'Hledat závod…',
        ariaLabel: 'Vyhledávání závodů',
    },
}

export const ShortMinChars: Story = {
    render: (args) => <InteractiveTemplate {...args} />,
    args: {
        minChars: 1,
        placeholder: 'Hledat oddíl (od 1 znaku)…',
    },
}
