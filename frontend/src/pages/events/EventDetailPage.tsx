import {type ReactElement} from 'react';
import {Link} from 'react-router-dom';
import {useHalPageData} from '../../hooks/useHalPageData.ts';
import {DetailRow, Skeleton} from '../../components/UI';
import {Badge} from '../../components/UI/Badge';
import {HalFormButton} from '../../components/HalNavigator2/HalFormButton.tsx';
import {HalEmbeddedTable} from '../../components/HalNavigator2/HalEmbeddedTable.tsx';
import {TableCell} from '../../components/KlabisTable';
import {formatDate} from '../../utils/dateUtils.ts';
import type {EntityModel} from '../../api';

interface EventDetail {
    name: string;
    eventDate: string;
    location?: string;
    organizer?: string;
    websiteUrl?: string;
    eventCoordinatorId?: {value: string};
    status?: string;
    [key: string]: unknown;
}

interface RegistrationData extends EntityModel<{
    firstName: string;
    lastName: string;
    registeredAt: string;
}> {}

const STATUS_VARIANT: Record<string, 'default' | 'primary' | 'success' | 'warning' | 'error' | 'info'> = {
    DRAFT: 'default',
    ACTIVE: 'success',
    FINISHED: 'info',
    CANCELLED: 'error',
};

export const EventDetailPage = (): ReactElement => {
    const {resourceData, isLoading, error} = useHalPageData<EventDetail>();

    if (isLoading) {
        return <Skeleton/>;
    }

    if (error) {
        return <div className="text-error">{error.message}</div>;
    }

    if (!resourceData) {
        return <Skeleton/>;
    }

    const event = resourceData;
    const statusVariant = event.status ? (STATUS_VARIANT[event.status] ?? 'default') : 'default';

    return (
        <div className="flex flex-col gap-8">
            <div>
                <Link to="/events" className="text-sm text-primary hover:text-primary-light">
                    &larr; Zpět na seznam
                </Link>
            </div>

            <div className="flex flex-col gap-4 sm:flex-row sm:items-start sm:justify-between">
                <div className="flex items-center gap-4">
                    <h1 className="text-3xl font-bold text-text-primary">{event.name}</h1>
                </div>

                <div className="flex flex-wrap gap-3 sm:flex-shrink-0">
                    <HalFormButton name="default" modal={true} label="Upravit"/>
                    <HalFormButton name="publishEvent" modal={true}/>
                    <HalFormButton name="cancelEvent" modal={true}/>
                    <HalFormButton name="finishEvent" modal={true}/>
                    <HalFormButton name="registerForEvent" modal={true}/>
                    <HalFormButton name="unregisterFromEvent" modal={true}/>
                </div>
            </div>

            <hr className="border-border"/>

            <div className="bg-surface-raised rounded-md border border-border p-6">
                <h3 className="text-xs uppercase font-semibold text-text-secondary mb-4">INFORMACE O ZÁVODĚ</h3>
                <dl>
                    <DetailRow label="Status">
                        <Badge variant={statusVariant} size="sm">{event.status}</Badge>
                    </DetailRow>
                    <DetailRow label="Datum">{formatDate(event.eventDate)}</DetailRow>
                    {event.location && <DetailRow label="Místo">{event.location}</DetailRow>}
                    {event.organizer && <DetailRow label="Pořadatel">{event.organizer}</DetailRow>}
                    {event.websiteUrl && (
                        <DetailRow label="Web">
                            <a
                                href={event.websiteUrl}
                                target="_blank"
                                rel="noopener noreferrer"
                                className="text-primary hover:text-primary-light underline"
                            >
                                {event.websiteUrl}
                            </a>
                        </DetailRow>
                    )}
                    {event.eventCoordinatorId && (
                        <DetailRow label="Koordinátor">{event.eventCoordinatorId.value}</DetailRow>
                    )}
                </dl>
            </div>

            <div className="flex flex-col gap-4">
                <h2 className="text-xl font-bold text-text-primary">Přihlášky</h2>
                <HalEmbeddedTable<RegistrationData> collectionName="registrationDtoList">
                    <TableCell column="firstName">Jméno</TableCell>
                    <TableCell column="lastName">Příjmení</TableCell>
                    <TableCell column="registeredAt">Datum přihlášení</TableCell>
                </HalEmbeddedTable>
            </div>
        </div>
    );
};
