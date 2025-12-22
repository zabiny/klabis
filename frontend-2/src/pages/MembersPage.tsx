import {type ReactElement} from "react";
import {useHalRoute} from "../contexts/HalRouteContext";
import {HalNavigatorTable} from "../components/HalNavigator/halNavigatorTable.tsx";
import type {EntityModel, Link} from "../api";
import {TableCell} from "../components/KlabisTable";
import {useNavigate} from "react-router-dom";
import {toHref} from "../components/HalNavigator/hooks.ts";

export const MembersPage = (): ReactElement => {
    const {resourceData} = useHalRoute();
    const navigate = useNavigate();
    console.log(resourceData);
    return <HalNavigatorTable<EntityModel<{
        id: number,
        firstName: string,
        lastName: string,
        registrationNumber: string
    }>>
        embeddedName={'membersApiResponseList'}
        onRowClick={(item) => navigate(toHref(item._links.self as Link))}
        defaultOrderBy="lastName"
        defaultOrderDirection="asc"
    >
        <TableCell sortable column="firstName">Jméno</TableCell>
        <TableCell sortable column="lastName">Příjmení</TableCell>
        <TableCell sortable column="registrationNumber">Registrační číslo</TableCell>
        {/*<TableCell column="sex">Pohlaví</TableCell>*/}
        {/*<TableCell sortable column="dateOfBirth">Datum narození</TableCell>*/}
        {/*<TableCell column="nationality">Národnost</TableCell>*/}
        {/*<TableCell column="_links"*/}
        {/*           dataRender={props => (<HalLinksUi value={props.value}/>)}>Akce</TableCell>*/}
    </HalNavigatorTable>;
}