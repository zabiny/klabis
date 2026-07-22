import type {Meta, StoryObj} from '@storybook/react-vite'
import {Grid} from './Grid'

const cell = (label: string) => (
    <div className="bg-primary/10 border border-primary/30 rounded-md px-3 py-6 text-center text-sm text-text-primary">
        {label}
    </div>
)

const meta = {
    title: 'Components/Layout/Grid',
    component: Grid,
    tags: ['autodocs'],
    args: {
        children: null,
    },
} satisfies Meta<typeof Grid>

export default meta
type Story = StoryObj<typeof meta>

export const TwelveColumns: Story = {
    render: () => (
        <Grid container spacing={2}>
            <Grid item xs={12}>{cell('xs=12')}</Grid>
            <Grid item xs={6}>{cell('xs=6')}</Grid>
            <Grid item xs={6}>{cell('xs=6')}</Grid>
            <Grid item xs={4}>{cell('xs=4')}</Grid>
            <Grid item xs={4}>{cell('xs=4')}</Grid>
            <Grid item xs={4}>{cell('xs=4')}</Grid>
        </Grid>
    ),
}

export const ResponsiveColumns: Story = {
    render: () => (
        <Grid container spacing={3}>
            <Grid item xs={12} md={4}>{cell('xs=12 md=4')}</Grid>
            <Grid item xs={12} md={4}>{cell('xs=12 md=4')}</Grid>
            <Grid item xs={12} md={4}>{cell('xs=12 md=4')}</Grid>
        </Grid>
    ),
}

export const MemberCards: Story = {
    render: () => (
        <Grid container spacing={4}>
            {['Jan Novák (ZBM1234)', 'Petra Dvořáková (ZBM1235)', 'Tomáš Král (ZBM1236)', 'Eva Malá (ZBM1237)'].map((name) => (
                <Grid item xs={12} sm={6} lg={3} key={name}>
                    <div className="border border-border rounded-lg p-4 bg-surface-raised">
                        <p className="font-medium text-text-primary">{name}</p>
                        <p className="text-sm text-text-secondary">SK Žabovřesky</p>
                    </div>
                </Grid>
            ))}
        </Grid>
    ),
}

export const Spacing: Story = {
    render: () => (
        <div className="flex flex-col gap-6">
            {([1, 4, 8] as const).map((spacing) => (
                <div key={spacing}>
                    <p className="text-sm text-text-secondary mb-2">spacing={spacing}</p>
                    <Grid container spacing={spacing}>
                        <Grid item xs={4}>{cell('A')}</Grid>
                        <Grid item xs={4}>{cell('B')}</Grid>
                        <Grid item xs={4}>{cell('C')}</Grid>
                    </Grid>
                </div>
            ))}
        </div>
    ),
}
