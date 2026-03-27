import {type ReactElement} from "react";
import type {EntityModel} from "../../api";
import {TableCell} from "../../components/KlabisTable";
import {HalEmbeddedTable} from "../../components/HalNavigator2/HalEmbeddedTable.tsx";
import {HalFormButton} from "../../components/HalNavigator2/HalFormButton.tsx";
import {formatDate} from "../../utils/dateUtils.ts";
import {useHalPageData} from "../../hooks/useHalPageData.ts";
import {labels, getEnumLabel} from "../../localization";

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
        <h1 className="text-3xl font-bold text-text-primary">{labels.sections.events}</h1>

        <div className="flex flex-col gap-4">
            <div className="flex items-center justify-between">
                <h2 className="text-xl font-bold text-text-primary">{labels.sections.eventsList}</h2>
                <HalFormButton name="createEvent" modal={true} label={labels.templates.createEvent}/>
            </div>
            <HalEmbeddedTable<EventListData> collectionName={"eventSummaryDtoList"} defaultOrderBy={"eventDate"}
                                             onRowClick={route.navigateToResource}>
                <TableCell sortable column={"eventDate"}
                           dataRender={({value}) => typeof value === 'string' ? formatDate(value) : ''}>{labels.tables.date}</TableCell>
                <TableCell sortable column={"name"}>{labels.fields.name}</TableCell>
                <TableCell sortable column={"location"}>{labels.fields.location}</TableCell>
                <TableCell sortable column={"organizer"}>{labels.fields.organizer}</TableCell>
                <TableCell sortable column={"status"}
                           dataRender={({value}) => typeof value === 'string' ? getEnumLabel('eventStatus', value) : ''}>{labels.tables.status}</TableCell>
            </HalEmbeddedTable>
        </div>
    </div>;

}