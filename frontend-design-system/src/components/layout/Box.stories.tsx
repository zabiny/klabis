import type {Meta, StoryObj} from '@storybook/react-vite'
import {Box} from './Box'

const chip = (label: string) => (
    <div className="bg-primary/10 border border-primary/30 rounded-md px-4 py-3 text-sm text-text-primary">
        {label}
    </div>
)

const meta = {
    title: 'Components/Layout/Box',
    component: Box,
    tags: ['autodocs'],
    args: {
        children: null,
    },
} satisfies Meta<typeof Box>

export default meta
type Story = StoryObj<typeof meta>

export const Default: Story = {
    render: () => <Box p={4} border rounded="md">{chip('Box s paddingem a rámečkem')}</Box>,
}

export const FlexRow: Story = {
    render: () => (
        <Box display="flex" flex="row" gap={3} alignItems="center">
            {chip('Sprint')}
            {chip('Klasika')}
            {chip('Štafeta')}
        </Box>
    ),
}

export const FlexColumn: Story = {
    render: () => (
        <Box display="flex" flex="col" gap={2} width="80">
            {chip('Jan Novák')}
            {chip('Petra Dvořáková')}
        </Box>
    ),
}

export const JustifyBetween: Story = {
    render: () => (
        <Box display="flex" flex="row" justifyContent="between" alignItems="center" p={3} border rounded="md" width="96">
            <span className="font-medium text-text-primary">Detail člena</span>
            <span className="text-sm text-text-secondary">ZBM1234</span>
        </Box>
    ),
}

export const CardLike: Story = {
    render: () => (
        <Box p={4} border rounded="lg" shadow="md" bgcolor="bg-surface-raised" width="80">
            <p className="font-semibold text-text-primary">SK Žabovřesky</p>
            <p className="text-sm text-text-secondary">Oddíl orientačního běhu</p>
        </Box>
    ),
}
