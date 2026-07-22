import type {Meta, StoryObj} from '@storybook/react-vite'
import {Card} from './Card'
import {Badge} from './Badge'
import {Button} from './Button'

const meta = {
    title: 'Components/Card',
    component: Card,
    tags: ['autodocs'],
    argTypes: {
        shadow: {
            control: 'select',
            options: ['none', 'sm', 'md', 'lg'],
        },
        hoverable: {control: 'boolean'},
    },
    args: {
        shadow: 'sm',
        hoverable: false,
        children: 'Obsah karty',
    },
} satisfies Meta<typeof Card>

export default meta
type Story = StoryObj<typeof meta>

export const Default: Story = {
    render: (args) => (
        <Card {...args} className="p-6 w-96">
            <h3 className="text-lg font-semibold text-text-primary mb-2">Jan Novák</h3>
            <p className="text-sm text-text-secondary">Registrační číslo ZBM4821 · Oddíl orientačního běhu Sokol Praha</p>
        </Card>
    ),
}

export const Hoverable: Story = {
    args: {hoverable: true, children: 'Obsah karty'},
    render: (args) => (
        <Card {...args} className="p-6 w-96">
            <h3 className="text-lg font-semibold text-text-primary mb-2">Klubový závod – Jarní pohár</h3>
            <p className="text-sm text-text-secondary">Sobota 12. dubna 2026 · Krkonoše</p>
        </Card>
    ),
}

export const ShadowVariants: Story = {
    args: {children: 'Obsah karty'},
    render: () => (
        <div className="flex flex-wrap gap-6">
            {(['none', 'sm', 'md', 'lg'] as const).map((shadow) => (
                <Card key={shadow} shadow={shadow} className="p-4 w-48">
                    <p className="text-sm text-text-primary">shadow = {shadow}</p>
                </Card>
            ))}
        </div>
    ),
}

export const WithHeaderAndActions: Story = {
    args: {children: 'Obsah karty'},
    render: (args) => (
        <Card {...args} className="w-96">
            <div className="flex items-center justify-between px-6 py-4 border-b border-border">
                <h3 className="text-lg font-semibold text-text-primary">Platba členského příspěvku</h3>
                <Badge variant="success">Zaplaceno</Badge>
            </div>
            <div className="px-6 py-4 text-sm text-text-secondary">
                Příspěvek za rok 2026 ve výši 1 200 Kč byl uhrazen 3. 1. 2026.
            </div>
            <div className="flex justify-end gap-3 px-6 py-4 border-t border-border">
                <Button variant="secondary" size="sm">Zobrazit doklad</Button>
                <Button variant="primary" size="sm">Odeslat potvrzení</Button>
            </div>
        </Card>
    ),
}
