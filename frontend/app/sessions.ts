import { createCookieSessionStorage } from "react-router";

export type User = {
	userId: string;
	preferredUsername: string;
	givenName: string;
	familyName: string;
	accessToken: string;
	refreshToken: string;
};

type SessionData = {
	user: User;
};

// read only once
type SessionFlashData = {
	returnTo: string;
};

const { getSession, commitSession, destroySession } =
	createCookieSessionStorage<SessionData, SessionFlashData>({
		cookie: {
			name: "_klabis_session", // use any name you want here
			sameSite: "lax", // this helps with CSRF
			path: "/", // remember to add this so the cookie will work in all routes
			httpOnly: true, // for security reasons, make this cookie http only
			secrets: [import.meta.env.VITE_COOKIE_SECRET], // replace this with an actual secret
			secure: import.meta.env.PROD, // enable this in prod only
		},
	});

export { getSession, commitSession, destroySession };
