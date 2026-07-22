import type {Meta, StoryObj} from '@storybook/react-vite'
import {Container} from './Container'

const content = (
    <div className="bg-primary/10 border border-primary/30 rounded-md py-6 text-center text-sm text-text-primary">
        Obsah stránky (Container ohraničuje šířku a přidává boční odsazení)
    </div>
)

const meta = {
    title: 'Components/Layout/Container',
    component: Container,
    tags: ['autodocs'],
    argTypes: {
        maxWidth: {control: 'select', options: ['sm', 'md', 'lg', 'xl', '2xl', 'full']},
        disableGutters: {control: 'boolean'},
    },
    args: {
        maxWidth: 'lg',
        children: content,
    },
} satisfies Meta<typeof Container>

export default meta
type Story = StoryObj<typeof meta>

export const Default: Story = {}

export const MaxWidths: Story = {
    render: () => (
        <div className="flex flex-col gap-4 bg-surface-base">
            {(['sm', 'md', 'lg', 'xl', '2xl'] as const).map((maxWidth) => (
                <Container key={maxWidth} maxWidth={maxWidth}>
                    <div className="bg-primary/10 border border-primary/30 rounded-md py-3 text-center text-sm text-text-primary">
                        maxWidth={maxWidth}
                    </div>
                </Container>
            ))}
        </div>
    ),
}

export const DisableGutters: Story = {
    args: {disableGutters: true},
}

export const MemberDetailPage: Story = {
    render: () => (
        <Container maxWidth="md">
            <div className="border border-border rounded-lg p-6 bg-surface-raised">
                <h2 className="text-lg font-semibold text-text-primary">Jan Novák</h2>
                <p className="text-sm text-text-secondary">ZBM1234 &middot; SK Žabovřesky</p>
            </div>
        </Container>
    ),
}
