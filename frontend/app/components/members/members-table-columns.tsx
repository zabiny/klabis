
import { type ColumnDef } from '@tanstack/react-table'

import { Badge } from '@/components/ui/badge'

import { type Member } from './member-schema'
import { MembersTableColumnHeader } from './members-table-column-header'
import { MembersTableRowActions } from './members-table-row-actions'
//import { trainingGroups } from '@/components/members/members-data.ts'

export const columns: ColumnDef<Member>[] = [
  {
    accessorKey: 'lastName',
    header: ({ column }) => (
      <MembersTableColumnHeader column={column} title="Příjmení" />
    ),
    cell: ({ row }) => (
      <div className="w-[80px]">{row.getValue('lastName')}</div>
    ),
  },
  {
    accessorKey: 'firstName',
    header: ({ column }) => (
      <MembersTableColumnHeader column={column} title="Jméno" />
    ),
    cell: ({ row }) => (
      <div className="w-[80px]">{row.getValue('firstName')}</div>
    ),
  },
  {
    accessorKey: 'registrationNumber',
    header: ({ column }) => (
      <MembersTableColumnHeader column={column} title="Registrační číslo" />
    ),
    cell: ({ row }) => (
      <div className="w-[80px]">{row.getValue('registrationNumber')}</div>
    ),
  },
  // {
  //   accessorKey: 'trainingGroup',
  //   header: ({ column }) => (
  //     <MembersTableColumnHeader column={column} title="Tréninkové skupiny" />
  //   ),
  //   cell: ({ row }) => {
  //     const group = trainingGroups.find(
  //       (label) => label.value === row.original.trainingGroup,
  //     )
  //
  //     if (!group) return null
  //
  //     return (
  //       <div className="flex space-x-2">
  //         <Badge key={row.original.trainingGroup} variant="outline">
  //           {group?.label}
  //         </Badge>
  //       </div>
  //     )
  //   },
  //   filterFn: (row, id, value) => {
  //     const goupsInFilter = value as string[]
  //     const group = row.getValue(id) as string
  //     return goupsInFilter.includes(group)
  //   },
  // },
  {
    id: 'actions',
    header: 'Akce',
    cell: ({ row }) => <MembersTableRowActions row={row} />,
  },
]
