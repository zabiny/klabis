import {type ReactElement, type ReactNode, useState} from 'react';
import {Link, useLocation} from 'react-router-dom';
import {useHalPageData} from '../../hooks/useHalPageData.ts';
import {Badge, Button, Card, DetailRow, Modal, Skeleton} from '../../components/UI';
import {ErrorPage} from '../ErrorPage.tsx';
import {HalFormButton} from '../../components/HalNavigator2/HalFormButton.tsx';
import {HalFormDisplay} from '../../components/HalNavigator2/HalFormDisplay.tsx';
import {type FormRenderHelpers} from '../../components/HalNavigator2/halforms';
import {HalEmbeddedTable} from '../../components/HalNavigator2/HalEmbeddedTable.tsx';
import {HalSubresourceProvider} from '../../contexts/HalRouteContext.tsx';
import {useHalRoute} from '../../contexts/halRouteContext.ts';
import {TableCell} from '../../components/KlabisTable';
import {formatDate, formatDateTime, getRelevantDeadlineIndex, getTodayIso} from '../../utils/dateUtils.ts';
import type {EntityModel, Link as HalLink} from '../../api';
import type {HalFormsTemplate, HalResponse} from '../../api';
import {toHref} from '../../api/hateoas.ts';
import {useInlineEditing} from '../../hooks/useInlineEditing.ts';
import {labels, getEnumLabel} from '../../localization';
import {EventTypeBadge} from '../../components/events/EventTypeBadge.tsx';
import {useEventTypes} from '../../hooks/useEventTypes.ts';
import {AlertTriangle, Banknote, Check, ExternalLink, Globe, List, Pencil, RefreshCw, UserMinus, UserPlus, XCircle} from 'lucide-react';
import {MemberName} from '../../components/members/MemberName.tsx';
import {eventFormFieldsFactory} from '../../components/events/eventFormFieldsFactory.tsx';
import type {TableCellRenderProps} from '../../components/KlabisTable/types.ts';
import {FinanceTransactionDialog} from '../../components/finance/FinanceTransactionDialog.tsx';

interface EventDetail {
    name: string;
    eventDate: string;
    location?: string | null;
    organizer?: string;
    websiteUrl?: string;
    deadlines?: string[];
    cancellationReason?: string;
    eventCoordinatorId?: {value: string};
    eventTypeId?: string | null;
    status?: string;
    categories?: string[];
    [key: string]: unknown;
}

interface RegistrationData extends EntityModel<{
    firstName: string;
    lastName: string;
    registrationTime?: string;
    category?: string;
    [key: string]: unknown;
}> {
    _templates?: Record<string, HalFormsTemplate>;
}

interface RegistrationEditModalState {
    item: RegistrationData;
    template: HalFormsTemplate;
}

const STATUS_VARIANT: Record<string, 'default' | 'primary' | 'success' | 'warning' | 'error' | 'info'> = {
    DRAFT: 'default',
    ACTIVE: 'success',
    FINISHED: 'info',
    CANCELLED: 'error',
};

const CoordinatorDisplay = (): ReactElement => {
    const {resourceData, navigateToResource} = useHalRoute();
    return (
        <span className="inline-flex items-center gap-1.5">
            <MemberName/>
            {resourceData && (
                <button
                    onClick={() => navigateToResource(resourceData)}
                    className="text-primary hover:text-primary-light"
                    title={labels.ui.showMemberDetails}
                >
                    <ExternalLink className="w-4 h-4"/>
                </button>
            )}
        </span>
    );
};

export const EventDetailPage = (): ReactElement => {
    const {resourceData, isLoading, error} = useHalPageData<EventDetail>();

    if (isLoading) {
        return <Skeleton/>;
    }

    if (error) {
        return <ErrorPage error={error}/>;
    }

    if (!resourceData) {
        return <Skeleton/>;
    }

    return <EventDetailContent resourceData={resourceData}/>;
};

interface RegistrationsTableProps {
    event: EventDetail;
    onOpenEditModal: (state: RegistrationEditModalState) => void;
    onOpenTransactionDialog: (accountLink: HalLink) => void;
}

