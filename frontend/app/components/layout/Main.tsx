
import { Home, Trophy, HandCoins, Settings, Users, Bell, SquareLibrary } from 'lucide-react'

import { CommandBox } from '@/components/layout/CommandBox'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { SideMenu } from '@/components/layout/SideMenu'
import { UserMenu } from '@/components/layout/UserMenu'
import {Outlet, useLoaderData, useLocation } from "@remix-run/react";
import {cn} from "@/lib/utils";
import type {LoaderFunctionArgs} from "@remix-run/server-runtime";
import {getUser} from "@/services/auth.server";

const navItems = [
  {
    title: 'Domů',
    href: '/',
    icon: Home,
  },
  {
    title: 'Závody',
    href: '/#',
    icon: Trophy,
    badge: true,
  },
  {
    title: 'Finance',
    href: '/#',
    icon: HandCoins,
  },
  {
    title: 'Členové',
    href: '/members',
    icon: Users,
  },
  {
    title: 'Nastavení',
    href: '/settings/profile',
    icon: Settings,
  },
]


export default function Main({userName, children}: {userName: string, children: React.ReactNode}) {
  const pathname = useLocation().pathname;
  return <div
    className="grid min-h-screen w-full md:grid-cols-[220px_1fr] lg:grid-cols-[280px_1fr]"
  >
    <div className="hidden border-r bg-muted/40 md:block">
      <div className="flex h-full max-h-screen flex-col gap-2">
        <div className="flex h-14 items-center border-b px-4 lg:h-[60px] lg:px-6">
          <div className="flex items-center gap-2 font-semibold">
            <SquareLibrary className="h-6 w-6"/>
            <span>Klabis</span>
          </div>
          <Button variant="outline" size="icon" className="ml-auto h-8 w-8">
            <Bell className="h-4 w-4"/>
            <span className="sr-only">Toggle notifications</span>
          </Button>
        </div>
        <div className="flex-1">
          <nav className="grid items-start px-2 text-sm font-medium lg:px-4">
            {
              navItems.map((item) => (
                <a
                  key={item.title}
                  href={item.href}
                  className={cn(
                    'flex items-center gap-3 rounded-lg px-3 py-2 transition-all hover:text-primary',
                    {
                      'bg-muted': item.href === pathname,
                      'text-muted-foreground': item.href !== pathname,
                    },
                  )}
                >
                  {item.icon && <item.icon className="h-4 w-4"/>}
                  {item.title}
                  {item.badge && (
                    <Badge className="ml-auto flex h-6 w-6 shrink-0 items-center justify-center rounded-full">
                      6
                    </Badge>
                  )}
                </a>
              ))
            }
          </nav>
        </div>
      </div>
    </div>
    <div className="flex flex-col">
      <header
        className="flex h-14 items-center gap-4 border-b bg-muted/40 px-4 lg:h-[60px] lg:px-6"
      >
        <SideMenu>
          <nav className="grid gap-2 text-lg font-medium">
            <a
              href="#"
              className="mb-4 flex items-center gap-2 text-lg font-semibold"
            >
              <SquareLibrary className="h-6 w-6"/>
              <span>Klabis</span>
            </a>
            {
              navItems.map((item) => (
                <a
                  key={item.title}
                  href={item.href}
                  className={cn(
                    'mx-[-0.65rem] flex items-center gap-4 rounded-xl px-3 py-2  hover:text-foreground',
                    {
                      'text-foreground': item.href === pathname,
                      'bg-muted': item.href === pathname,
                      'text-muted-foreground': item.href !== pathname,
                    },
                  )}
                >
                  {item.icon && <item.icon className="h-5 w-5"/>}
                  {item.title}
                  {item.badge && (
                    <Badge className="ml-auto flex h-6 w-6 shrink-0 items-center justify-center rounded-full">
                      6
                    </Badge>
                  )}
                </a>
              ))
            }
          </nav>
        </SideMenu>
        <div className="w-full flex-1">
          <CommandBox />
        </div>
        <UserMenu name={userName} />
      </header>
      <main className="flex flex-1 flex-col gap-4 p-4 lg:gap-6 lg:p-6">
        {children}
        {/*<div class="flex items-center">*/}
        {/*  <h1 class="text-lg font-semibold md:text-2xl">Inventory</h1>*/}
        {/*</div>*/}
        {/*<div*/}
        {/*  class="flex flex-1 items-center justify-center rounded-lg border border-dashed shadow-sm"*/}
        {/*&gt;*/}
        {/*  <div class="flex flex-col items-center gap-1 text-center">*/}
        {/*    <h3 class="text-2xl font-bold tracking-tight">*/}
        {/*      You have no products*/}
        {/*    </h3>*/}
        {/*    <p class="text-sm text-muted-foreground">*/}
        {/*      You can start selling as soon as you add a product.*/}
        {/*    </p>*/}
        {/*    <Button className="mt-4">Add Product</Button>*/}
        {/*  </div>*/}
        {/*</div>*/}
      </main>
    </div>
  </div>
}


