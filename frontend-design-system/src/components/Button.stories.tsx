import type {Meta, StoryObj} from '@storybook/react-vite'
import {Button} from './Button'
import {EditIcon} from './icons/EditIcon'
import {DeleteIcon} from './icons/DeleteIcon'

const meta = {
    title: 'Components/Button',
    component: Button,
    tags: ['autodocs'],
    argTypes: {
        variant: {
            control: 'select',
            options: ['primary', 'secondary', 'danger', 'warning', 'ghost', 'danger-ghost', 'primary-ghost', 'warning-ghost'],
        },
        size: {
            control: 'select',
            options: ['sm', 'md', 'lg'],
        },
        fullWidth: {control: 'boolean'},
        loading: {control: 'boolean'},
        disabled: {control: 'boolean'},
        children: {control: 'text'},
    },
    args: {
        variant: 'primary',
        size: 'md',
        fullWidth: false,
        loading: false,
        disabled: false,
        children: 'Uložit',
    },
} satisfies Meta<typeof Button>

export default meta
type Story = StoryObj<typeof meta>

export const Default: Story = {}

export const Primary: Story = {
    args: {variant: 'primary', children: 'Uložit člena'},
}

export const Secondary: Story = {
    args: {variant: 'secondary', children: 'Zrušit'},
}

export const Danger: Story = {
    args: {variant: 'danger', children: 'Smazat člena'},
}

export const Warning: Story = {
    args: {variant: 'warning', children: 'Pozastavit členství'},
}

export const Ghost: Story = {
    args: {variant: 'ghost', children: 'Zpět na seznam'},
}

export const DangerGhost: Story = {
    args: {variant: 'danger-ghost', children: 'Odebrat oprávnění'},
}

export const PrimaryGhost: Story = {
    args: {variant: 'primary-ghost', children: 'Zobrazit detail'},
}

export const WarningGhost: Story = {
    args: {variant: 'warning-ghost', children: 'Vyžaduje pozornost'},
}

export const Sizes: Story = {
    render: () => (
        <div className="flex items-center gap-3">
            <Button size="sm">Malé</Button>
            <Button size="md">Střední</Button>
            <Button size="lg">Velké</Button>
        </div>
    ),
}

export const WithIcons: Story = {
    render: () => (
        <div className="flex items-center gap-3">
            <Button startIcon={<EditIcon className="w-4 h-4"/>}>Upravit</Button>
            <Button variant="danger" endIcon={<DeleteIcon className="w-4 h-4"/>}>Smazat</Button>
        </div>
    ),
}

export const Loading: Story = {
    args: {loading: true, children: 'Ukládám…'},
}

export const Disabled: Story = {
    args: {disabled: true, children: 'Nedostupné'},
}

export const FullWidth: Story = {
    args: {fullWidth: true, children: 'Odeslat přihlášku'},
    render: (args) => (
        <div className="w-96">
            <Button {...args} />
        </div>
    ),
}

export const AllVariants: Story = {
    render: () => (
        <div className="flex flex-wrap gap-3">
            <Button variant="primary">Primary</Button>
            <Button variant="secondary">Secondary</Button>
            <Button variant="danger">Danger</Button>
            <Button variant="warning">Warning</Button>
            <Button variant="ghost">Ghost</Button>
            <Button variant="danger-ghost">Danger ghost</Button>
            <Button variant="primary-ghost">Primary ghost</Button>
            <Button variant="warning-ghost">Warning ghost</Button>
        </div>
    ),
}
