// app/services/auth.server.ts
import { Authenticator } from "remix-auth";
import { sessionStorage } from "./session.server";
import {OAuth2Strategy} from "remix-auth-oauth2";
import {AppLoadContext, createCookieSessionStorage} from "@remix-run/cloudflare";
import type {LoaderFunctionArgs} from "@remix-run/server-runtime";

// Create an instance of the authenticator, pass a generic with what
// strategies will return and will store in the session
type User = string;

export async function getUser({ request, context }: Omit<LoaderFunctionArgs, "params">): Promise<User | null> {
  const auth = getAuth(context);
  return auth.isAuthenticated(request);
}

export function getAuth(context: AppLoadContext) {

  const sessionStorage = createCookieSessionStorage({
    cookie: {
      name: "_klabis_session", // use any name you want here
      sameSite: "lax", // this helps with CSRF
      path: "/", // remember to add this so the cookie will work in all routes
      httpOnly: true, // for security reasons, make this cookie http only
      secrets: [context.cloudflare.env.COOKIE_SECRET], // replace this with an actual secret
      secure: process.env.NODE_ENV === "production", // enable this in prod only
    },
  });

  const authenticator = new Authenticator<User>(sessionStorage);
  authenticator.use(
    new OAuth2Strategy<
      User,
      { provider: "discord" },
      { id_token: string }
    >(
      {
        clientId: context.cloudflare.env.AUTH_CLIENT_ID!,
        clientSecret: context.cloudflare.env.AUTH_CLIENT_SECRET!,

        authorizationEndpoint: "https://discord.com/oauth2/authorize",
        tokenEndpoint: "https://discord.com/api/oauth2/token",
        redirectURI: "http://localhost:5173/auth/callback",

        // codeChallengeMethod: "S256", // optional
        scopes: ["openid", "email", "identify"], // optional

        // authenticateWith: "request_body", // optional
      },
      async ({ tokens, profile, context, request }) => {
        const res = await fetch("https://discordapp.com/api/users/@me", {
          headers: {
            authorization: `Bearer ${tokens.access_token}`
          }
        });
        const json = await res.json();
        // @ts-ignore
        return json.username;
      },
    ),
    // this is optional, but if you setup more than one OAuth2 instance you will
    // need to set a custom name to each one
    "discord",
  );
  return authenticator;
}
