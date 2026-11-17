import {type ReactElement, useState} from "react";
import type {HalResponse, Link, ListEventsItemResource} from "../../api";
import {TableCell} from "../../components/KlabisTable";
import type {TableCellRenderProps} from "../../components/KlabisTable/types.ts";
import {HalEmbeddedTable} from "../../components/HalNavigator2/HalEmbeddedTable.tsx";
import {useTemplateDialog} from "../../components/HalNavigator2/useTemplateDialog.tsx";
import {halResource} from "../../api/halResource";
import {asLinkArray, toHref} from "../../api/hateoas.ts";
import {useAuthorizedQuery} from "../../hooks/useAuthorizedFetch.ts";
import {HalRouteProvider} from "../../contexts/HalRouteContext.tsx";
import {useHalRoute} from "../../contexts/halRouteContext.ts";
import {formatDate, getFutureDeadlines, getRelevantDeadlineIndex} from "../../utils/dateUtils.ts";
import {getActionVariant} from "../../utils/actionVariants.ts";
import {EventRegistrationDialog} from "../../components/events/EventRegistrationDialog.tsx";
import {useHalPageData} from "../../hooks/useHalPageData.ts";
import {getEnumLabel, labels} from "../../localization";
import {ImportOrisEventsButton} from "../../components/events/ImportOrisEventsButton.tsx";
import {BulkSyncOrisModal} from "../../components/events/BulkSyncOrisModal.tsx";
import {eventFormFieldsFactory} from "../../components/events/eventFormFieldsFactory.tsx";
import {Button, DetailRow, Tooltip} from "../../components/UI";
import {HalFormButton} from "../../components/HalNavigator2/HalFormButton.tsx";
import {Section} from "../members/MemberSection.tsx";
import type {HalFormPanelRenderHelpers} from "../../components/HalNavigator2/HalFormPanel.tsx";
import {MemberName} from "../../components/members/MemberName.tsx";
import {ExternalLink, Globe, Pencil, UserMinus, UserPen, UserPlus, XCircle} from "lucide-react";
import {type CoordinatorOption, EventsFilterBar} from "../../components/events/EventsFilterBar.tsx";
import {EventSiPolicyFields} from "../../components/events/EventSiPolicyFields.tsx";
import {EventTypeBadge} from "../../components/events/EventTypeBadge.tsx";
import {SyncStatusIndicator} from "../../components/sync/SyncStatusIndicator.tsx";
import {useAuth} from "../../contexts/authContext";
import {useEventTypes} from "../../hooks/useEventTypes.ts";
import {getTodayIso} from "../../components/events/eventsFilterUtils.ts";
import {useEventsFilterState} from "./useEventsFilterState.ts";
import {HalPageGate} from "../HalPageGate.tsx";

type EventListData = ListEventsItemResource;

const ROW_ACTION_BUTTONS = [
    {name: 'updateEvent', icon: Pencil, label: labels.templates.updateEvent},
    {name: 'publishEvent', icon: Globe, label: labels.templates.publishEvent},
    {name: 'cancelEvent', icon: XCircle, label: labels.templates.cancelEvent},
    {name: 'registerForEvent', icon: UserPlus, label: labels.templates.registerForEvent},
    {name: 'unregisterFromEvent', icon: UserMinus, label: labels.templates.unregisterFromEvent},
];

interface CoordinatorCellProps {
    coordinatorLinks: Link[];
}

