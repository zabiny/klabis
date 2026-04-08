import {type ReactElement, type ReactNode, useMemo, useState} from 'react';
import {Link} from 'react-router-dom';
import {useHalPageData} from '../../hooks/useHalPageData.ts';
import {Alert, Badge, Button, Card, DetailRow, Skeleton} from '../../components/UI';
import {HalFormButton} from '../../components/HalNavigator2/HalFormButton.tsx';
import {HalFormDisplay} from '../../components/HalNavigator2/HalFormDisplay.tsx';
import {type FormRenderHelpers} from '../../components/HalNavigator2/halforms';
import {HalEmbeddedTable} from '../../components/HalNavigator2/HalEmbeddedTable.tsx';
import {HalSubresourceProvider, useHalRoute} from '../../contexts/HalRouteContext.tsx';
import {TableCell} from '../../components/KlabisTable';
import {formatDate, formatDateTime} from '../../utils/dateUtils.ts';
import type {EntityModel} from '../../api';
import type {HalFormsProperty, HalFormsTemplate, HalResponse} from '../../api';
import {labels, getEnumLabel} from '../../localization';
import {Check, CheckCircle, ExternalLink, Globe, Pencil, RefreshCw, UserMinus, UserPlus, XCircle} from 'lucide-react';
import {MemberName} from '../../components/members/MemberName.tsx';

interface EventDetail {
    name: string;
    eventDate: string;
    location?: string;
    organizer?: string;
    websiteUrl?: string;
    registrationDeadline?: string;
    eventCoordinatorId?: {value: string};
    status?: string;
    categories?: string[];
    [key: string]: unknown;
}

interface RegistrationData extends EntityModel<{
    firstName: string;
    lastName: string;
    registeredAt: string;
    category?: string;
}> {}

const STATUS_VARIANT: Record<string, 'default' | 'primary' | 'success' | 'warning' | 'error' | 'info'> = {
    DRAFT: 'default',
    ACTIVE: 'success',
    FINISHED: 'info',
    CANCELLED: 'error',
};

function enrichTemplateWithReadOnlyFields(
    template: HalFormsTemplate,
    resourceData: Record<string, unknown>
): HalFormsTemplate {
    const templateFieldNames = new Set(template.properties.map(p => p.name));

    const readOnlyProps: HalFormsProperty[] = Object.keys(resourceData)
        .filter(key => !templateFieldNames.has(key) && !key.startsWith('_'))
        .filter(key => {
            const value = resourceData[key];
            return value === null || value === undefined || typeof value !== 'object';
        })
        .map(key => ({
            name: key,
            type: 'text',
            readOnly: true,
        }));

    return {
        ...template,
        properties: [...template.properties, ...readOnlyProps],
    };
}

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
        return <Alert severity="error">{error.message}</Alert>;
    }

    if (!resourceData) {
        return <Skeleton/>;
    }

    return <EventDetailContent resourceData={resourceData}/>;
};

interface EventDetailContentProps {
    resourceData: EventDetail & HalResponse;
}

const EventDetailContent = ({resourceData}: EventDetailContentProps): ReactElement => {
    const {route} = useHalPageData<EventDetail>();
    const [isEditing, setIsEditing] = useState(false);

    const event = resourceData;
    const statusVariant = event.status ? (STATUS_VARIANT[event.status] ?? 'default') : 'default';

    const template: HalFormsTemplate | null = resourceData?._templates?.updateEvent ?? null;
    const hasEditTemplate = template !== null;

    const enrichedTemplate = useMemo(() => {
        if (!isEditing || !template) return null;
        return enrichTemplateWithReadOnlyFields(template, resourceData as Record<string, unknown>);
    }, [isEditing, template, resourceData]);

    const enrichedFieldNames = useMemo(() =>
        enrichedTemplate
            ? new Set(enrichedTemplate.properties.map(p => p.name))
            : new Set<string>(),
        [enrichedTemplate]);

    const originalEditableFieldNames = useMemo(() =>
        template ? new Set(template.properties.map(p => p.name)) : new Set<string>(),
        [template]);

    const startEditing = () => setIsEditing(true);
    const cancelEditing = () => setIsEditing(false);

    const postprocessPayload = (payload: Record<string, unknown>): Record<string, unknown> =>
        Object.fromEntries(
            Object.entries(payload).filter(([key]) => originalEditableFieldNames.has(key))
        );

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
                    <div className="flex items-center gap-4">
                        <h1 className="text-3xl font-bold text-text-primary">{event.name}</h1>
                        {!isEditing && (
                            <Badge variant={statusVariant} size="sm">
                                {event.status ? getEnumLabel('eventStatus', event.status) : event.status}
                            </Badge>
                        )}
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
                            <HalFormButton name="finishEvent" modal={true} icon={<CheckCircle className="w-4 h-4"/>}/>
                            <HalFormButton name="syncEventFromOris" modal={true} icon={<RefreshCw className="w-4 h-4"/>}/>
                            <HalFormButton name="registerForEvent" modal={true} icon={<UserPlus className="w-4 h-4"/>}/>
                            <HalFormButton name="unregisterFromEvent" modal={true} icon={<UserMinus className="w-4 h-4"/>}/>
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
                        {(isEditing || event.registrationDeadline) && (
                            <DetailRow label={labels.fields.registrationDeadline}>
                                {ri('registrationDeadline') ?? (event.registrationDeadline ? formatDate(event.registrationDeadline) : null)}
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

                <div className="flex flex-col gap-4">
                    <h2 className="text-xl font-bold text-text-primary">{labels.sections.registrations}</h2>
                    <HalEmbeddedTable<RegistrationData> collectionName="registrationDtoList">
                        <TableCell column="firstName">{labels.fields.firstName}</TableCell>
                        <TableCell column="lastName">{labels.fields.lastName}</TableCell>
                        {event.categories && event.categories.length > 0 && (
                            <TableCell column="category">{labels.fields.categories}</TableCell>
                        )}
                        <TableCell column="registeredAt" dataRender={({value}) => formatDateTime(value as string)}>{labels.tables.registeredAt}</TableCell>
                    </HalEmbeddedTable>
                </div>
            </div>
        );
    };

    if (isEditing && enrichedTemplate) {
        return (
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
            />
        );
    }

    return renderContent() as ReactElement;
};
