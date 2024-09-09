import type { MetaFunction } from "@remix-run/react";
import type {LoaderFunctionArgs} from "@remix-run/server-runtime";
import {authenticate} from "@/services/auth.server";

export const meta: MetaFunction = () => {
	return [
		{ title: 'TODO' },
		{ name: "description", content: "Welcome to Remix!" },
	];
};

export async function loader({ request, context }: LoaderFunctionArgs) {
	// If the user is already authenticated redirect to /dashboard directly
	const { auth } = authenticate(context);
	return await auth.isAuthenticated(request, {
		failureRedirect: "/login"
	});
};

export default function Index() {
	return (
		<main className="container prose py-8">
			<h1>Welcome to Remix</h1>
			<ul>
				<li>
					<a
						target="_blank"
						href="https://remix.run/tutorials/blog"
						rel="noreferrer"
					>
						15m Quickstart Blog Tutorial
					</a>
				</li>
				<li>
					<a
						target="_blank"
						href="https://remix.run/tutorials/jokes"
						rel="noreferrer"
					>
						Deep Dive Jokes App Tutorial
					</a>
				</li>
				<li>
					<a target="_blank" href="https://remix.run/docs" rel="noreferrer">
						Remix Docs
					</a>
				</li>
			</ul>
		</main>
	);
}
