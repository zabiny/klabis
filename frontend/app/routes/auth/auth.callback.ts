import {type LoaderFunctionArgs} from "@remix-run/cloudflare";
import {z} from "zod";

import {authenticate, KLABIS_AUTH} from "@/services/auth.server";

export async function loader({request, context}: LoaderFunctionArgs) {
  const {auth} = authenticate(context);
  await auth.authenticate(KLABIS_AUTH, request, {
    successRedirect: "/",
    failureRedirect: "/",
  });
}
