import {type ReactElement, useState} from "react";
import {useAuthorizedQuery} from "../../hooks/useAuthorizedFetch.ts";
import type {EntityModel, HalFormsTemplate, Link} from "../../api";
import {TableCell} from "../../components/KlabisTable";
import type {TableCellRenderProps} from "../../components/KlabisTable/types.ts";
import {HalEmbeddedTable} from "../../components/HalNavigator2/HalEmbeddedTable.tsx";
import {HalFormDisplay} from "../../components/HalNavigator2/HalFormDisplay.tsx";
import {toHref} from "../../api/hateoas.ts";
import {HalRouteProvider, useHalRoute} from "../../contexts/HalRouteContext.tsx";
import {formatDate, getFutureDeadlines, getRelevantDeadlineIndex} from "../../utils/dateUtils.ts";
import {normalizeKlabisApiPath} from "../../utils/halFormsUtils.ts";
import {getActionVariant} from "../../utils/actionVariants.ts";
import {useHalPageData} from "../../hooks/useHalPageData.ts";
import {getDialogTitleLabel, getEnumLabel, getTemplateLabel, labels} from "../../localization";
import {ImportOrisEventModal} from "../../components/events/ImportOrisEventModal.tsx";
import {BulkSyncOrisModal} from "../../components/events/BulkSyncOrisModal.tsx";
import {useOrisEventImport} from "../../hooks/useOrisEventImport.ts";
import {eventFormFieldsFactory} from "../../components/events/eventFormFieldsFactory.tsx";
import {Button, DetailRow, Modal, Tooltip} from "../../components/UI";
import {HalFormButton} from "../../components/HalNavigator2/HalFormButton.tsx";
import {Section} from "../members/MemberSection.tsx";
import type {HalFormPanelRenderHelpers} from "../../components/HalNavigator2/HalFormPanel.tsx";
import {MemberName} from "../../components/members/MemberName.tsx";
import {ExternalLink, Globe, Pencil, RefreshCw, UserMinus, UserPlus, XCircle} from "lucide-react";
import {EventsFilterBar} from "../../components/events/EventsFilterBar.tsx";
import {EventTypeBadge} from "../../components/events/EventTypeBadge.tsx";
import {useAuth} from "../../contexts/AuthContext2.tsx";
import {useEventTypes} from "../../hooks/useEventTypes.ts";
import {getTodayIso} from "../../components/events/eventsFilterUtils.ts";
import {useEventsFilterState} from "./useEventsFilterState.ts";

type EventListData = EntityModel<{
    id: string,
    name: string,
    eventDate: string,
    location: string | null,
    organizer: string,
    websiteUrl?: string,
    deadlines?: string[],
    cancellationReason?: string,
    status?: 'DRAFT' | 'ACTIVE' | 'FINISHED' | 'CANCELLED',
    eventTypeId?: string | null,
}> & {
    _templates?: Record<string, HalFormsTemplate>;
}

interface EventActionModalState {
    event: EventListData;
    templateName: string;
    template: HalFormsTemplate;
}

const ROW_ACTION_BUTTONS = [
    {name: 'updateEvent', icon: Pencil, label: labels.templates.updateEvent},
    {name: 'publishEvent', icon: Globe, label: labels.templates.publishEvent},
    {name: 'cancelEvent', icon: XCircle, label: labels.templates.cancelEvent},
    {name: 'syncEventFromOris', icon: RefreshCw, label: labels.templates.syncEventFromOris},
    {name: 'registerForEvent', icon: UserPlus, label: labels.templates.registerForEvent},
    {name: 'unregisterFromEvent', icon: UserMinus, label: labels.templates.unregisterFromEvent},
];

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

const CREATE_FORM_BASIC_FIELDS = ['name', 'eventDate', 'location', 'organizer', 'websiteUrl'];
const CREATE_FORM_COORDINATION_FIELDS = ['eventCoordinatorId', 'eventTypeId'];

