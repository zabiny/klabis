import {type ReactElement} from "react";
import type {EntityModel} from "../api";
import {TableCell} from "../components/KlabisTable";
import {HalLinksSection} from "../components/HalNavigator2/HalLinksSection.tsx";
import {HalEmbeddedTable} from "../components/HalNavigator2/HalEmbeddedTable.tsx";
import {HalFormsSection} from "../components/HalNavigator2/HalFormsSection.tsx";

interface MemberListData extends EntityModel<{
    id: number,
    firstName: string,
    lastName: string,
    registrationNumber: string
}> {
};

export const MembersPage = (): ReactElement => {
    return <div>
        <h1>Adresář</h1>

        <HalEmbeddedTable<MemberListData> collectionName={"membersApiResponseList"} defaultOrderBy={"lastName"}>
            <TableCell sortable column="firstName">Jméno</TableCell>
            <TableCell sortable column="lastName">Příjmení</TableCell>
            <TableCell sortable column="registrationNumber">Registrační číslo</TableCell>
            <TableCell column="_links"
                       dataRender={props => (
                           <HalLinksSection links={props.value as any}/>)}>Akce</TableCell>
        </HalEmbeddedTable>

        <HalLinksSection/>
        <HalFormsSection/>
    </div>;

}