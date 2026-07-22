import type {Meta, StoryObj} from '@storybook/react-vite'
import {DetailRow} from './DetailRow'
import {Badge} from './Badge'

const meta = {
    title: 'Components/DetailRow',
    component: DetailRow,
    tags: ['autodocs'],
    argTypes: {
        label: {control: 'text'},
    },
    args: {
        label: 'Registrační číslo',
        children: 'ZBM4821',
    },
} satisfies Meta<typeof DetailRow>

export default meta
type Story = StoryObj<typeof meta>

export const Default: Story = {
    render: (args) => (
        <dl className="w-96">
            <DetailRow {...args} />
        </dl>
    ),
}

export const WithBadgeValue: Story = {
    render: () => (
        <dl className="w-96">
            <DetailRow label="Stav členství">
                <Badge variant="success" size="sm">Aktivní</Badge>
            </DetailRow>
        </dl>
    ),
}

export const MemberDetailGroup: Story = {
    render: () => (
        <dl className="w-96">
            <DetailRow label="Jméno a příjmení">Jan Novák</DetailRow>
            <DetailRow label="Registrační číslo">ZBM4821</DetailRow>
            <DetailRow label="Datum narození">15. 3. 1998</DetailRow>
            <DetailRow label="Oddíl">Sokol Praha – orientační běh</DetailRow>
            <DetailRow label="Stav členství">
                <Badge variant="success" size="sm">Aktivní</Badge>
            </DetailRow>
        </dl>
    ),
}