const RegistrationsTable = ({event, onOpenEditModal, onOpenTransactionDialog}: RegistrationsTableProps): ReactElement => {
    const renderActionsCell = ({item}: TableCellRenderProps) => {
        const registration = item as unknown as RegistrationData;
        const editTemplate = registration._templates?.editRegistration;
        const recordTransactionLink = registration._links?.recordTransaction;
        const accountLink = recordTransactionLink && !Array.isArray(recordTransactionLink)
            ? recordTransactionLink as HalLink
            : Array.isArray(recordTransactionLink) ? recordTransactionLink[0] as HalLink : null;

        const hasAnyAction = editTemplate || accountLink;
        if (!hasAnyAction) return null;

        return (
            <div className="flex items-center gap-1">
                {accountLink && (
                    <Button
                        variant="ghost"
                        size="sm"
                        aria-label={labels.finance.openTransactionDialogAriaLabel}
                        className="text-primary"
                        onClick={(e) => {
                            e.stopPropagation();
                            onOpenTransactionDialog(accountLink);
                        }}
                    >
                        <Banknote className="w-4 h-4"/>
                    </Button>
                )}
                {editTemplate && (
                    <Button
                        variant="ghost"
                        size="sm"
                        aria-label={labels.templates.editRegistration}
                        onClick={(e) => {
                            e.stopPropagation();
                            onOpenEditModal({item: registration, template: editTemplate});
                        }}
                    >
                        <Pencil className="w-4 h-4"/>
                    </Button>
                )}
            </div>
        );
    };

    return (
        <HalEmbeddedTable<RegistrationData> collectionName="registrationDtoList" hideEmptyColumns defaultOrderBy="registrationTime">
            <TableCell column="firstName" sortable>{labels.fields.firstName}</TableCell>
            <TableCell column="lastName" sortable>{labels.fields.lastName}</TableCell>
            {event.categories && event.categories.length > 0 && (
                <TableCell column="category" sortable>{labels.fields.categories}</TableCell>
            )}
            <TableCell column="registrationTime" sortable dataRender={({value}) => formatDateTime(value as string)}>{labels.tables.registeredAt}</TableCell>
            <TableCell column="_actions" dataRender={renderActionsCell} alwaysVisible>{labels.tables.actions}</TableCell>
        </HalEmbeddedTable>
    );
};

interface EventDetailContentProps {
    resourceData: EventDetail & HalResponse;
}

