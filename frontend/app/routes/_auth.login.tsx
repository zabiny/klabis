// app/routes/_auth.login.tsx
import type { ActionFunctionArgs, LoaderFunctionArgs } from "@remix-run/server-runtime";
import { getAuth } from "@/services/auth.server";

// Finally, we can export a loader function where we check if the user is
// authenticated with `authenticator.isAuthenticated` and redirect to the
// dashboard if it is or return null if it's not
export async function loader({ request, context }: LoaderFunctionArgs) {
  // If the user is already authenticated redirect to /dashboard directly
  const auth = getAuth(context);
  await auth.isAuthenticated(request, {
    successRedirect: "/"
  });

  return await auth.authenticate("discord", request, {
    successRedirect: "/",
    failureRedirect: "/login",
  });
};
