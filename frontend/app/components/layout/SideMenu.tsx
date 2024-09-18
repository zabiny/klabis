import { Sheet, SheetContent, SheetTrigger } from '@/components/ui/sheet'
import { Button } from '@/components/ui/button'

import { Menu, LucideSidebar } from 'lucide-react'
import type { ReactNode } from 'react'

export const SideMenu = ({ children }: { children: ReactNode }) => (
  <Sheet>
    <SheetTrigger asChild>
      <Button variant="outline" size="icon" className="shrink-0 md:hidden">
        <LucideSidebar className="h-5 w-5" />
        <span className="sr-only">Toggle navigation menu</span>
      </Button>
    </SheetTrigger>
    <SheetContent side="left" className="flex flex-col">
      {children}
    </SheetContent>
  </Sheet>
)
