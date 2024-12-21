import { commitSession, destroySession, getSession } from "@/sessions";
import { redirect } from "react-router";
import { Route } from "./+types/logout";

export const loader = async ({ request }: Route.LoaderArgs) => {
	let session = await getSession(request.headers.get("cookie"));
	await destroySession(session);
	session = await getSession();
	return redirect("http://api.klabis.otakar.io/logout", {
		headers: { "Set-Cookie": await commitSession(session) },
	});
};
