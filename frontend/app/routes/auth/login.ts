// app/routes/login.tsx
import type {
	ActionFunctionArgs,
	LoaderFunctionArgs,
} from "@remix-run/server-runtime";
import { authenticate, getAuth, KLABIS_AUTH } from "@/services/auth.server";

// Finally, we can export a loader function where we check if the user is
// authenticated with `authenticator.isAuthenticated` and redirect to the
// dashboard if it is or return null if it's not
export async function loader({ request, context }: LoaderFunctionArgs) {
	// If the user is already authenticated redirect to /dashboard directly
	const { auth } = authenticate(context);
	return await auth.authenticate(KLABIS_AUTH, request, {
		successRedirect: "/",
		failureRedirect: "/login",
	});
}
