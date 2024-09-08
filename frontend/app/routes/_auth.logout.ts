import type { LoaderFunction } from "@remix-run/node"
import { getAuth } from "@/services/auth.server";

export let loader: LoaderFunction = async ({ request, context }) => {
  const auth = getAuth(context);
  await auth.logout(request, { redirectTo: "/" });
};