const CoordinatorCellContent = ({coordinatorLinks}: CoordinatorCellProps): ReactElement => {
    const {navigateToResource} = useHalRoute();
    const firstLink = coordinatorLinks[0];
    const extraCount = coordinatorLinks.length - 1;

    return (
        <span className="inline-flex items-center gap-1.5">
            <HalRouteProvider routeLink={firstLink}>
                <CoordinatorName onNavigate={() => navigateToResource(firstLink)}/>
            </HalRouteProvider>
            {extraCount > 0 && (
                <span className="inline-flex items-center justify-center px-1.5 h-5 rounded-full bg-primary text-white text-xs font-medium whitespace-nowrap">
                    +{extraCount}
                </span>
            )}
        </span>
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
const CREATE_FORM_COORDINATION_FIELDS = ['coordinators', 'eventTypeId'];
const CREATE_FORM_SI_POLICY_FIELDS = ['siUsage', 'siRentalAllowed'];
const CREATE_FORM_SHARED_SERVICE_FIELDS = ['sharedTransportEnabled', 'sharedAccommodationEnabled'];

const EventsPageContent = ({resourceData}: {resourceData: HalResponse}): ReactElement => {
    const {navigateToResource, refetch} = useHalPageData();
    const {getUser} = useAuth();
    const {getById: getEventTypeById, eventTypes} = useEventTypes();
    const [isBulkSyncModalOpen, setIsBulkSyncModalOpen] = useState(false);
    const actionDialog = useTemplateDialog({size: '2xl'});
    const [registrationTarget, setRegistrationTarget] = useState<Link | null>(null);
    const {filterValue, extraParams, defaultSort, handleFilterChange} = useEventsFilterState();
    const coordinatorOptionsLink = resourceData._links?.coordinatorOptions;
    const {data: coordinatorOptions} = useAuthorizedQuery<CoordinatorOption[]>(
        coordinatorOptionsLink ? toHref(coordinatorOptionsLink) : undefined,
        {staleTime: 5 * 60 * 1000},
    );

    const bulkSyncTemplate = resourceData._templates?.syncAllUpcomingFromOris;
    // `status` is returned to everyone; `_templates.createEvent` is present exactly for EVENTS:MANAGE.
    const canManageEvents = Boolean(resourceData._templates?.createEvent);
    const showRegisteredByMeToggle = Boolean(getUser()?.memberId);

    const renderActionsCell = ({item: event}: TableCellRenderProps<EventListData>) => {
        const actions = halResource(event);
        const newRegistrationAction = actions.link('newRegistration');
        const editRegistrationTarget = actions.template('editRegistration')?.template.target;
        const syncAction = actions.link('sync');

        return (
            <div className="flex items-center gap-1" onClick={(e) => e.stopPropagation()}>
                {newRegistrationAction && (
                    <Button key="newRegistration" variant={getActionVariant('newRegistration')} size="sm" title={newRegistrationAction.label} onClick={(e) => {
                        e.stopPropagation();
                        setRegistrationTarget(newRegistrationAction.link);
                    }}>
                        <UserPlus className="w-4 h-4"/>
                    </Button>
                )}
                {editRegistrationTarget && (
                    <Button key="editRegistration" variant={getActionVariant('editRegistration')} size="sm" title={labels.templates.editRegistration} onClick={(e) => {
                        e.stopPropagation();
                        setRegistrationTarget({href: editRegistrationTarget});
                    }}>
                        <UserPen className="w-4 h-4"/>
                    </Button>
                )}
                {ROW_ACTION_BUTTONS.filter(({name}) => name !== 'registerForEvent' || !newRegistrationAction).map(({name, icon: Icon, label}) => {
                    const action = actions.template(name);
                    return action && (
                        <Button key={name} variant={getActionVariant(name)} size="sm" title={label} onClick={(e) => {
                            e.stopPropagation();
                            if (name === 'updateEvent') {
                                navigateToResource(event, {state: {editing: true}});
                            } else {
                                actionDialog.open(action);
                            }
                        }}>
                            <Icon className="w-4 h-4"/>
                        </Button>
                    );
                })}
                {syncAction && <SyncStatusIndicator syncLink={syncAction.link} mode="icon"/>}
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
                    <ImportOrisEventsButton resource={resourceData}/>
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
                                                    {hasField('coordinators') && (
                                                        <DetailRow label={labels.fields.coordinators}>
                                                            {renderInput('coordinators')}
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
                                    {hasField('memberInfo') && (
                                        <Section title={labels.sections.memberInfo}>
                                            <DetailRow label={labels.fields.memberInfo}>{renderInput('memberInfo')}</DetailRow>
                                        </Section>
                                    )}
                                    {hasField('categories') && (
                                        <Section title={labels.sections.eventCategories}>
                                            {renderInput('categories')}
                                        </Section>
                                    )}
                                    {hasFields(CREATE_FORM_SI_POLICY_FIELDS) && (
                                        <Section title={labels.sections.siPolicy}>
                                            <EventSiPolicyFields renderInput={renderInput} hasField={hasField}/>
                                        </Section>
                                    )}
                                    {hasFields(CREATE_FORM_SHARED_SERVICE_FIELDS) && (
                                        <Section title={labels.sections.sharedServices}>
                                            {hasField('sharedTransportEnabled') && (
                                                <DetailRow label={labels.fields.sharedTransportEnabled}>
                                                    {renderInput('sharedTransportEnabled')}
                                                </DetailRow>
                                            )}
                                            {hasField('sharedAccommodationEnabled') && (
                                                <DetailRow label={labels.fields.sharedAccommodationEnabled}>
                                                    {renderInput('sharedAccommodationEnabled')}
                                                </DetailRow>
                                            )}
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
                coordinatorOptions={Array.isArray(coordinatorOptions) ? coordinatorOptions : undefined}
            />

            <HalEmbeddedTable<EventListData>
                tableId={`events.${filterValue.timeWindow}`}
                defaultOrderBy={defaultSort.by}
                defaultOrderDirection={defaultSort.direction}
                extraParams={extraParams}
                onRowClick={navigateToResource}
                hideEmptyColumns={true}
                emptyMessage={labels.eventsFilter.emptyState}
            >
                <TableCell sortable column={"eventDate"}
                           dataRender={({value}) => typeof value === 'string' ? formatDate(value) : ''}>{labels.tables.date}</TableCell>
                <TableCell<EventListData> sortable column={"name"}
                           dataRender={({value, item: event}) => {
                               if (event.status !== 'CANCELLED') return value as string;
                               // Non-managers see no status column, so the state is repeated for screen readers.
                               return (
                                   <span className="line-through opacity-60">
                                       {value as string}
                                       <span className="sr-only"> — {getEnumLabel('eventStatus', 'CANCELLED')}</span>
                                   </span>
                               );
                           }}>{labels.fields.name}</TableCell>
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
                <TableCell<EventListData> alwaysVisible column={"deadlines"}
                           dataRender={({item: event}) => {
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
                <TableCell<EventListData> column={"_links"}
                           dataRender={({item}) => {
                               const coordinatorLinks = asLinkArray(item._links?.coordinator);
                               if (coordinatorLinks.length === 0) return null;
                               return <CoordinatorCellContent coordinatorLinks={coordinatorLinks}/>;
                           }}>{labels.tables.coordinator}</TableCell>
                <TableCell sortable column={"status"}
                           hidden={!canManageEvents}
                           dataRender={({value}) => typeof value === 'string' ? getEnumLabel('eventStatus', value) : ''}>{labels.tables.status}</TableCell>
                <TableCell<EventListData> column={"eventTypeId"}
                           dataRender={({item: event}) => {
                               const eventType = getEventTypeById(event.eventTypeId);
                               if (!eventType) return null;
                               return <EventTypeBadge eventType={eventType}/>;
                           }}>{labels.tables.eventType}</TableCell>
                <TableCell column={"_actions"} dataRender={renderActionsCell}>{labels.tables.actions}</TableCell>
            </HalEmbeddedTable>
        </div>

        {bulkSyncTemplate && (
            <BulkSyncOrisModal
                isOpen={isBulkSyncModalOpen}
                onClose={() => setIsBulkSyncModalOpen(false)}
                syncUrl={bulkSyncTemplate.target}
                onSyncComplete={refetch}
            />
        )}

        {actionDialog.dialog}

        <EventRegistrationDialog
            registration={registrationTarget}
            onClose={() => setRegistrationTarget(null)}
            onChanged={refetch}
        />
    </div>;
}

export const EventsPage = (): ReactElement => (
    <HalPageGate>
        {(resourceData) => <EventsPageContent resourceData={resourceData}/>}
    </HalPageGate>
);
