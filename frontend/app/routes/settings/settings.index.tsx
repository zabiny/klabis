import {getAuth} from "@/services/auth.server";
import {redirect} from "react-router";
import {Route} from "./+types/settings.index";

export async function loader({ request, context }: Route.LoaderArgs) {
	const user = await getAuth({ request, context });
	return redirect(`/settings/${user.userId}/personal`);
}
