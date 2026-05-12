import {type ReactElement, useCallback, useEffect, useMemo, useRef, useState} from "react";
import {useSearchParams} from "react-router-dom";
import {useAuthorizedQuery} from "../../hooks/useAuthorizedFetch.ts";
import type {EntityModel, HalFormsTemplate} from "../../api";
import type {Link} from "../../api";
import {TableCell} from "../../components/KlabisTable";
import type {TableCellRenderProps} from "../../components/KlabisTable/types.ts";
import {HalEmbeddedTable} from "../../components/HalNavigator2/HalEmbeddedTable.tsx";
import {HalFormButton} from "../../components/HalNavigator2/HalFormButton.tsx";
import {HalFormDisplay} from "../../components/HalNavigator2/HalFormDisplay.tsx";
import {toHref} from "../../api/hateoas.ts";
import {HalRouteProvider, useHalRoute} from "../../contexts/HalRouteContext.tsx";
import {formatDate, getRelevantDeadlineIndex} from "../../utils/dateUtils.ts";
import {normalizeKlabisApiPath} from "../../utils/halFormsUtils.ts";
import {getActionVariant} from "../../utils/actionVariants.ts";
import {useHalPageData} from "../../hooks/useHalPageData.ts";
import {labels, getEnumLabel, getDialogTitleLabel, getTemplateLabel} from "../../localization";
import {ImportOrisEventModal} from "../../components/events/ImportOrisEventModal.tsx";
import {BulkSyncOrisModal} from "../../components/events/BulkSyncOrisModal.tsx";
import {useOrisEventImport} from "../../hooks/useOrisEventImport.ts";
import {eventFormFieldsFactory} from "../../components/events/eventFormFieldsFactory.tsx";
import {Button, Modal} from "../../components/UI";
import {MemberName} from "../../components/members/MemberName.tsx";
import {ExternalLink, Globe, Pencil, RefreshCw, UserMinus, UserPlus, XCircle} from "lucide-react";
import {EventsFilterBar, type EventsFilterValue} from "../../components/events/EventsFilterBar.tsx";
import {EventTypeBadge} from "../../components/events/EventTypeBadge.tsx";
import {useAuth} from "../../contexts/AuthContext2.tsx";
import {useEventTypes} from "../../hooks/useEventTypes.ts";
import {
    DEFAULT_TIME_WINDOW,
    getDefaultSortForTimeWindow,
    getTimeWindowFromParams,
    getTodayIso,
    REGISTERED_BY_ME,
    timeWindowToDateParams,
    type TimeWindow,
} from "../../components/events/eventsFilterUtils.ts";

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

