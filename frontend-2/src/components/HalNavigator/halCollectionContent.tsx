import type {EntityModel, HalResponse, NavigationTarget, PageMetadata} from "../../api";
import type {Navigation} from "../../hooks/useNavigation";
import React, {ReactElement, ReactNode} from "react";
import {toURLPath, useHalExplorerNavigation, useResponseBody} from "./hooks";
import {Box, Button, Link as MuiLink, Typography} from "@mui/material";
import {HalActionsUi, HalLinksUi} from "./halActionComponents";
import {HalNavigatorTable} from "./halNavigatorTable";
import {TableCell} from "../KlabisTable";
import EventType from "../events/EventType";
import {Public} from "@mui/icons-material";
import MemberName from "../members/MemberName";

function omitMetadataAttributes<T extends { _links?: any }>(obj: T): Omit<T, '_links'> {
    const {_links, ...rest} = obj;
    return rest;
}

const formatDate = (dateString: string) => {
    const date = new Date(dateString);
    return new Intl.DateTimeFormat('cs-CZ').format(date);
};

export function HalCollectionContent({navigation}: {
    data: HalResponse,
    navigation: Navigation<NavigationTarget>
}): ReactElement {

    const data = useResponseBody();

    const renderCollectionContent = (relName: string, items: Record<string, unknown>[], paging?: PageMetadata): ReactNode => {

        const resourceUrlPath = toURLPath(navigation.current);

        const navigateToEntityModel = (item: EntityModel<unknown>): void => {
            if (item._links.self) {
                navigation.navigate(item._links.self);
            } else {
                alert(`Missing "self" link in entity model ${JSON.stringify(item)}`)
            }
        }

        switch (resourceUrlPath) {
            case '/members':
                return (<Box>
                        <Typography variant="h4" component="h1" gutterBottom>
                            Adresář
                        </Typography>

                        <HalLinksUi links={data._links} onClick={navigation.navigate} showPagingNavigation={false}/>

                        <HalNavigatorTable<EntityModel<{
                            id: number,
                            firstName: string,
                            lastName: string,
                            registrationNumber: string
                        }>>
                            embeddedName={'membersApiResponseList'}
                            onRowClick={navigateToEntityModel}
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
                        </HalNavigatorTable>
                    </Box>
                );
            case '/events':
                return (<Box>
                    <Typography variant="h4" component="h1" gutterBottom>
                        Závody
                    </Typography>

                    <HalLinksUi links={data._links} onClick={navigation.navigate} showPagingNavigation={false}/>

                    <HalNavigatorTable<EntityModel<{ date: string, name: string, id: number, location: string }>>
                        embeddedName={'eventResponseList'} defaultOrderBy={"date"}
                        defaultOrderDirection={'desc'}
                        onRowClick={navigateToEntityModel}>
                        <TableCell sortable column={"date"}
                                   dataRender={({value}) => formatDate(value)}>Datum</TableCell>
                        <TableCell sortable column={"name"}>Název</TableCell>
                        <TableCell sortable column={"location"}>Místo</TableCell>
                        <TableCell sortable column={"organizer"}>Pořadatel</TableCell>
                        <TableCell column={"type"}
                                   dataRender={({value}) => <EventType eventType={value}/>}>Typ</TableCell>
                        <TableCell column={"web"}
                                   dataRender={({value}) => <MuiLink hidden={!value}
                                                                     href={value}><Public/></MuiLink>}>Web</TableCell>
                        <TableCell sortable column={"registrationDeadline"} dataRender={({value}) => formatDate(value)}>Uzávěrka
                            přihlášek</TableCell>
                        <TableCell column={"coordinator"} dataRender={({value}) => value ?
                            <MemberName memberId={value}/> : <>--</>}>Vedoucí</TableCell>
                    </HalNavigatorTable>
                </Box>);
            default:
                return (<GenericHalCollectionContent label={relName} items={items}/>);
        }
    }

    return (
        <>
            {data?._embedded && Object.entries(data._embedded).map(([rel, items]) => renderCollectionContent(rel, items, data?.page))}

            {data?._templates && <HalActionsUi links={data._templates} onClick={link => navigation.navigate(link)}/>}

        </>)

}

const GenericHalCollectionContent = ({label, items}: { label: string, items: Record<string, unknown> }): ReactNode => {
    const navigation = useHalExplorerNavigation();
    const responseBody = useResponseBody();

    return (<div key={label}>
            <Typography variant="h4" component="h1" gutterBottom>{label}</Typography>

            <HalLinksUi links={responseBody._links} onClick={navigation.navigate} showPagingNavigation={true}/>

            <ul className="list-disc list-inside">
                {(Array.isArray(items) ? items : [items]).map((item, idx) => (
                    <li key={idx}>
                        {JSON.stringify(omitMetadataAttributes(item))}
                        {item._links?.self && (
                            <Button
                                className="ml-2 px-2 py-0.5 text-sm bg-gray-300 rounded"
                                onClick={() => navigation.navigate(item._links.self)}
                            >
                                Open
                            </Button>
                        )}
                    </li>
                ))}
            </ul>
        </div>
    );
}