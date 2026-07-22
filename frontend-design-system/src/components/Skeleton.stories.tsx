import type {Meta, StoryObj} from '@storybook/react-vite'
import {Skeleton} from './Skeleton'
import {Card} from './Card'

const meta = {
    title: 'Components/Skeleton',
    component: Skeleton,
    tags: ['autodocs'],
    argTypes: {
        width: {control: 'text'},
        height: {control: 'text'},
        count: {control: 'number'},
        circle: {control: 'boolean'},
    },
    args: {
        width: '100%',
        height: '1rem',
        count: 1,
        circle: false,
    },
} satisfies Meta<typeof Skeleton>

export default meta
type Story = StoryObj<typeof meta>

export const Default: Story = {
    render: (args) => (
        <div className="w-80">
            <Skeleton {...args} />
        </div>
    ),
}

export const MultipleLinesCount: Story = {
    args: {count: 4},
    render: (args) => (
        <div className="w-80">
            <Skeleton {...args} />
        </div>
    ),
}

export const Circle: Story = {
    args: {circle: true, width: 48, height: 48},
}

export const MemberListLoadingState: Story = {
    render: () => (
        <div className="flex flex-col gap-3 w-96">
            {Array.from({length: 3}).map((_, i) => (
                <Card key={i} className="p-4 flex items-center gap-3">
                    <Skeleton circle width={40} height={40}/>
                    <div className="flex-1">
                        <Skeleton count={2} height="0.875rem"/>
                    </div>
                </Card>
            ))}
        </div>
    ),
}

export const CustomChildren: Story = {
    render: () => (
        <Skeleton className="w-96">
            <div className="h-6 w-1/2 bg-surface-raised animate-pulse rounded"/>
            <div className="h-4 w-full bg-surface-raised animate-pulse rounded"/>
            <div className="h-4 w-2/3 bg-surface-raised animate-pulse rounded"/>
        </Skeleton>
    ),
}
