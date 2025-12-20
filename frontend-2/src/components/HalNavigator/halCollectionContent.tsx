import type {EntityModel, HalFormsTemplate, NavigationTarget} from "../../api";
import React, {type ReactElement, type ReactNode} from "react";
import {toURLPath, useHalExplorerNavigation, useResponseBody} from "./hooks";
import {Button} from "../UI";
import {HalActionsUi, HalLinksUi} from "./halActionComponents";
import {HalNavigatorTable} from "./halNavigatorTable";
import {TableCell} from "../KlabisTable";
import EventType from "../events/EventType";
import MemberName from "../members/MemberName";

function dropMetadataAttributes<T extends { _links?: any }>(obj: T): Omit<T, '_links'> {
    const {_links, ...rest} = obj;
    return rest;
}

const formatDate = (dateString: string) => {
    const date = new Date(dateString);
    return new Intl.DateTimeFormat('cs-CZ').format(date);
};

export function HalCollectionContent(): ReactElement {

    const navigation = useHalExplorerNavigation();
    const responseData = useResponseBody();


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
                               dataRender={({value}) => !value ? null : (
                                   <a href={typeof value === 'string' ? value : undefined}
                                      target="_blank"
                                      rel="noopener noreferrer"
                                      className="text-blue-600 dark:text-blue-400 hover:underline"
                                      aria-label="External link">
                                       <svg className="w-5 h-5 inline" fill="currentColor" viewBox="0 0 20 20">
                                           <path
                                               d="M11 3a1 1 0 100 2h3.586L9.293 9.293a1 1 0 101.414 1.414L16 6.414V10a1 1 0 102 0V4a1 1 0 00-1-1h-6z"/>
                                           <path
                                               d="M5 5a2 2 0 00-2 2v8a2 2 0 002 2h8a2 2 0 002-2v-3a1 1 0 10-2 0v3H5V7h3a1 1 0 000-2H5z"/>
                                       </svg>
                                   </a>
                               )}>Web</TableCell>
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
                {responseData?._embedded && Object.entries(responseData._embedded).map(([rel, items]) =>
                    <GenericHalCollectionContent
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

    return (<div>
        {label && <h1 className="text-2xl font-bold mb-4 text-gray-900 dark:text-white">{label}</h1>}

        <HalLinksUi links={data?._links || {}} onClick={navigation.navigate}
                    showPagingNavigation={includePageNavigation}/>

        {children}

        <HalActionsUi links={(data?._templates || {}) as Record<string, HalFormsTemplate>}
                      onClick={link => navigation.navigate(link)}/>

    </div>);

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
                                variant="secondary"
                                size="sm"
                                className="ml-2"
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