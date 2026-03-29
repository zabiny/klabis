import {type ReactElement, useState} from "react";
import type {EntityModel} from "../../api";
import type {Link} from "../../api";
import {TableCell} from "../../components/KlabisTable";
import {HalEmbeddedTable} from "../../components/HalNavigator2/HalEmbeddedTable.tsx";
import {HalFormButton} from "../../components/HalNavigator2/HalFormButton.tsx";
import {formatDate} from "../../utils/dateUtils.ts";
import {useHalPageData} from "../../hooks/useHalPageData.ts";
import {labels, getEnumLabel} from "../../localization";
import {ImportOrisEventModal} from "../../components/events/ImportOrisEventModal.tsx";
import {Button} from "../../components/UI/Button.tsx";
import {HalRouteProvider, useHalRoute} from "../../contexts/HalRouteContext.tsx";
import {MemberName} from "../../components/members/MemberName.tsx";
import {ExternalLink} from "lucide-react";

interface EventListData extends EntityModel<{
    id: string,
    name: string,
    eventDate: string,
    location: string,
    organizer: string,
    websiteUrl?: string,
    registrationDeadline?: string,
    status?: 'DRAFT' | 'ACTIVE' | 'FINISHED' | 'CANCELLED'
}> {
}

interface CoordinatorCellProps {
    coordinatorLink: Link;
}

const CoordinatorCellContent = ({coordinatorLink}: CoordinatorCellProps): ReactElement => {
    const {navigateToResource} = useHalRoute();

    return (
        <HalRouteProvider routeLink={coordinatorLink}>
            <CoordinatorName onNavigate={() => navigateToResource(coordinatorLink)}/>
        </HalRouteProvider>
    );
};

const CoordinatorName = ({onNavigate}: { onNavigate: () => void }): ReactElement => {
    const {resourceData} = useHalRoute();

    if (!resourceData) {
        return <span/>;
    }

    return (
        <button
            onClick={(e) => {
                e.stopPropagation();
                onNavigate();
            }}
            className="text-primary hover:text-primary-light hover:underline text-left"
        >
            <MemberName/>
        </button>
    );
};

interface EventRowActionsProps {
    selfLink: Link;
}

const EventRowActions = ({selfLink}: EventRowActionsProps): ReactElement => {
    return (
        <HalRouteProvider routeLink={selfLink}>
            <EventRowActionButtons/>
        </HalRouteProvider>
    );
};

const EventRowActionButtons = (): ReactElement => {
    return (
        <div className="flex gap-1" onClick={(e) => e.stopPropagation()}>
            <HalFormButton name="registerForEvent" modal={true}/>
            <HalFormButton name="unregisterFromEvent" modal={true} variant="danger"/>
        </div>
    );
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
            <HalEmbeddedTable<EventListData>
                collectionName={"eventSummaryDtoList"}
                defaultOrderBy={"eventDate"}
                onRowClick={route.navigateToResource}
                hideEmptyColumns={true}
            >
                <TableCell sortable column={"eventDate"}
                           dataRender={({value}) => typeof value === 'string' ? formatDate(value) : ''}>{labels.tables.date}</TableCell>
                <TableCell sortable column={"name"}>{labels.fields.name}</TableCell>
                <TableCell sortable column={"location"}>{labels.fields.location}</TableCell>
                <TableCell sortable column={"organizer"}>{labels.fields.organizer}</TableCell>
                <TableCell column={"websiteUrl"}
                           dataRender={({value}) => value ? (
                               <a
                                   href={value as string}
                                   target="_blank"
                                   rel="noopener noreferrer"
                                   onClick={(e) => e.stopPropagation()}
                                   className="inline-flex text-primary hover:text-primary-light"
                                   title={value as string}
                               >
                                   <ExternalLink className="w-4 h-4"/>
                               </a>
                           ) : null}>{labels.tables.web}</TableCell>
                <TableCell sortable column={"registrationDeadline"}
                           dataRender={({value}) => typeof value === 'string' ? formatDate(value) : ''}>{labels.tables.registrationDeadline}</TableCell>
                <TableCell column={"_links"}
                           dataRender={({item}) => {
                               const links = item._links as Record<string, Link> | undefined;
                               const coordinatorLink = links?.coordinator;
                               if (!coordinatorLink) return null;
                               return <CoordinatorCellContent coordinatorLink={coordinatorLink}/>;
                           }}>{labels.tables.coordinator}</TableCell>
                <TableCell sortable column={"status"}
                           dataRender={({value}) => typeof value === 'string' ? getEnumLabel('eventStatus', value) : ''}>{labels.tables.status}</TableCell>
                <TableCell column={"actions"}
                           dataRender={({item}) => {
                               const links = item._links as Record<string, Link> | undefined;
                               const selfLink = links?.self;
                               if (!selfLink) return null;
                               return <EventRowActions selfLink={selfLink}/>;
                           }}>{labels.tables.actions}</TableCell>
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
