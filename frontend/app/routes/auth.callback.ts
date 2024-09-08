import { type LoaderFunctionArgs } from "@remix-run/cloudflare";
import { z } from "zod";

import { getAuth } from "@/services/auth.server";

export async function loader({ request, params, context }: LoaderFunctionArgs) {
  let auth = getAuth(context);
  await auth.authenticate("discord", request, {
    successRedirect: "/",
    failureRedirect: "/",
  });
}
