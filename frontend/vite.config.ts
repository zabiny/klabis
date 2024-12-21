import { reactRouter } from "@react-router/dev/vite";
import { cloudflareDevProxy } from "@react-router/dev/vite/cloudflare";
import { defineConfig } from "vite";
import envOnly from "vite-env-only";
import tsconfigPaths from "vite-tsconfig-paths";

export default defineConfig({
	plugins: [envOnly(), tsconfigPaths(), cloudflareDevProxy(), reactRouter()],
});
