import {type ReactElement} from "react";
import {Link} from "react-router-dom";
import type {EntityModel} from "../../api";
import {TableCell} from "../../components/KlabisTable";
import {HalEmbeddedTable} from "../../components/HalNavigator2/HalEmbeddedTable.tsx";
import {useHalPageData} from "../../hooks/useHalPageData.ts";

interface MemberSummaryData extends EntityModel<{
    id: string,
    registrationNumber: string,
    lastName: string,
    firstName: string,
}> {
}

export const MembersPage = (): ReactElement => {
    const {route, resourceData} = useHalPageData();

    return <div className="flex flex-col gap-8">
        <h1 className="text-3xl font-bold text-text-primary">Členové</h1>

        <div className="flex flex-col gap-4">
            <div className="flex items-center justify-between">
                <h2 className="text-xl font-bold text-text-primary">Seznam členů</h2>
                {resourceData?._templates?.registerMember && (
                    <Link
                        to="/members/new"
                        className="inline-flex items-center px-4 py-2 text-sm font-medium rounded-md bg-primary text-white hover:bg-primary-light"
                    >
                        Registrovat člena
                    </Link>
                )}
            </div>
            <HalEmbeddedTable<MemberSummaryData> collectionName={"memberSummaryResponseList"}
                                                  defaultOrderBy={"lastName"}
                                                  onRowClick={route.navigateToResource}>
                <TableCell sortable column={"registrationNumber"}>Reg. číslo</TableCell>
                <TableCell sortable column={"lastName"}>Příjmení</TableCell>
                <TableCell sortable column={"firstName"}>Jméno</TableCell>
            </HalEmbeddedTable>
        </div>
    </div>;
};
