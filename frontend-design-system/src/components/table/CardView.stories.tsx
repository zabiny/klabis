import type {Meta, StoryObj} from '@storybook/react-vite'
import {CardView} from './CardView'
import {Badge} from '../Badge'
import type {ColumnDef} from './types'

interface Member extends Record<string, unknown> {
    id: number
    name: string
    registrationNumber: string
    club: string
    status: 'active' | 'inactive'
}

const members: Member[] = [
    {id: 1, name: 'Jan Novák', registrationNumber: 'ZBM1234', club: 'SK Žabovřesky', status: 'active'},
    {id: 2, name: 'Petra Dvořáková', registrationNumber: 'ZBM1235', club: 'SK Žabovřesky', status: 'active'},
    {id: 3, name: 'Tomáš Král', registrationNumber: 'ZBM1236', club: 'SK Žabovřesky', status: 'inactive'},
]

const columns: ColumnDef[] = [
    {name: 'name', label: 'Jméno', hidden: false, sortable: false, alwaysVisible: false},
    {name: 'registrationNumber', label: 'Reg. číslo', hidden: false, sortable: false, alwaysVisible: false},
    {name: 'club', label: 'Klub', hidden: false, sortable: false, alwaysVisible: false},
    {
        name: 'status',
        label: 'Stav',
        hidden: false,
        sortable: false,
        alwaysVisible: false,
        dataRender: ({value}) => (
            <Badge variant={value === 'active' ? 'success' : 'default'}>
                {value === 'active' ? 'Aktivní' : 'Neaktivní'}
            </Badge>
        ),
    },
]

const meta = {
    title: 'Components/Table/CardView',
    component: CardView,
    tags: ['autodocs'],
    args: {
        data: members,
        columns,
        emptyMessage: 'Žádní členové',
    },
} satisfies Meta<typeof CardView<Member>>

export default meta
type Story = StoryObj<typeof meta>

export const Default: Story = {
    render: () => (
        <div className="max-w-sm">
            <CardView data={members} columns={columns} emptyMessage="Žádní členové"/>
        </div>
    ),
}

export const Clickable: Story = {
    render: () => (
        <div className="max-w-sm">
            <CardView
                data={members}
                columns={columns}
                emptyMessage="Žádní členové"
                onRowClick={(member) => alert(`Detail člena: ${member.name}`)}
            />
        </div>
    ),
}

export const Empty: Story = {
    render: () => (
        <div className="max-w-sm">
            <CardView data={[]} columns={columns} emptyMessage="Žádní členové neodpovídají zadaným filtrům"/>
        </div>
    ),
}