export const EventsPage = (): ReactElement => {
    const {route, resourceData} = useHalPageData();
    const {getUser} = useAuth();
    const {getById: getEventTypeById, eventTypes} = useEventTypes();
    const [isImportModalOpen, setIsImportModalOpen] = useState(false);
    const [isBulkSyncModalOpen, setIsBulkSyncModalOpen] = useState(false);
    const [actionModal, setActionModal] = useState<EventActionModalState | null>(null);
    const [newRegistrationState, setNewRegistrationState] = useState<{url: string; event: EventListData} | null>(null);
    const [searchParams, setSearchParams] = useSearchParams();

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
                        openActionModal(event, name);
                    }}>
                        <Icon className="w-4 h-4"/>
                    </Button>
                ))}
            </div>
        );
    };

    // Read filter state from URL (source of truth)
    const urlDateFrom = searchParams.get('dateFrom');
    const urlDateTo = searchParams.get('dateTo');
    const urlQ = searchParams.get('q') ?? '';
    const urlRegisteredByMe = searchParams.get('registeredBy') === REGISTERED_BY_ME;
    const urlEventTypeIds = searchParams.getAll('eventTypeId');

    const timeWindow: TimeWindow = getTimeWindowFromParams(urlDateFrom, urlDateTo);

    // On first mount: if no date params in URL, apply the Budoucí default
    const defaultAppliedRef = useRef(false);
    useEffect(() => {
        if (defaultAppliedRef.current) return;
        defaultAppliedRef.current = true;

        // Only apply default if neither dateFrom nor dateTo is present in URL
        if (!urlDateFrom && !urlDateTo) {
            const today = getTodayIso();
            const {dateFrom} = timeWindowToDateParams(DEFAULT_TIME_WINDOW, today);
            setSearchParams(
                (prev) => {
                    const next = new URLSearchParams(prev);
                    if (dateFrom) next.set('dateFrom', dateFrom);
                    return next;
                },
                {replace: true},
            );
        }
    }, []); // eslint-disable-line react-hooks/exhaustive-deps

    const filterValue: EventsFilterValue = useMemo(() => ({
        q: urlQ,
        timeWindow,
        registeredByMe: urlRegisteredByMe,
        eventTypeIds: urlEventTypeIds,
    // urlEventTypeIds is a new array on every render — compare its join to avoid infinite loops
     
    }), [urlQ, timeWindow, urlRegisteredByMe, urlEventTypeIds.join(',')]); // eslint-disable-line react-hooks/exhaustive-deps

    const handleFilterChange = useCallback((next: EventsFilterValue) => {
        const today = getTodayIso();
        const {dateFrom, dateTo} = timeWindowToDateParams(next.timeWindow, today);
        setSearchParams((prev) => {
            const params = new URLSearchParams(prev);
            if (dateFrom) { params.set('dateFrom', dateFrom); } else { params.delete('dateFrom'); }
            if (dateTo) { params.set('dateTo', dateTo); } else { params.delete('dateTo'); }
            if (next.q) { params.set('q', next.q); } else { params.delete('q'); }
            if (next.registeredByMe) { params.set('registeredBy', REGISTERED_BY_ME); } else { params.delete('registeredBy'); }
            params.delete('eventTypeId');
            next.eventTypeIds.forEach((id) => params.append('eventTypeId', id));
            return params;
        });
    }, [setSearchParams]);

    // Build extra params for the API call
    const extraParams = useMemo((): Record<string, string | string[]> => {
        const params: Record<string, string | string[]> = {};
        if (urlDateFrom) params.dateFrom = urlDateFrom;
        if (urlDateTo) params.dateTo = urlDateTo;
        if (urlQ && urlQ.length >= 2) params.q = urlQ;
        if (urlRegisteredByMe) params.registeredBy = REGISTERED_BY_ME;
        if (urlEventTypeIds.length > 0) params.eventTypeId = urlEventTypeIds;
        return params;
    // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [urlDateFrom, urlDateTo, urlQ, urlRegisteredByMe, urlEventTypeIds.join(',')]);

    const defaultSort = getDefaultSortForTimeWindow(timeWindow);

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
                    <HalFormButton name="createEvent" modal={true} label={labels.templates.createEvent} fieldsFactory={eventFormFieldsFactory}/>
                </div>
            </div>

            <EventsFilterBar
                value={filterValue}
                onChange={handleFilterChange}
                showRegisteredByMeToggle={showRegisteredByMeToggle}
                eventTypes={eventTypes}
            />

            {/* key={timeWindow} forces a remount when the time window changes, resetting internal sort state */}
            <HalEmbeddedTable<EventListData>
                key={timeWindow}
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
                               const relevantIndex = getRelevantDeadlineIndex(deadlines, getTodayIso());
                               const primary = deadlines[relevantIndex];
                               const otherCount = deadlines.length - 1;
                               const otherDates = deadlines.filter((_, i) => i !== relevantIndex).map(d => formatDate(d));
                               return (
                                   <span className="inline-flex items-center gap-1.5">
                                       <span>{formatDate(primary)}</span>
                                       {otherCount > 0 && (
                                           <span
                                               title={otherDates.join(', ')}
                                               className="inline-flex items-center justify-center w-5 h-5 rounded-full bg-primary text-white text-xs font-medium cursor-help"
                                           >
                                               +{otherCount}
                                           </span>
                                       )}
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