export const EventsPage = (): ReactElement => {
    const {route, resourceData} = useHalPageData();
    const {getUser} = useAuth();
    const {getById: getEventTypeById, eventTypes} = useEventTypes();
    const [isImportModalOpen, setIsImportModalOpen] = useState(false);
    const [isBulkSyncModalOpen, setIsBulkSyncModalOpen] = useState(false);
    const [actionModal, setActionModal] = useState<EventActionModalState | null>(null);
    const [newRegistrationState, setNewRegistrationState] = useState<{url: string; event: EventListData} | null>(null);
    const {filterValue, extraParams, defaultSort, handleFilterChange} = useEventsFilterState();

    const {data: newRegistrationData} = useAuthorizedQuery<Record<string, unknown>>(
        newRegistrationState?.url ?? '',
        {enabled: newRegistrationState !== null, staleTime: 0, gcTime: 0, retry: false},
    );

    const importTemplate = resourceData?._templates?.importEvent;
    const bulkSyncTemplate = resourceData?._templates?.syncAllUpcomingFromOris;
    const showRegisteredByMeToggle = Boolean(getUser()?.memberId);

    const orisImport = useOrisEventImport(
        importTemplate?.target ?? '',
        isImportModalOpen,
        {onImported: () => setIsImportModalOpen(false)},
    );

    const openActionModal = (event: EventListData, templateName: string) => {
        const template = event._templates?.[templateName];
        if (!template) return;
        setActionModal({event, templateName, template});
    };

    const renderActionsCell = ({item}: TableCellRenderProps) => {
        const event = item as unknown as EventListData;
        const templates = event._templates;
        const links = event._links as Record<string, {href: string}> | undefined;
        const newRegLink = links?.['newRegistration'];

        return (
            <div className="flex items-center gap-1" onClick={(e) => e.stopPropagation()}>
                {newRegLink && (
                    <Button key="newRegistration" variant={getActionVariant('newRegistration')} size="sm" title={labels.templates.registerForEvent} onClick={(e) => {
                        e.stopPropagation();
                        setNewRegistrationState({url: newRegLink.href, event});
                    }}>
                        <UserPlus className="w-4 h-4"/>
                    </Button>
                )}
                {ROW_ACTION_BUTTONS.filter(({name}) => name !== 'registerForEvent' || !newRegLink).map(({name, icon: Icon, label}) => templates?.[name] && (
                    <Button key={name} variant={getActionVariant(name)} size="sm" title={label} onClick={(e) => {
                        e.stopPropagation();
                        if (name === 'updateEvent') {
                            route.navigateToResource(event, {state: {editing: true}});
                        } else {
                            openActionModal(event, name);
                        }
                    }}>
                        <Icon className="w-4 h-4"/>
                    </Button>
                ))}
            </div>
        );
    };

    return <div className="flex flex-col gap-8">
        <h1 className="text-3xl font-bold text-text-primary">{labels.sections.events}</h1>

        <div className="flex flex-col gap-4">
            <div className="flex items-center justify-between">
                <h2 className="text-xl font-bold text-text-primary">{labels.sections.eventsList}</h2>
                <div className="flex items-center gap-2">
                    {bulkSyncTemplate && (
                        <Button variant="secondary" onClick={() => setIsBulkSyncModalOpen(true)}>
                            {labels.templates.syncAllUpcomingFromOris}
                        </Button>
                    )}
                    {importTemplate && (
                        <Button variant="secondary" onClick={() => setIsImportModalOpen(true)}>
                            {labels.templates.importEvent}
                        </Button>
                    )}
                    <HalFormButton name="createEvent" modal={false} fieldsFactory={eventFormFieldsFactory}>
                        {({renderInput, renderField, hasField}: HalFormPanelRenderHelpers) => {
                            const hasFields = (fieldNames: string[]) => fieldNames.some(f => hasField(f));
                            return (
                                <div className="flex flex-col gap-8">
                                    <div className="grid grid-cols-1 lg:grid-cols-2 gap-10">
                                        <div className="flex flex-col gap-6">
                                            {hasFields(CREATE_FORM_BASIC_FIELDS) && (
                                                <Section title={labels.sections.eventBasicInfo}>
                                                    {hasField('name') && <DetailRow label={labels.fields.name}>{renderInput('name')}</DetailRow>}
                                                    {hasField('eventDate') && <DetailRow label={labels.fields.eventDate}>{renderInput('eventDate')}</DetailRow>}
                                                    {hasField('location') && <DetailRow label={labels.fields.location}>{renderInput('location')}</DetailRow>}
                                                    {hasField('organizer') && <DetailRow label={labels.fields.organizer}>{renderInput('organizer')}</DetailRow>}
                                                    {hasField('websiteUrl') && <DetailRow label={labels.fields.websiteUrl}>{renderInput('websiteUrl')}</DetailRow>}
                                                </Section>
                                            )}
                                        </div>
                                        <div className="flex flex-col gap-6">
                                            {hasFields(CREATE_FORM_COORDINATION_FIELDS) && (
                                                <Section title={labels.sections.eventCoordination}>
                                                    {hasField('eventCoordinatorId') && (
                                                        <DetailRow label={labels.fields.eventCoordinatorId}>
                                                            {renderInput('eventCoordinatorId')}
                                                        </DetailRow>
                                                    )}
                                                    {hasField('eventTypeId') && (
                                                        <DetailRow label={labels.fields.eventTypeId}>
                                                            {renderInput('eventTypeId')}
                                                        </DetailRow>
                                                    )}
                                                </Section>
                                            )}
                                        </div>
                                    </div>
                                    {hasField('deadlines') && (
                                        <Section title={labels.sections.eventDeadlines}>
                                            {renderInput('deadlines')}
                                        </Section>
                                    )}
                                    {hasField('categories') && (
                                        <Section title={labels.sections.eventCategories}>
                                            {renderInput('categories')}
                                        </Section>
                                    )}
                                    <div className="flex flex-wrap justify-end gap-3 pt-4 border-t border-border">
                                        {renderField('cancel')}
                                        {renderField('submit')}
                                    </div>
                                </div>
                            );
                        }}
                    </HalFormButton>
                </div>
            </div>

            <EventsFilterBar
                value={filterValue}
                onChange={handleFilterChange}
                showRegisteredByMeToggle={showRegisteredByMeToggle}
                eventTypes={eventTypes}
            />

            <HalEmbeddedTable<EventListData>
                key={filterValue.selectedYear ?? filterValue.timeWindow}
                tableId={`events.${filterValue.timeWindow}`}
                collectionName={"eventSummaryDtoList"}
                defaultOrderBy={defaultSort.by}
                defaultOrderDirection={defaultSort.direction}
                extraParams={extraParams}
                onRowClick={route.navigateToResource}
                hideEmptyColumns={true}
                emptyMessage={labels.eventsFilter.emptyState}
            >
                <TableCell sortable column={"eventDate"}
                           dataRender={({value}) => typeof value === 'string' ? formatDate(value) : ''}>{labels.tables.date}</TableCell>
                <TableCell sortable column={"name"}>{labels.fields.name}</TableCell>
                <TableCell sortable column={"location"}
                           dataRender={({value}) => (value as string | null) ?? null}>{labels.fields.location}</TableCell>
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
                <TableCell alwaysVisible column={"deadlines"}
                           dataRender={({item}) => {
                               const event = item as unknown as EventListData;
                               const deadlines = event.deadlines;
                               if (!deadlines || deadlines.length === 0) return null;
                               const today = getTodayIso();
                               const relevantIndex = getRelevantDeadlineIndex(deadlines, today);
                               const primary = deadlines[relevantIndex];
                               const activeOrdinal = relevantIndex + 1;

                               const badge = deadlines.length > 1 && (
                                   <span className="inline-flex items-center justify-center px-1.5 h-5 rounded-full bg-primary text-white text-xs font-medium whitespace-nowrap">
                                       {labels.ui.deadlineOrdinalShort(activeOrdinal)}
                                   </span>
                               );

                               const futureDeadlines = getFutureDeadlines(deadlines, today)
                                   .filter(({ordinal}) => ordinal > activeOrdinal);

                               return (
                                   <span className="inline-flex items-center gap-1.5">
                                       <span>{formatDate(primary)}</span>
                                       {badge && (futureDeadlines.length > 0 ? (
                                           <Tooltip
                                               content={futureDeadlines
                                                   .map(({date, ordinal}) => `${labels.ui.deadlineOrdinal(ordinal)} ${formatDate(date)}`)
                                                   .join('\n')}
                                           >
                                               <span className="cursor-help">{badge}</span>
                                           </Tooltip>
                                       ) : badge)}
                                   </span>
                               );
                           }}>{labels.tables.registrationDeadline}</TableCell>
                <TableCell column={"_links"}
                           dataRender={({item}) => {
                               const links = item._links as Record<string, Link> | undefined;
                               const coordinatorLink = links?.coordinator;
                               if (!coordinatorLink) return null;
                               return <CoordinatorCellContent coordinatorLink={coordinatorLink}/>;
                           }}>{labels.tables.coordinator}</TableCell>
                <TableCell sortable column={"status"}
                           dataRender={({value, item}) => {
                               const label = typeof value === 'string' ? getEnumLabel('eventStatus', value) : '';
                               const event = item as unknown as EventListData;
                               if (event.status === 'CANCELLED' && event.cancellationReason) {
                                   return <span title={event.cancellationReason}>{label}</span>;
                               }
                               return label;
                           }}>{labels.tables.status}</TableCell>
                <TableCell column={"eventTypeId"}
                           dataRender={({item}) => {
                               const event = item as unknown as EventListData;
                               const eventType = getEventTypeById(event.eventTypeId);
                               if (!eventType) return null;
                               return <EventTypeBadge eventType={eventType}/>;
                           }}>{labels.tables.eventType}</TableCell>
                <TableCell column={"_actions"} dataRender={renderActionsCell}>{labels.tables.actions}</TableCell>
            </HalEmbeddedTable>
        </div>

        {importTemplate && (
            <ImportOrisEventModal
                isOpen={isImportModalOpen}
                onClose={() => setIsImportModalOpen(false)}
                {...orisImport}
            />
        )}

        {bulkSyncTemplate && (
            <BulkSyncOrisModal
                isOpen={isBulkSyncModalOpen}
                onClose={() => setIsBulkSyncModalOpen(false)}
                syncUrl={bulkSyncTemplate.target}
                onSyncComplete={route.refetch}
            />
        )}

        {actionModal && (
            <Modal isOpen={true} onClose={() => setActionModal(null)}
                   title={getDialogTitleLabel(actionModal.templateName) ?? getTemplateLabel(actionModal.templateName) ?? actionModal.template.title ?? actionModal.templateName} size="2xl">
                <HalFormDisplay
                    template={actionModal.template}
                    templateName={actionModal.templateName}
                    resourceData={actionModal.event as unknown as Record<string, unknown>}
                    pathname={route.pathname}
                    resourceUrl={actionModal.event._links?.self ? toHref(actionModal.event._links.self) : undefined}
                    onClose={() => setActionModal(null)}
                    fieldsFactory={actionModal.templateName === 'updateEvent' ? eventFormFieldsFactory : undefined}
                />
            </Modal>
        )}

        {(() => {
            if (!newRegistrationState || !newRegistrationData) return null;
            const eventTemplates = newRegistrationState.event._templates as Record<string, HalFormsTemplate> | undefined;
            const registerTemplate = eventTemplates?.registerForEvent;
            if (!registerTemplate) return null;
            // Submit must use the registerForEvent template (POST) from the event resource,
            // not editRegistration (PUT) from the prefill defaults — the registration does
            // not yet exist. resourceData carries the prefilled siCardNumber from defaults.
            const registerTemplatePath = registerTemplate.target
                ? normalizeKlabisApiPath(registerTemplate.target)
                : route.pathname;
            return (
                <Modal isOpen={true} onClose={() => setNewRegistrationState(null)}
                       title={labels.templates.registerForEvent} size="2xl">
                    <HalFormDisplay
                        template={registerTemplate}
                        templateName="registerForEvent"
                        resourceData={newRegistrationData}
                        pathname={registerTemplatePath}
                        onClose={() => setNewRegistrationState(null)}
                        navigateOnSuccess={false}
                    />
                </Modal>
            );
        })()}
    </div>;
}
