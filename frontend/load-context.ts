import { type PlatformProxy } from "wrangler";

// When using `wrangler.toml` to configure bindings,
// `wrangler types` will generate types for those bindings
// into the global `Env` interface.
// Need this empty interface so that typechecking passes
// even if no `wrangler.toml` exists.
// eslint-disable-next-line @typescript-eslint/no-empty-interface
interface Env {
	BASE_URL: string;
	COOKIE_SECRET: string;
	AUTH_CLIENT_SECRET: string;
	AUTH_CLIENT_ID: string;
}

type Cloudflare = Omit<PlatformProxy<Env>, "dispose">;

// @ts-ignore
declare module "@remix-run/cloudflare" {
	interface AppLoadContext {
		cloudflare: Cloudflare;
	}
}
