import { vitePlugin as remix,
	cloudflareDevProxyVitePlugin} from "@remix-run/dev";
import { defineConfig } from "vite";
import envOnly from "vite-env-only";
import tsconfigPaths from "vite-tsconfig-paths";

export default defineConfig({
	plugins: [
		envOnly(),
		tsconfigPaths(),
		cloudflareDevProxyVitePlugin(),
		remix({
			routes(defineRoutes) {
				return defineRoutes((route) => {
					route("/auth/callback", "routes/auth/auth.callback.ts");
					route("/login", "routes/auth/login.ts");
					route("/logout", "routes/auth/logout.ts");
				});
			},
			future: {
				v3_fetcherPersist: true,
				v3_relativeSplatPath: true,
				v3_throwAbortReason: true,
			},
		}),
	],
});
