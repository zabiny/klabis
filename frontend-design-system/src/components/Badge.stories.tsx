import type {Meta, StoryObj} from '@storybook/react-vite'
import {Badge} from './Badge'

const meta = {
    title: 'Components/Badge',
    component: Badge,
    tags: ['autodocs'],
    argTypes: {
        variant: {
            control: 'select',
            options: ['default', 'primary', 'success', 'warning', 'error', 'info', 'orange', 'blue'],
        },
        size: {
            control: 'select',
            options: ['sm', 'md', 'lg'],
        },
        children: {control: 'text'},
    },
    args: {
        variant: 'default',
        size: 'md',
        children: 'Aktivní',
    },
} satisfies Meta<typeof Badge>

export default meta
type Story = StoryObj<typeof meta>

export const Default: Story = {}

export const Primary: Story = {
    args: {variant: 'primary', children: 'Trenér'},
}

export const Success: Story = {
    args: {variant: 'success', children: 'Zaplaceno'},
}

export const Warning: Story = {
    args: {variant: 'warning', children: 'Čeká na schválení'},
}

export const ErrorVariant: Story = {
    name: 'Error',
    args: {variant: 'error', children: 'Po splatnosti'},
}

export const Info: Story = {
    args: {variant: 'info', children: 'Nový člen'},
}

export const Orange: Story = {
    args: {variant: 'orange', children: 'B kategorie'},
}

export const Blue: Story = {
    args: {variant: 'blue', children: 'Přípravka'},
}

export const Sizes: Story = {
    render: () => (
        <div className="flex items-center gap-3">
            <Badge size="sm">Malý</Badge>
            <Badge size="md">Střední</Badge>
            <Badge size="lg">Velký</Badge>
        </div>
    ),
}

export const AllVariants: Story = {
    render: () => (
        <div className="flex flex-wrap gap-3">
            <Badge variant="default">Výchozí</Badge>
            <Badge variant="primary">Trenér</Badge>
            <Badge variant="success">Zaplaceno</Badge>
            <Badge variant="warning">Čeká</Badge>
            <Badge variant="error">Po splatnosti</Badge>
            <Badge variant="info">Nový</Badge>
            <Badge variant="orange">B kategorie</Badge>
            <Badge variant="blue">Přípravka</Badge>
        </div>
    ),
}
