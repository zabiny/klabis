import { paths } from "@/api/schema";
import { User, commitSession, getSession } from "@/sessions";
import { jwtDecode } from "jwt-decode";
import createClient from "openapi-fetch";
import { AppLoadContext } from "react-router";
import { redirect } from "react-router";
import { Authenticator } from "remix-auth";
import { OAuth2Strategy } from "remix-auth-oauth2";

type IdTokenPayload = {
	sub: string;
	preferred_username: string;
	given_name: string;
	family_name: string;
};

// Create an instance of the authenticator, pass a generic with what
// strategies will return and will store in the session

async function authenticate(request: Request) {
	const session = await getSession(request.headers.get("cookie"));
	const user = session.get("user");
	if (user) return user;
	session.flash("returnTo", request.url);
	throw redirect("/login");
}

async function refreshToken(
	user: User,
	authenticator: Authenticator<User>,
	request: Request,
	headers: Headers = new Headers(),
) {
	try {
		const strategy = authenticator.get(KLABIS_AUTH) as OAuth2Strategy<User>;
		const tokens = await strategy.refreshToken(user.refreshToken);
		const accessToken = tokens.accessToken();
		const refreshToken = tokens.refreshToken();

		const session = await getSession();
		session.set("user", {
			...user,
			accessToken,
			refreshToken,
		});

		headers.append("Set-Cookie", await commitSession(session));

		// redirect to the same URL if the request was a GET (loader)
		if (request.method === "GET") throw redirect(request.url, { headers });

		// return the access token so you can use it in your action
		return { ...user, accessToken, refreshToken, headers };
	} catch (error) {
		// if the refresh token is invalid, redirect to login
		if (error instanceof Error) throw redirect("/login");
		// rethrow redirect
		throw error;
	}
}

export const KLABIS_AUTH = "klabis-auth";

export const strategy = new OAuth2Strategy<User>(
	{
		clientId: import.meta.env.VITE_AUTH_CLIENT_ID,
		clientSecret: import.meta.env.VITE_AUTH_CLIENT_SECRET,

		authorizationEndpoint: `${
			import.meta.env.VITE_AUTH_SERVER
		}/oauth/authorize`,
		tokenEndpoint: `${import.meta.env.VITE_AUTH_SERVER}/oauth/token`,
		redirectURI: `${import.meta.env.VITE_BASE_URL}/auth/callback`,
		tokenRevocationEndpoint: `${
			import.meta.env.VITE_AUTH_SERVER
		}/oauth2/revoke`,

		// codeChallengeMethod: "S256", // optional
		scopes: ["openid", "profile", "email"], // optional

		// authenticateWith: "request_body", // optional
	},
	async ({ tokens, request }) => {
		// const res = await fetch("https://api.klabis.otakar.io/oidc/userinfo", {
		// 	headers: {
		// 		authorization: `Bearer ${tokens.access_token}`,
		// 	},
		// });
		// const json = await res.json();
		const accessToken: string = tokens.accessToken();
		const refreshToken: string = tokens.refreshToken() as string;
		const parsed = jwtDecode<IdTokenPayload>(tokens.idToken());
		const userId = parsed.sub;
		const preferredUsername = parsed.preferred_username as string;
		const givenName = parsed.given_name;
		const familyName = parsed.family_name;

		return {
			userId,
			preferredUsername,
			givenName,
			familyName,
			accessToken,
			refreshToken,
		};
	},
);

const getAuthenticator = () => {
	const authenticator = new Authenticator<User>();
	authenticator.use(strategy, KLABIS_AUTH);
	return authenticator;
};

export const authenticator = getAuthenticator();

// https://sergiodxa.com/articles/working-with-refresh-tokens-in-remix
// https://github.com/sergiodxa/remix-auth-oauth2
export async function getAuth({
	request,
	context,
	headers = new Headers(),
}: {
	request: Request;
	context: AppLoadContext;
	headers?: Headers;
}): Promise<User & { headers: Headers }> {
	const user = await authenticate(request);

	// if not found, redirect to login, this means the user is not even logged-in
	if (!user.accessToken || !user.refreshToken) {
		throw redirect("/login");
	}

	if (new Date(getExpirationDate(user.accessToken)) > new Date()) {
		return { ...user, headers: new Headers() };
	}

	return await refreshToken(user, authenticator, request, headers);
}

export async function getClient({
	context,
	request,
}: { request: Request; context: AppLoadContext }) {
	const user = await getAuth({ context, request });
	const client = createClient<paths>({
		baseUrl: import.meta.env.VITE_AUTH_SERVER,
		headers: {
			Authorization: `Bearer ${user.accessToken}`,
		},
	});
	return client;
}

function getExpirationDate(token: string): Date {
	const parsed = jwtDecode(token);
	return new Date((parsed.exp as number) * 1000);
}
