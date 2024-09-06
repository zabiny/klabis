
import { Separator } from '@/components/ui/separator'
import { UserForm } from '@/components/settings/UserForm'
import type {LoaderFunctionArgs} from "@remix-run/server-runtime";
import {getUser} from "@/services/auth.server";
import {useLoaderData} from "@remix-run/react";

export const loader = async ({ request, context }: LoaderFunctionArgs) => {
  return getUser({request, context});
};

export default function Profile() {
  return <div className="space-y-6">
      <div>
        <h3 className="text-lg font-medium">Profil</h3>
        <p className="text-sm text-muted-foreground">
          Update your account settings. Set your preferred language and
          timezone.
        </p>
      </div>
      <Separator />
      {/*<ProfileForm defaultValues={{}}/>*/}
      <UserForm />
    </div>;
}
