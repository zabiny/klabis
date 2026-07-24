import type {Meta, StoryObj} from '@storybook/react-vite'
import {useState} from 'react'
import {KlabisTable} from './KlabisTable'
import {TableCell} from './TableCell'
import {Badge} from '../Badge'
import type {SortDirection, SortState} from './types'

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
    {id: 4, name: 'Eva Malá', registrationNumber: 'ZBM1237', club: 'SK Žabovřesky', status: 'active'},
    {id: 5, name: 'Martin Svoboda', registrationNumber: 'ZBM1238', club: 'SK Žabovřesky', status: 'active'},
]

const meta = {
    title: 'Components/Table/KlabisTable',
    component: KlabisTable,
    tags: ['autodocs'],
    args: {
        data: members,
        children: null,
    },
} satisfies Meta<typeof KlabisTable<Member>>

export default meta
type Story = StoryObj<typeof meta>

export const Default: Story = {
    render: () => (
        <KlabisTable data={members}>
            <TableCell column="name" sortable>Jméno</TableCell>
            <TableCell column="registrationNumber">Reg. číslo</TableCell>
            <TableCell column="club">Klub</TableCell>
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

export const Sortable: Story = {
    render: () => {
        const [sort, setSort] = useState<SortState | undefined>({by: 'name', direction: 'asc'})

        const sorted = [...members].sort((a, b) => {
            const dir = sort?.direction === 'desc' ? -1 : 1
            const key = (sort?.by ?? 'name') as keyof Member
            return String(a[key]).localeCompare(String(b[key])) * dir
        })

        return (
            <KlabisTable
                data={sorted}
                currentSort={sort}
                onSortChange={(column, direction: SortDirection) => setSort({by: column, direction})}
                onSortReset={() => setSort(undefined)}
            >
                <TableCell column="name" sortable>Jméno</TableCell>
                <TableCell column="registrationNumber" sortable>Reg. číslo</TableCell>
                <TableCell column="club">Klub</TableCell>
            </KlabisTable>
        )
    },
}

export const Paginated: Story = {
    render: () => {
        const [page, setPage] = useState(0)
        const [rowsPerPage, setRowsPerPage] = useState(2)

        const start = page * rowsPerPage
        const pageData = members.slice(start, start + rowsPerPage)

        return (
            <KlabisTable
                data={pageData}
                page={{
                    size: rowsPerPage,
                    totalElements: members.length,
                    totalPages: Math.ceil(members.length / rowsPerPage),
                    number: page,
                }}
                currentPage={page}
                rowsPerPage={rowsPerPage}
                onPageChange={setPage}
                onRowsPerPageChange={(newSize) => {
                    setRowsPerPage(newSize)
                    setPage(0)
                }}
            >
                <TableCell column="name">Jméno</TableCell>
                <TableCell column="registrationNumber">Reg. číslo</TableCell>
                <TableCell column="club">Klub</TableCell>
            </KlabisTable>
        )
    },
}

export const ClickableRows: Story = {
    render: () => (
        <KlabisTable data={members} onRowClick={(member) => alert(`Detail člena: ${member.name}`)}>
            <TableCell column="name">Jméno</TableCell>
            <TableCell column="registrationNumber">Reg. číslo</TableCell>
            <TableCell column="club">Klub</TableCell>
        </KlabisTable>
    ),
}

export const Empty: Story = {
    render: () => (
        <KlabisTable data={[]} emptyMessage="Žádní členové neodpovídají zadaným filtrům">
            <TableCell column="name">Jméno</TableCell>
            <TableCell column="registrationNumber">Reg. číslo</TableCell>
            <TableCell column="club">Klub</TableCell>
        </KlabisTable>
    ),
}

export const ErrorState: Story = {
    render: () => (
        <KlabisTable data={[]} error={new Error('Nepodařilo se načíst seznam členů')}>
            <TableCell column="name">Jméno</TableCell>
            <TableCell column="registrationNumber">Reg. číslo</TableCell>
        </KlabisTable>
    ),
}

// KlabisTable switches to CardView automatically under the `(max-width: 639px)` media query —
// resize the Storybook preview below that width (or see Table/CardView for the mobile layout directly).
export const NarrowContainer: Story = {
    render: () => (
        <div className="max-w-sm resize-x overflow-auto border border-dashed border-border p-2">
            <KlabisTable data={members}>
                <TableCell column="name">Jméno</TableCell>
                <TableCell column="registrationNumber">Reg. číslo</TableCell>
                <TableCell column="club">Klub</TableCell>
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
        </div>
    ),
}
