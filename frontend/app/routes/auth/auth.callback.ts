import {redirect} from "react-router";

import {KLABIS_AUTH, authenticator} from "@/services/auth.server";
import {commitSession, getSession} from "@/sessions";
import {Route} from "./+types/auth.callback";

export async function loader({request, context}: Route.LoaderArgs) {
  const session = await getSession(request.headers.get("cookie"));
  const returnTo = session.get("returnTo");
  const user = await authenticator.authenticate(KLABIS_AUTH, request);
  console.log(request.url);
  if (user) {
    session.set("user", user);
    const headers = request.headers;
    headers.set("Set-Cookie", await commitSession(session));
    if (returnTo) {
      return redirect(returnTo, {headers});
    }
    return redirect("/", {headers});
  }
}
