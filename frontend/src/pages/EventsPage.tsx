import {type ReactElement} from "react";
import type {EntityModel} from "../api";
import {TableCell} from "../components/KlabisTable";
import {HalLinksSection} from "../components/HalNavigator2/HalLinksSection.tsx";
import {HalEmbeddedTable} from "../components/HalNavigator2/HalEmbeddedTable.tsx";
import {HalFormsSection} from "../components/HalNavigator2/HalFormsSection.tsx";
import {formatDate} from "../utils/dateUtils.ts";
import {useHalPageData} from "../hooks/useHalPageData.ts";

interface EventListData extends EntityModel<{
    id: string,  // UUID as string
    name: string,
    eventDate: string,  // ISO date string
    location: string,
    organizer: string,
    status: 'DRAFT' | 'ACTIVE' | 'FINISHED' | 'CANCELLED'
}> {
};

export const EventsPage = (): ReactElement => {
    const {route} = useHalPageData();

    return <div className="flex flex-col gap-8">
        <h1 className="text-3xl font-bold text-text-primary">Závody</h1>

        <div className="flex flex-col gap-4">
            <h2 className="text-xl font-bold text-text-primary">Seznam závodů</h2>
            <HalEmbeddedTable<EventListData> collectionName={"eventSummaryDtoList"} defaultOrderBy={"eventDate"}
                                             onRowClick={route.navigateToResource}>
                <TableCell sortable column={"eventDate"}
                           dataRender={({value}) => typeof value === 'string' ? formatDate(value) : ''}>Datum</TableCell>
                <TableCell sortable column={"name"}>Název</TableCell>
                <TableCell sortable column={"location"}>Místo</TableCell>
                <TableCell sortable column={"organizer"}>Pořadatel</TableCell>
                <TableCell sortable column={"status"}>Status</TableCell>
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