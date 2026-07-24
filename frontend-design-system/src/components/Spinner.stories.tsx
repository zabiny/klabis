import type {Meta, StoryObj} from '@storybook/react-vite'
import {Spinner} from './Spinner'

const meta = {
    title: 'Components/Spinner',
    component: Spinner,
    tags: ['autodocs'],
    argTypes: {
        size: {
            control: 'select',
            options: ['sm', 'md', 'lg'],
        },
    },
    args: {
        size: 'md',
    },
} satisfies Meta<typeof Spinner>

export default meta
type Story = StoryObj<typeof meta>

export const Default: Story = {
    render: (args) => (
        <div className="text-primary">
            <Spinner {...args} />
        </div>
    ),
}

export const Small: Story = {
    args: {size: 'sm'},
    render: (args) => (
        <div className="text-primary">
            <Spinner {...args} />
        </div>
    ),
}

export const Large: Story = {
    args: {size: 'lg'},
    render: (args) => (
        <div className="text-primary">
            <Spinner {...args} />
        </div>
    ),
}

export const AllSizes: Story = {
    render: () => (
        <div className="flex items-center gap-4 text-primary">
            <Spinner size="sm"/>
            <Spinner size="md"/>
            <Spinner size="lg"/>
        </div>
    ),
}

export const LoadingMemberList: Story = {
    render: () => (
        <div className="flex flex-col items-center gap-3 text-primary py-8">
            <Spinner size="lg"/>
            <p className="text-sm text-text-secondary">Načítám seznam členů…</p>
        </div>
    ),
}
