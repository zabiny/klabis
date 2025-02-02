import { KLABIS_AUTH, authenticator } from "@/services/auth.server";
import { commitSession, getSession } from "@/sessions";
// app/routes/login.tsx
import { redirect } from "react-router";
import { Route } from "./+types/login";

// Finally, we can export a loader function where we check if the user is
// authenticated with `authenticator.isAuthenticated` and redirect to the
// dashboard if it is or return null if it's not
export async function loader({ request, context }: Route.LoaderArgs) {
	// If the user is already authenticated redirect to /dashboard directly
	const session = await getSession(request.headers.get("cookie"));
	const returnTo = session.get("returnTo");
	const user = await authenticator.authenticate(KLABIS_AUTH, request);
	if (user) {
		session.set("user", user);
		const headers = request.headers;
		headers.set("Set-Cookie", await commitSession(session));
		if (returnTo) {
			console.log(returnTo);
			return redirect(returnTo, { headers });
		}
		return redirect("/", { headers });
	}
}
