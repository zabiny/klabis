import {
	Links,
	Meta,
	Outlet,
	Scripts,
	ScrollRestoration,
	isRouteErrorResponse,
	useRouteError, useLoaderData,
} from "@remix-run/react";

import { GlobalPendingIndicator } from "@/components/global-pending-indicator";
import { Header } from "@/components/header";
import {
	ThemeSwitcherSafeHTML,
	ThemeSwitcherScript,
} from "@/components/theme-switcher";

import "./globals.css";
import Main from "@/components/layout/Main";
import type {LoaderFunctionArgs} from "@remix-run/server-runtime";
import {getAuth} from "@/services/auth.server";
import {Toaster} from "@/components/ui/sonner";

function App({ children }: { children: React.ReactNode }) {
	return (
		<ThemeSwitcherSafeHTML lang="en">
			<head>
				<meta charSet="utf-8" />
				<meta name="viewport" content="width=device-width, initial-scale=1" />
				<Meta />
				<Links />
				<ThemeSwitcherScript />
			</head>
			<body>
				<GlobalPendingIndicator />
				{children}
				<ScrollRestoration />
				<Scripts />
				<Toaster richColors/>
			</body>
		</ThemeSwitcherSafeHTML>
	);
}

export const loader = async ({ request, context }: LoaderFunctionArgs) => {
	const user = await getAuth({ request, context });
	return user?.preferredUsername ?? "NO_USER";
};

export default function Root() {
	const userName = useLoaderData<string>() ?? "";
	return (
		<App>
			<Main userName={userName}>
				<Outlet />
			</Main>
		</App>
	);
}

export function ErrorBoundary() {
	const error = useRouteError();
	let status = 500;
	let message = "An unexpected error occurred.";
	if (isRouteErrorResponse(error)) {
		status = error.status;
		switch (error.status) {
			case 404:
				message = "Page Not Found";
				break;
		}
	} else {
		console.error(error);
	}

	return (
		<App>
			<div className="container prose py-8">
				<h1>{status}</h1>
				<p>{message}</p>
			</div>
		</App>
	);
}
