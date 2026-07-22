import type {Meta, StoryObj} from '@storybook/react-vite'
import {Tooltip} from './Tooltip'
import {Button} from './Button'
import {EditIcon} from './icons/EditIcon'

const meta = {
    title: 'Components/Tooltip',
    component: Tooltip,
    tags: ['autodocs'],
    argTypes: {
        content: {control: 'text'},
    },
    args: {
        content: 'Upravit údaje člena',
        children: <Button size="sm" startIcon={<EditIcon className="w-4 h-4"/>}>Upravit</Button>,
    },
} satisfies Meta<typeof Tooltip>

export default meta
type Story = StoryObj<typeof meta>

export const Default: Story = {
    render: (args) => (
        <div className="p-12">
            <Tooltip {...args}>
                <Button size="sm" startIcon={<EditIcon className="w-4 h-4"/>}>Upravit</Button>
            </Tooltip>
        </div>
    ),
}

export const OnIconButton: Story = {
    render: () => (
        <div className="p-12">
            <Tooltip content="Upravit záznam">
                <button
                    className="p-2 rounded-md border border-border hover:bg-surface-raised text-text-primary"
                    aria-label="Upravit"
                >
                    <EditIcon className="w-5 h-5"/>
                </button>
            </Tooltip>
        </div>
    ),
}

export const MultilineContent: Story = {
    render: () => (
        <div className="p-12">
            <Tooltip content={'Registrační číslo: ZBM4821\nOddíl: Sokol Praha\nStav: Aktivní'}>
                <span className="underline decoration-dotted cursor-help text-text-primary">Jan Novák</span>
            </Tooltip>
        </div>
    ),
}

export const OnDisabledLikeText: Story = {
    render: () => (
        <div className="p-12">
            <Tooltip content="Tato akce je dostupná pouze administrátorům">
                <span className="text-text-tertiary cursor-not-allowed">Smazat člena</span>
            </Tooltip>
        </div>
    ),
}
