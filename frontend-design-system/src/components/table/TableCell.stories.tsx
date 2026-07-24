import type {Meta, StoryObj} from '@storybook/react-vite'
import {TableCell} from './TableCell'
import {KlabisTable} from './KlabisTable'
import {Badge} from '../Badge'

// TableCell is a declarative column definition — it renders nothing itself.
// KlabisTable reads its props (column, sortable, dataRender, …) from children
// to build the actual table. These stories show it in that context.

interface Member extends Record<string, unknown> {
    id: number
    name: string
    registrationNumber: string
    status: 'active' | 'inactive'
}

const members: Member[] = [
    {id: 1, name: 'Jan Novák', registrationNumber: 'ZBM1234', status: 'active'},
    {id: 2, name: 'Petra Dvořáková', registrationNumber: 'ZBM1235', status: 'inactive'},
]

const meta = {
    title: 'Components/Table/TableCell',
    component: TableCell,
    tags: ['autodocs'],
    args: {
        column: 'name',
        children: 'Jméno',
    },
} satisfies Meta<typeof TableCell>

export default meta
type Story = StoryObj<typeof meta>

export const PlainColumn: Story = {
    render: () => (
        <KlabisTable data={members}>
            <TableCell column="name">Jméno</TableCell>
            <TableCell column="registrationNumber">Reg. číslo</TableCell>
        </KlabisTable>
    ),
}

export const SortableColumn: Story = {
    render: () => (
        <KlabisTable data={members}>
            <TableCell column="name" sortable>Jméno</TableCell>
            <TableCell column="registrationNumber">Reg. číslo</TableCell>
        </KlabisTable>
    ),
}

export const CustomDataRender: Story = {
    render: () => (
        <KlabisTable data={members}>
            <TableCell column="name">Jméno</TableCell>
            <TableCell
                column="status"
                dataRender={({value}) => (
                    <Badge variant={value === 'active' ? 'success' : 'default'}>
                        {value === 'active' ? 'Aktivní' : 'Neaktivní'}
                    </Badge>
                )}
            >
                Stav
            </TableCell>
        </KlabisTable>
    ),
}

export const HiddenColumn: Story = {
    render: () => (
        <KlabisTable data={members}>
            <TableCell column="name">Jméno</TableCell>
            <TableCell column="registrationNumber" hidden>Reg. číslo (skryto)</TableCell>
        </KlabisTable>
    ),
}
