import {
	type RouteConfig,
	index,
	layout,
	prefix,
	route,
} from "@react-router/dev/routes";

export default [
	route("/auth/callback", "routes/auth/auth.callback.ts"),
	route("/login", "routes/auth/login.ts"),
	layout("routes/layouts/main.tsx", [
		index("routes/_index.tsx"),
		route("/logout", "routes/auth/logout.ts"),
		route("/members", "routes/members.tsx"),
		...prefix("settings", [
			index("routes/settings/settings.index.tsx"),
			route(":userId/:part", "routes/settings/settings.[userId].[part].tsx"),
		]),
	]),

	// index("./home.tsx"),
	// route("about", "./about.tsx"),
	//
	// layout("./auth/layout.tsx", [
	// 	route("login", "./auth/login.tsx"),
	// 	route("register", "./auth/register.tsx"),
	// ]),
	//
	// ...prefix("concerts", [
	// 	index("./concerts/home.tsx"),
	// 	route(":city", "./concerts/city.tsx"),
	// 	route("trending", "./concerts/trending.tsx"),
	// ]),
] satisfies RouteConfig;
