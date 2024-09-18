import {ActionFunctionArgs, redirect} from "@remix-run/server-runtime";
import {getAuth, getClient} from "@/services/auth.server";
import {json} from "@remix-run/react";

export async function loader({ request, context, params }: ActionFunctionArgs) {
  const user = await getAuth({request, context});
  console.log(user);
  return redirect(`/settings/${user.userId}`);
}
