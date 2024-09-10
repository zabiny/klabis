import {MembersTable} from "@/components/members/MembersTable";
import {json, MetaFunction, useLoaderData} from "@remix-run/react";
import {getClient} from "@/services/auth.server";
import type {LoaderFunctionArgs} from "@remix-run/server-runtime";
import {MemberViewCompact} from "@/api/types";

export const meta: MetaFunction = () => {
  return [
    { title: "Členové" },
    { name: "description", content: "Welcome to Remix!" },
  ];
};

export async function loader({context, request}: LoaderFunctionArgs) {
  const client = await getClient({context, request});
  const { data: memberList, error } = await client.GET("/members", {query: {view: 'compact'}});
  return json({ members: memberList?.items ?? [] as MemberViewCompact[] });
}

export default function Members() {
  const memberList = useLoaderData<typeof loader>();
  return <div className="h-full flex-1 flex-col space-y-8 p-8 md:flex">
        <div className="flex items-center justify-between space-y-2">
          <div>
            <h2 className="text-2xl font-bold tracking-tight">Členové</h2>
            <p className="text-muted-foreground">Seznam všech členů oddilu</p>
          </div>
          <div className="flex items-center space-x-2">
            {/*<UserNav />*/}
          </div>
        </div>
        <MembersTable data={memberList.members} />
      </div>;
}
