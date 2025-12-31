import {type ReactElement} from "react";
import type {EntityModel} from "../api";
import {TableCell} from "../components/KlabisTable";
import {HalLinksSection} from "../components/HalNavigator2/HalLinksSection.tsx";
import {HalEmbeddedTable} from "../components/HalNavigator2/HalEmbeddedTable.tsx";
import {HalFormsSection} from "../components/HalNavigator2/HalFormsSection.tsx";
import {useHalPageData} from "../hooks/useHalPageData.ts";

interface MemberListData extends EntityModel<{
    id: number,
    firstName: string,
    lastName: string,
    registrationNumber: string
}> {
};

export const MembersPage = (): ReactElement => {
    const {route} = useHalPageData();

    return <div className="flex flex-col gap-8">
        <h1 className="text-3xl font-bold text-text-primary">Adresář</h1>

        <div className="flex flex-col gap-4">
            <h2 className="text-xl font-bold text-text-primary">Členové</h2>
            <HalEmbeddedTable<MemberListData> collectionName={"membersApiResponseList"} defaultOrderBy={"lastName"}
                                              onRowClick={route.navigateToResource}>
                <TableCell sortable column="firstName">Jméno</TableCell>
                <TableCell sortable column="lastName">Příjmení</TableCell>
                <TableCell sortable column="registrationNumber">Registrační číslo</TableCell>
                <TableCell column="_links"
                           dataRender={props => (
                               <HalLinksSection links={props.value as any}/>)}>Akce</TableCell>
            </HalEmbeddedTable>
        </div>

        <div className="flex flex-col gap-4">
            <HalLinksSection/>
        </div>
        <div className="flex flex-col gap-4">
            <HalFormsSection/>
        </div>
    </div>;

}