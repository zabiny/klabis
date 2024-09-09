import {MembersTable} from "@/components/members/MembersTable";
import {json, MetaFunction, useLoaderData} from "@remix-run/react";
import {membersApi, MembersGetView, type MemberViewCompact} from "@/api";
import {AppLoadContext} from "@remix-run/cloudflare";
import {getAuth, getAuthHeaders} from "@/services/auth.server";
import type {LoaderFunctionArgs} from "@remix-run/server-runtime";

const _mainMembers = [
  {
    firstName: 'John',
    lastName: 'Doe',
    registrationNumber: 'ZBM0001',
    trainingGroup: 'dorost',
  },
  {
    firstName: 'Jane',
    lastName: 'Smith',
    registrationNumber: 'ZBM0002',
    trainingGroup: 'hobby',
  },
  {
    firstName: 'Alice',
    lastName: 'Johnson',
    registrationNumber: 'ZBM0003',
    trainingGroup: 'zabicky',
  },
  {
    firstName: 'Bob',
    lastName: 'Brown',
    registrationNumber: 'ZBM0004',
    trainingGroup: 'zabicky',
  },
  {
    firstName: 'Michael',
    lastName: 'Davis',
    registrationNumber: 'ZBM0005',
    trainingGroup: 'dorost',
  },
  {
    firstName: 'Sarah',
    lastName: 'Wilson',
    registrationNumber: 'ZBM0006',
    trainingGroup: 'hobby',
  },
  {
    firstName: 'David',
    lastName: 'Martinez',
    registrationNumber: 'ZBM0007',
    trainingGroup: 'zabicky',
  },
  {
    firstName: 'Emily',
    lastName: 'Taylor',
    registrationNumber: 'ZBM0008',
    trainingGroup: 'dorost',
  },
  {
    firstName: 'Matthew',
    lastName: 'Anderson',
    registrationNumber: 'ZBM0009',
    trainingGroup: 'zabicky',
  },
  {
    firstName: 'Olivia',
    lastName: 'Thomas',
    registrationNumber: 'ZBM0010',
    trainingGroup: 'dorost',
  },
  {
    firstName: 'Daniel',
    lastName: 'Hernandez',
    registrationNumber: 'ZBM0011',
    trainingGroup: 'zabicky',
  },
  {
    firstName: 'Sophia',
    lastName: 'Moore',
    registrationNumber: 'ZBM0012',
    trainingGroup: 'dorost',
  },
  {
    firstName: 'James',
    lastName: 'Clark',
    registrationNumber: 'ZBM0013',
    trainingGroup: 'hobby',
  },
  {
    firstName: 'Isabella',
    lastName: 'Lewis',
    registrationNumber: 'ZBM0014',
    trainingGroup: 'zabicky',
  },
  {
    firstName: 'Alexander',
    lastName: 'Walker',
    registrationNumber: 'ZBM0015',
    trainingGroup: '',
  },
  {
    firstName: 'Mia',
    lastName: 'Hall',
    registrationNumber: 'ZBM0016',
    trainingGroup: 'dorost',
  },
  {
    firstName: 'William',
    lastName: 'Young',
    registrationNumber: 'ZBM0017',
    trainingGroup: 'hobby',
  },
  {
    firstName: 'Ethan',
    lastName: 'Allen',
    registrationNumber: 'ZBM0018',
    trainingGroup: '',
  },
  {
    firstName: 'Charlotte',
    lastName: 'Adams',
    registrationNumber: 'ZBM0019',
    trainingGroup: 'dorost',
  },
  {
    firstName: 'Abigail',
    lastName: 'Wright',
    registrationNumber: 'ZBM0020',
    trainingGroup: '',
  },
  {
    firstName: 'Liam',
    lastName: 'King',
    registrationNumber: 'ZBM0021',
    trainingGroup: 'dorost',
  },
  {
    firstName: 'Harper',
    lastName: 'Scott',
    registrationNumber: 'ZBM0022',
    trainingGroup: 'zabicky',
  },
  {
    firstName: 'Elijah',
    lastName: 'Morris',
    registrationNumber: 'ZBM0023',
    trainingGroup: 'dorost',
  },
  {
    firstName: 'Avery',
    lastName: 'Parker',
    registrationNumber: 'ZBM0024',
    trainingGroup: 'hobby',
  },
  {
    firstName: 'Sofia',
    lastName: 'Evans',
    registrationNumber: 'ZBM0025',
    trainingGroup: 'zabicky',
  },
]

export const meta: MetaFunction = () => {
  return [
    { title: "Členové" },
    { name: "description", content: "Welcome to Remix!" },
  ];
};

export async function loader(context: LoaderFunctionArgs) {
  const members = await membersApi.membersGet({view: MembersGetView.Compact}, { headers: await getAuthHeaders(context) })
  .then((memberList) => memberList.items as MemberViewCompact[]);
  return json({ members });
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
