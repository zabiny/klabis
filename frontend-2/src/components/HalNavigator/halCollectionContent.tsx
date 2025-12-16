import type {EntityModel, HalCollectionResponse, HalFormsTemplate, NavigationTarget} from "../../api";
import type {Navigation} from "../../hooks/useNavigation";
import React, {type ReactElement, type ReactNode} from "react";
import {toURLPath, useHalExplorerNavigation, useResponseBody} from "./hooks";
import {Box, Button, Link as MuiLink, Typography} from "@mui/material";
import {HalActionsUi, HalLinksUi} from "./halActionComponents";
import {HalNavigatorTable} from "./halNavigatorTable";
import {TableCell} from "../KlabisTable";
import EventType from "../events/EventType";
import {Public} from "@mui/icons-material";
import MemberName from "../members/MemberName";

function dropMetadataAttributes<T extends { _links?: any }>(obj: T): Omit<T, '_links'> {
    const {_links, ...rest} = obj;
    return rest;
}

const formatDate = (dateString: string) => {
    const date = new Date(dateString);
    return new Intl.DateTimeFormat('cs-CZ').format(date);
};

export function HalCollectionContent({navigation}: {
    data: HalCollectionResponse,
    navigation: Navigation<NavigationTarget>
}): ReactElement {

    const data = useResponseBody();


    const resourceUrlPath = toURLPath(navigation.current);

    const navigateToEntityModel = (item: EntityModel<unknown>): void => {
        const self = item._links?.self as unknown;
        const link = Array.isArray(self) ? self[0] : self;
        if (link) {
            navigation.navigate(link as NavigationTarget);
        } else {
            alert(`Missing "self" link in entity model ${JSON.stringify(item)}`)
        }
    }

    const matchesUriPath = (regex: RegExp) => regex.test(resourceUrlPath);

    const equalsUriPath = (value: string) => {
        return value === resourceUrlPath;
    }

    switch (true) {
        case equalsUriPath('/members'):
            return (
                <HalNavigatorCollectionContentLayout includePageNavigation={false} label={"Adresář"}>
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
                </HalNavigatorCollectionContentLayout>
            );
        case equalsUriPath('/events'):
            return (<HalNavigatorCollectionContentLayout label={"Závody"} includePageNavigation={false}>
                <HalNavigatorTable<EntityModel<{ date: string, name: string, id: number, location: string }>>
                    embeddedName={'eventResponseList'} defaultOrderBy={"date"}
                    defaultOrderDirection={'desc'}
                    onRowClick={navigateToEntityModel}>
                    <TableCell sortable column={"date"}
                               dataRender={({value}) => typeof value === 'string' ? formatDate(value) : ''}>Datum</TableCell>
                    <TableCell sortable column={"name"}>Název</TableCell>
                    <TableCell sortable column={"location"}>Místo</TableCell>
                    <TableCell sortable column={"organizer"}>Pořadatel</TableCell>
                    <TableCell column={"type"}
                               dataRender={({value}) => <EventType eventType={value as any}/>}>Typ</TableCell>
                    <TableCell column={"web"}
                               dataRender={({value}) => <MuiLink hidden={!value as any}
                                                                 href={typeof value === 'string' ? value : undefined}><Public/></MuiLink>}>Web</TableCell>
                    <TableCell sortable column={"registrationDeadline"}
                               dataRender={({value}) => typeof value === 'string' ? formatDate(value) : ''}>Uzávěrka
                        přihlášek</TableCell>
                    <TableCell column={"coordinator"} dataRender={({value}) => typeof value === 'number' ?
                        <MemberName memberId={value}/> : <>--</>}>Vedoucí</TableCell>
                </HalNavigatorTable>
            </HalNavigatorCollectionContentLayout>);
        case matchesUriPath(/^\/calendar.*/):
            return <HalNavigatorCollectionContentLayout>
                <HalNavigatorTable<EntityModel<{ id: number, start: string, end: string, note: string }>>
                    embeddedName={'calendarItems'} defaultOrderBy={"start"} defaultOrderDirection={'desc'}
                    onRowClick={navigateToEntityModel}>
                    <TableCell column={"start"}
                               dataRender={({value}) => typeof value === 'string' ? formatDate(value) : ''}>Datum</TableCell>
                    <TableCell column={"end"}
                               dataRender={({value}) => typeof value === 'string' ? formatDate(value) : ''}>Datum</TableCell>
                    <TableCell column={"note"}>Poznámka</TableCell>
                </HalNavigatorTable>
            </HalNavigatorCollectionContentLayout>;
        case matchesUriPath(/^\/member\/\d+\/finance-account\/transactions$/):
            return <HalNavigatorCollectionContentLayout>
                <HalNavigatorTable<EntityModel<{ date: string, amount: number, note: string }>>
                    embeddedName={'transactionItemResponseList'} defaultOrderBy={"date"} defaultOrderDirection={'desc'}>
                    <TableCell column={"date"}
                               dataRender={({value}) => typeof value === 'string' ? formatDate(value) : ''}>Datum</TableCell>
                    <TableCell column={"amount"}>Název</TableCell>
                    <TableCell column={"note"}>Poznámka</TableCell>
                </HalNavigatorTable>
            </HalNavigatorCollectionContentLayout>;
        default:
            return (<HalNavigatorCollectionContentLayout>
                {data?._embedded && Object.entries(data._embedded).map(([rel, items]) => <GenericHalCollectionContent
                    label={rel} items={items}/>) || "Zadne polozky"}
            </HalNavigatorCollectionContentLayout>);
    }


}

type NavigatorCollectionLayoutProps = React.PropsWithChildren<{ label?: string, includePageNavigation?: boolean }>;

const HalNavigatorCollectionContentLayout = ({
                                                 label,
                                                 includePageNavigation = true,
                                                 children
                                             }: NavigatorCollectionLayoutProps): ReactElement => {

    const data = useResponseBody();
    const navigation = useHalExplorerNavigation();

    return (<Box>
        {label && <Typography variant="h4" component="h1" gutterBottom>{label}</Typography>}

        <HalLinksUi links={data?._links || {}} onClick={navigation.navigate}
                    showPagingNavigation={includePageNavigation}/>

        {children}

        <HalActionsUi links={(data?._templates || {}) as Record<string, HalFormsTemplate>}
                      onClick={link => navigation.navigate(link)}/>

    </Box>);

}


const GenericHalCollectionContent = ({label, items}: {
    label: string,
    items: Record<string, unknown>
}): ReactNode => {
    const navigation = useHalExplorerNavigation();

    return (<div key={label}>
            <ul className="list-disc list-inside">
                {(Array.isArray(items) ? items : [items]).map((item, idx) => (
                    <li key={idx}>
                        {JSON.stringify(dropMetadataAttributes(item))}
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