const EventDetailContent = ({resourceData}: EventDetailContentProps): ReactElement => {
    const {route} = useHalPageData<EventDetail>();
    const {getById: getEventTypeById} = useEventTypes();
    const location = useLocation();
    const initialEditing = !!(location.state as { editing?: boolean })?.editing;
    const [registrationEditModal, setRegistrationEditModal] = useState<RegistrationEditModalState | null>(null);
    const [transactionDialogAccount, setTransactionDialogAccount] = useState<HalLink | null>(null);

    const event = resourceData;
    const statusVariant = event.status ? (STATUS_VARIANT[event.status] ?? 'default') : 'default';

    const template: HalFormsTemplate | null = resourceData?._templates?.updateEvent ?? null;
    const hasEditTemplate = template !== null;

    const {isEditing, enrichedTemplate, enrichedFieldNames, startEditing, cancelEditing, postprocessPayload} =
        useInlineEditing(template, resourceData as Record<string, unknown>, {initialEditing});

    const renderContent = (helpers?: FormRenderHelpers) => {
        const ri = (name: string): ReactNode =>
            isEditing && enrichedFieldNames.has(name) && helpers
                ? helpers.renderInput(name)
                : null;

        return (
            <div className="flex flex-col gap-8">
                <div>
                    <Link to="/events" className="text-sm text-primary hover:text-primary-light">
                        {labels.ui.backToList}
                    </Link>
                </div>

                <div className="flex flex-col gap-4 sm:flex-row sm:items-start sm:justify-between">
                    <div className="flex items-center gap-4 flex-wrap">
                        <h1 className="text-3xl font-bold text-text-primary">{event.name}</h1>
                        {!isEditing && (
                            <Badge variant={statusVariant} size="sm">
                                {event.status ? getEnumLabel('eventStatus', event.status) : event.status}
                            </Badge>
                        )}
                        {!isEditing && event.eventTypeId && (() => {
                            const eventType = getEventTypeById(event.eventTypeId);
                            return eventType ? <EventTypeBadge eventType={eventType}/> : null;
                        })()}
                    </div>

                    {!isEditing && (
                        <div className="flex flex-wrap gap-3 sm:flex-shrink-0">
                            {hasEditTemplate && (
                                <Button
                                    variant="primary"
                                    onClick={startEditing}
                                    startIcon={<Pencil className="w-4 h-4"/>}
                                >
                                    {labels.templates.updateEvent}
                                </Button>
                            )}
                            <HalFormButton name="publishEvent" modal={true} icon={<Globe className="w-4 h-4"/>}/>
                            <HalFormButton name="cancelEvent" modal={true} icon={<XCircle className="w-4 h-4"/>}/>
                            <HalFormButton name="syncEventFromOris" modal={true} icon={<RefreshCw className="w-4 h-4"/>}/>
                            <HalFormButton name="registerForEvent" modal={true} navigateOnSuccess={false} icon={<UserPlus className="w-4 h-4"/>}/>
                            <HalFormButton name="unregisterFromEvent" modal={true} icon={<UserMinus className="w-4 h-4"/>}/>
                            <HalFormButton name="editRegistration" modal={true} icon={<Pencil className="w-4 h-4"/>}/>
                            {resourceData._links?.['accommodation-list'] && (
                                <Link
                                    to={`${route.pathname}/accommodation-list`}
                                    className="inline-flex items-center gap-2 px-3 py-1.5 text-sm font-medium rounded-md border border-border text-text-primary hover:bg-bg-secondary"
                                >
                                    <List className="w-4 h-4"/>
                                    {labels.buttons.accommodationList}
                                </Link>
                            )}
                        </div>
                    )}
                </div>

                <hr className="border-border"/>

                <Card className="p-6">
                    <h3 className="text-xs uppercase font-semibold text-text-secondary mb-4">{labels.sections.eventInfo}</h3>
                    <dl>
                        <DetailRow label={labels.fields.eventDate}>{ri('eventDate') ?? formatDate(event.eventDate)}</DetailRow>
                        {(isEditing || event.location) && (
                            <DetailRow label={labels.fields.location}>{ri('location') ?? event.location}</DetailRow>
                        )}
                        {(isEditing || event.organizer) && (
                            <DetailRow label={labels.fields.organizer}>{ri('organizer') ?? event.organizer}</DetailRow>
                        )}
                        {(isEditing || event.websiteUrl) && (
                            <DetailRow label={labels.fields.websiteUrl}>
                                {ri('websiteUrl') ?? (event.websiteUrl ? (
                                    <a
                                        href={event.websiteUrl}
                                        target="_blank"
                                        rel="noopener noreferrer"
                                        className="text-primary hover:text-primary-light underline"
                                    >
                                        {event.websiteUrl}
                                    </a>
                                ) : null)}
                            </DetailRow>
                        )}
                        {isEditing && (
                            <DetailRow label={labels.fields.deadlines}>
                                {ri('deadlines')}
                            </DetailRow>
                        )}
                        {(isEditing || (event.categories && event.categories.length > 0)) && (
                            <DetailRow label={labels.fields.categories}>
                                {ri('categories') ?? (
                                    <div className="flex flex-wrap gap-1.5">
                                        {event.categories?.map(category => (
                                            <Badge key={category} variant="info" size="sm">{category}</Badge>
                                        ))}
                                    </div>
                                )}
                            </DetailRow>
                        )}
                        {isEditing && (
                            <DetailRow label={labels.fields.eventTypeId}>
                                {ri('eventTypeId')}
                            </DetailRow>
                        )}
                        {isEditing && (
                            <DetailRow label={labels.fields.eventCoordinatorId}>
                                {ri('eventCoordinatorId')}
                            </DetailRow>
                        )}
                        {!isEditing && resourceData._links?.coordinator && (
                            <DetailRow label={labels.fields.eventCoordinatorId}>
                                <HalSubresourceProvider subresourceLinkName="coordinator">
                                    <CoordinatorDisplay/>
                                </HalSubresourceProvider>
                            </DetailRow>
                        )}
                    </dl>
                </Card>

                {isEditing && (
                    <div className="flex justify-end gap-3 pt-4 border-t border-border">
                        <Button variant="secondary" onClick={cancelEditing}>
                            {labels.buttons.cancel}
                        </Button>
                        {helpers?.renderField('submit')}
                    </div>
                )}

                {!isEditing && event.status === 'CANCELLED' && (
                    <Card className="p-6 border-error">
                        <h3 className="text-xs uppercase font-semibold text-error mb-4 flex items-center gap-2">
                            <AlertTriangle className="w-4 h-4"/>
                            {labels.sections.eventCancelled}
                        </h3>
                        {event.cancellationReason && (
                            <p className="text-text-primary">{event.cancellationReason}</p>
                        )}
                    </Card>
                )}

                {!isEditing && event.deadlines && event.deadlines.length > 0 && (
                    <Card className="p-6">
                        <h3 className="text-xs uppercase font-semibold text-text-secondary mb-4">{labels.sections.deadlines}</h3>
                        <ul className="space-y-1">
                            {(() => {
                                const relevantIndex = getRelevantDeadlineIndex(event.deadlines!, getTodayIso());
                                return event.deadlines!.map((deadline, index) => {
                                    const isRelevant = index === relevantIndex;
                                    return (
                                        <li key={deadline} className="flex items-center gap-2 text-text-primary">
                                            <span className="text-text-secondary">{labels.ui.deadlineOrdinal(index + 1)}</span>
                                            <span className={isRelevant ? 'font-bold text-primary' : undefined}>{formatDate(deadline)}</span>
                                            {isRelevant && (
                                                <span className="text-xs font-medium text-primary">{labels.ui.currentDeadline}</span>
                                            )}
                                        </li>
                                    );
                                });
                            })()}
                        </ul>
                    </Card>
                )}

                {!isEditing && resourceData._links?.registrations && (
                    <div className="flex flex-col gap-4">
                        <h2 className="text-xl font-bold text-text-primary">{labels.sections.registrations}</h2>
                        <HalSubresourceProvider subresourceLinkName="registrations">
                            <RegistrationsTable
                                event={event}
                                onOpenEditModal={setRegistrationEditModal}
                                onOpenTransactionDialog={setTransactionDialogAccount}
                            />
                        </HalSubresourceProvider>
                    </div>
                )}
            </div>
        );
    };

    const handleRegistrationEditClose = () => setRegistrationEditModal(null);

    const financeTransactionDialogJsx = transactionDialogAccount && (
        <FinanceTransactionDialog
            accountLink={transactionDialogAccount}
            isOpen={true}
            onClose={() => setTransactionDialogAccount(null)}
            defaultNote={event.name}
        />
    );

    const registrationEditModalJsx = registrationEditModal && (
        <Modal
            isOpen={true}
            onClose={handleRegistrationEditClose}
            title={labels.dialogTitles.editRegistration}
            size="md"
        >
            <HalFormDisplay
                template={registrationEditModal.template}
                templateName="edit"
                resourceData={registrationEditModal.item as unknown as Record<string, unknown>}
                pathname={route.pathname}
                resourceUrl={registrationEditModal.item._links?.self ? toHref(registrationEditModal.item._links.self) : undefined}
                onClose={handleRegistrationEditClose}
                navigateOnSuccess={false}
            />
        </Modal>
    );

    if (isEditing && enrichedTemplate) {
        return (
            <>
                <HalFormDisplay
                    template={enrichedTemplate}
                    templateName="updateEvent"
                    resourceData={resourceData as Record<string, unknown>}
                    pathname={route.pathname}
                    onClose={cancelEditing}
                    postprocessPayload={postprocessPayload}
                    successMessage={labels.ui.savedSuccessfully}
                    submitButtonLabel={labels.buttons.saveChanges}
                    submitIcon={<Check className="w-4 h-4"/>}
                    customLayout={renderContent}
                    fieldsFactory={eventFormFieldsFactory}
                />
                {registrationEditModalJsx}
                {financeTransactionDialogJsx}
            </>
        );
    }

    return (
        <>
            {renderContent() as ReactElement}
            {registrationEditModalJsx}
            {financeTransactionDialogJsx}
        </>
    );
};
