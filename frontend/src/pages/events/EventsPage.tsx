import {type ReactElement, useState} from "react";
import type {EntityModel} from "../../api";
import {TableCell} from "../../components/KlabisTable";
import {HalEmbeddedTable} from "../../components/HalNavigator2/HalEmbeddedTable.tsx";
import {HalFormButton} from "../../components/HalNavigator2/HalFormButton.tsx";
import {formatDate} from "../../utils/dateUtils.ts";
import {useHalPageData} from "../../hooks/useHalPageData.ts";
import {labels, getEnumLabel} from "../../localization";
import {ImportOrisEventModal} from "../../components/events/ImportOrisEventModal.tsx";
import {Button} from "../../components/UI/Button.tsx";

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
    const {route, resourceData} = useHalPageData();
    const [isImportModalOpen, setIsImportModalOpen] = useState(false);

    const importTemplate = resourceData?._templates?.importEvent;

    return <div className="flex flex-col gap-8">
        <h1 className="text-3xl font-bold text-text-primary">{labels.sections.events}</h1>

        <div className="flex flex-col gap-4">
            <div className="flex items-center justify-between">
                <h2 className="text-xl font-bold text-text-primary">{labels.sections.eventsList}</h2>
                <div className="flex items-center gap-2">
                    {importTemplate && (
                        <Button variant="secondary" onClick={() => setIsImportModalOpen(true)}>
                            {labels.templates.importEvent}
                        </Button>
                    )}
                    <HalFormButton name="createEvent" modal={true} label={labels.templates.createEvent}/>
                </div>
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

        {importTemplate && (
            <ImportOrisEventModal
                isOpen={isImportModalOpen}
                onClose={() => setIsImportModalOpen(false)}
                importHref={importTemplate.target!}
            />
        )}
    </div>;

}