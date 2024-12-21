import { MemberViewCompact } from "@/api/types";
import { MembersTable } from "@/components/members/MembersTable";
import { getClient } from "@/services/auth.server";
import { MetaFunction } from "react-router";
import { Route } from "./+types/members";

export const meta: MetaFunction = () => {
	return [
		{ title: "Členové" },
		{ name: "description", content: "Welcome to Remix!" },
	];
};

export async function loader({ context, request }: Route.LoaderArgs) {
	const client = await getClient({ context, request });
	const { data: memberList, error } = await client.GET("/members", {
		params: {
			query: { view: "compact" },
		},
	});
	console.log(memberList?.items, error);
	return { members: memberList?.items ?? ([] as MemberViewCompact[]) };
}

export default function Members({ loaderData }: Route.ComponentProps) {
	return (
		<div className="h-full flex-1 flex-col space-y-8 p-8 md:flex">
			<div className="flex items-center justify-between space-y-2">
				<div>
					<h2 className="text-2xl font-bold tracking-tight">Členové</h2>
					<p className="text-muted-foreground">Seznam všech členů oddilu</p>
				</div>
				<div className="flex items-center space-x-2">{/*<UserNav />*/}</div>
			</div>
			<MembersTable data={loaderData.members} />
		</div>
	);
}
