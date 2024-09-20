
import { DotsHorizontalIcon } from '@radix-ui/react-icons'
import { type Row } from '@tanstack/react-table'

import { Button } from '@/components/ui/button'
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem, DropdownMenuLabel, DropdownMenuSeparator,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu'
import {Link} from "@remix-run/react";
import {HandCoins, Settings} from "lucide-react";

interface MembersTableRowActionsProps<TData> {
  row: Row<TData>
}

export function MembersTableRowActions<TData>({
  row,
}: MembersTableRowActionsProps<TData>) {
  return (
    <DropdownMenu>
      <DropdownMenuTrigger asChild>
        <Button
          variant="ghost"
          className="flex h-8 w-8 p-0 data-[state=open]:bg-muted"
        >
          <DotsHorizontalIcon className="h-4 w-4" />
          <span className="sr-only">Open menu</span>
        </Button>
      </DropdownMenuTrigger>
      <DropdownMenuContent align="end" className="w-[160px]">
        <DropdownMenuLabel>
          {row.original.firstName} {row.original.lastName}
        </DropdownMenuLabel>
        <DropdownMenuSeparator />
        <DropdownMenuItem asChild>
          <Link to={`/settings/${row.original.id}/personal`}><Settings className="mr-2 h-4 w-4"/> Nastavení</Link>
        </DropdownMenuItem>
        <DropdownMenuItem asChild>
          <Link to={`/members/${row.original.id}/suspend`}><HandCoins className="mr-2 h-4 w-4"/> Finance</Link>
        </DropdownMenuItem>
        <DropdownMenuSeparator />
        <DropdownMenuItem asChild>
          <Link to={`/members/${row.original.id}/suspend`}>Pozastavit členství</Link>
        </DropdownMenuItem>

        {/*<DropdownMenuItem>Make a copy</DropdownMenuItem>*/}
        {/*<DropdownMenuItem>Favorite</DropdownMenuItem>*/}
        {/*<DropdownMenuSeparator />*/}
        {/*<DropdownMenuItem>*/}
        {/*  Delete*/}
        {/*  <DropdownMenuShortcut>⌘⌫</DropdownMenuShortcut>*/}
        {/*</DropdownMenuItem>*/}
      </DropdownMenuContent>
    </DropdownMenu>
  )
}
