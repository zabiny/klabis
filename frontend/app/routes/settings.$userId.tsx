
import { Separator } from '@/components/ui/separator'
import { cn } from '@/lib/utils'
import {Button, buttonVariants} from '@/components/ui/button'
import {
  Breadcrumb,
  BreadcrumbItem,
  BreadcrumbLink,
  BreadcrumbList,
  BreadcrumbPage,
  BreadcrumbSeparator,
} from '@/components/ui/breadcrumb'
import {Form, json, Outlet, useLoaderData, useLocation} from '@remix-run/react'
import {ActionFunctionArgs, LoaderFunctionArgs, redirect} from "@remix-run/server-runtime";
import {getAuth, getClient} from "@/services/auth.server";



export async function loader({ request, context, params }: ActionFunctionArgs) {
  const client = await getClient({request, context});
  const memberId = +(params.userId ?? "");
  const {data, error} = await client.GET('/members/{memberId}', {
    params: {
      path: { memberId }
    }
  });
  if (error) {
    return redirect('/404');
  }
  return json(data);
}

export default function SettingsId() {
  const user = useLoaderData<typeof loader>();
  const pathname = useLocation().pathname;
  const sidebarNavItems = [
    {
      title: 'Profil',
      href: `/settings/${user.id}`,
    },
    {
      title: 'Adresa',
      href: '/#',
    },
    {
      title: 'Kontakt',
      href: '/#',
    },
    {
      title: 'Přílašení',
      href: '/#',
    },
    {
      title: 'Admin',
      href: `/settings/${user.id}/admin`,
    },
  ]
  return <div className="mx-10 hidden space-y-6 pb-16 md:block">
    <Breadcrumb>
      <BreadcrumbList>
        <BreadcrumbItem> Nastavení </BreadcrumbItem>
        <BreadcrumbSeparator />
        <BreadcrumbItem> {user.registrationNumber} </BreadcrumbItem>
      </BreadcrumbList>
    </Breadcrumb>
    <div className="space-y-0.5">
      <h2 className="text-2xl font-bold tracking-tight">Nastavení</h2>
      <p className="text-muted-foreground">
        Manage your account settings and set e-mail preferences.
      </p>
    </div>
    <Separator className="my-6" />
    <div className="flex flex-col space-y-8 lg:flex-row lg:space-x-12 lg:space-y-0">
      <aside className="-mx-4 lg:w-1/5">
        <nav className="flex space-x-2 lg:flex-col lg:space-x-0 lg:space-y-1">
          {
            sidebarNavItems.map((item) => (
              <a
                key={item.title}
                href={item.href}
                className={cn(
                  buttonVariants({ variant: 'ghost' }),
                  pathname === item.href
                    ? 'bg-muted hover:bg-muted'
                    : 'hover:bg-transparent hover:underline',
                  'justify-start',
                )}
              >
                {item.title}
              </a>
            ))
          }
        </nav>
      </aside>
      <div className="flex-1 lg:max-w-2xl">
          <Outlet />
      </div>
    </div>
  </div>;
}
