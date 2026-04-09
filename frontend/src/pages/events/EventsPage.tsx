import {type ReactElement, useState} from "react";
import type {EntityModel, HalFormsTemplate} from "../../api";
import type {Link} from "../../api";
import {TableCell} from "../../components/KlabisTable";
import type {TableCellRenderProps} from "../../components/KlabisTable/types.ts";
import {HalEmbeddedTable} from "../../components/HalNavigator2/HalEmbeddedTable.tsx";
import {HalFormButton} from "../../components/HalNavigator2/HalFormButton.tsx";
import {HalFormDisplay} from "../../components/HalNavigator2/HalFormDisplay.tsx";
import {HalRouteProvider, useHalRoute} from "../../contexts/HalRouteContext.tsx";
import {formatDate} from "../../utils/dateUtils.ts";
import {useHalPageData} from "../../hooks/useHalPageData.ts";
import {labels, getEnumLabel} from "../../localization";
import {ImportOrisEventModal} from "../../components/events/ImportOrisEventModal.tsx";
import {Button, Modal} from "../../components/UI";
import {MemberName} from "../../components/members/MemberName.tsx";
import {ExternalLink, Globe, Pencil, RefreshCw, UserMinus, UserPlus, XCircle} from "lucide-react";

type EventListData = EntityModel<{
    id: string,
    name: string,
    eventDate: string,
    location: string | null,
    organizer: string,
    websiteUrl?: string,
    registrationDeadline?: string,
    status?: 'DRAFT' | 'ACTIVE' | 'FINISHED' | 'CANCELLED'
}> & {
    _templates?: Record<string, HalFormsTemplate>;
}

interface EventActionModalState {
    event: EventListData;
    templateName: string;
    template: HalFormsTemplate;
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

export const EventsPage = (): ReactElement => {
    const {route, resourceData} = useHalPageData();
    const [isImportModalOpen, setIsImportModalOpen] = useState(false);
    const [actionModal, setActionModal] = useState<EventActionModalState | null>(null);

    const importTemplate = resourceData?._templates?.importEvent;

    const openActionModal = (event: EventListData, templateName: string) => {
        const template = event._templates?.[templateName];
        if (!template) return;
        setActionModal({event, templateName, template});
    };

    const renderActionsCell = ({item}: TableCellRenderProps) => {
        const event = item as unknown as EventListData;
        const t = event._templates ?? {};

        return (
            <div className="flex items-center gap-1" onClick={(e) => e.stopPropagation()}>
                {t.updateEvent && (
                    <Button variant="ghost" size="sm" title={labels.templates.updateEvent} onClick={(e) => {
                        e.stopPropagation();
                        openActionModal(event, 'updateEvent');
                    }}>
                        <Pencil className="w-4 h-4"/>
                    </Button>
                )}
                {t.publishEvent && (
                    <Button variant="ghost" size="sm" title={labels.templates.publishEvent} onClick={(e) => {
                        e.stopPropagation();
                        openActionModal(event, 'publishEvent');
                    }}>
                        <Globe className="w-4 h-4"/>
                    </Button>
                )}
                {t.cancelEvent && (
                    <Button variant="ghost" size="sm" title={labels.templates.cancelEvent} onClick={(e) => {
                        e.stopPropagation();
                        openActionModal(event, 'cancelEvent');
                    }}>
                        <XCircle className="w-4 h-4"/>
                    </Button>
                )}
                {t.syncEventFromOris && (
                    <Button variant="ghost" size="sm" title={labels.templates.syncEventFromOris} onClick={(e) => {
                        e.stopPropagation();
                        openActionModal(event, 'syncEventFromOris');
                    }}>
                        <RefreshCw className="w-4 h-4"/>
                    </Button>
                )}
                {t.registerForEvent && (
                    <Button variant="ghost" size="sm" onClick={(e) => {
                        e.stopPropagation();
                        openActionModal(event, 'registerForEvent');
                    }}>
                        <UserPlus className="w-4 h-4"/>
                    </Button>
                )}
                {t.unregisterFromEvent && (
                    <Button variant="ghost" size="sm" className="text-red-600" onClick={(e) => {
                        e.stopPropagation();
                        openActionModal(event, 'unregisterFromEvent');
                    }}>
                        <UserMinus className="w-4 h-4"/>
                    </Button>
                )}
            </div>
        );
    };

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
                <TableCell sortable column={"location"}
                           dataRender={({value}) => value ? <span>{value as string}</span> : null}>{labels.fields.location}</TableCell>
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
                <TableCell sortable alwaysVisible column={"registrationDeadline"}
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
                <TableCell column={"_actions"} dataRender={renderActionsCell}>{labels.tables.actions}</TableCell>
            </HalEmbeddedTable>
        </div>

        {importTemplate && (
            <ImportOrisEventModal
                isOpen={isImportModalOpen}
                onClose={() => setIsImportModalOpen(false)}
                importHref={importTemplate.target!}
            />
        )}

        {actionModal && (
            <Modal isOpen={true} onClose={() => setActionModal(null)}
                   title={actionModal.template.title ?? actionModal.templateName} size="2xl">
                <HalFormDisplay
                    template={actionModal.template}
                    templateName={actionModal.templateName}
                    resourceData={actionModal.event as unknown as Record<string, unknown>}
                    pathname={route.pathname}
                    onClose={() => setActionModal(null)}
                />
            </Modal>
        )}
    </div>;
}
