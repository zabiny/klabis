
import { Cross2Icon } from '@radix-ui/react-icons'
import { type Table } from '@tanstack/react-table'

import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'

// import { trainingGroups } from '@/components/members/members-data'
import { MembersTableFacetedFilter } from './members-table-faceted-filter'
import {UserPlus} from "lucide-react";
import {Link} from "@remix-run/react";

interface MembersTableToolbarProps<TData> {
  table: Table<TData>
}

export function MembersTableToolbar<TData>({
  table,
}: MembersTableToolbarProps<TData>) {
  const isFiltered = table.getState().columnFilters.length > 0

  return (
    <div className="flex items-center justify-between">
      <div className="flex flex-1 items-center space-x-2">
        <Input
          placeholder="Hledat..."
          value={table.getState().globalFilter ?? ''}
          onChange={(event) => table.setGlobalFilter(event.target.value)}
          className="h-8 w-[150px] lg:w-[250px]"
        />
        {/*{table.getColumn('trainingGroup') && (*/}
        {/*  <MembersTableFacetedFilter*/}
        {/*    column={table.getColumn('trainingGroup')}*/}
        {/*    title="Tréninková skupina"*/}
        {/*    options={trainingGroups}*/}
        {/*  />*/}
        {/*)}*/}
        {isFiltered && (
          <Button
            variant="ghost"
            onClick={() => {
              table.resetGlobalFilter()
              table.resetColumnFilters()
            }}
            className="h-8 px-2 lg:px-3"
          >
            Reset
            <Cross2Icon className="ml-2 h-4 w-4" />
          </Button>
        )}
      </div>
      <div>
        <Button
          variant="secondary"
          className="h-8 px-2 lg:px-3"
          asChild
        >
          <Link to={'/members/new'}>
            <UserPlus className="mr-2 h-4 w-4" /> Registrovat člena
          </Link>
        </Button>
      </div>
    </div>
  )
}
