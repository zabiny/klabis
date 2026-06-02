import {type ReactElement, useState} from 'react';
import {Link, useParams} from 'react-router-dom';
import {useAuthorizedQuery} from '../../hooks/useAuthorizedFetch.ts';
import {authorizedFetch} from '../../api/authorizedFetch.ts';
import {Button, Skeleton} from '../../components/UI';
import {ErrorPage} from '../ErrorPage.tsx';
import {labels} from '../../localization';
import {formatDate} from '../../utils/dateUtils.ts';
import {Download, Printer} from 'lucide-react';
import type {HalResponse} from '../../api';
import {toHref} from '../../api/hateoas.ts';

interface AccommodationListItem {
    firstName: string;
    lastName: string;
    identityCardNumber?: string | null;
    identityCardValidityDate?: string | null;
    dateOfBirth?: string | null;
    addressStreet?: string | null;
    addressCity?: string | null;
    addressPostalCode?: string | null;
    addressCountry?: string | null;
}

interface AccommodationListResponse extends HalResponse {
    _embedded?: {
        accommodationList: AccommodationListItem[];
    };
}

interface EventData extends HalResponse {
    name?: string;
}

function formatAddress(item: AccommodationListItem): string | null {
    const parts = [
        item.addressStreet,
        item.addressCity,
        item.addressPostalCode,
        item.addressCountry,
    ].filter(Boolean);
    return parts.length > 0 ? parts.join(', ') : null;
}

const NOT_PROVIDED = labels.ui.notProvided;

function parseFilenameFromContentDisposition(header: string | null): string {
    if (!header) return 'ubytovani.csv';
    const match = header.match(/filename="([^"]+)"/);
    return match ? match[1] : 'ubytovani.csv';
}

export const AccommodationListPage = (): ReactElement => {
    const {id: eventId} = useParams<{id: string}>();
    const [isDownloading, setIsDownloading] = useState(false);

    const eventUrl = `/api/events/${eventId}`;

    const {data: eventData, isLoading: eventLoading, error: eventError} =
        useAuthorizedQuery<EventData>(eventUrl);

    const accommodationListLink = eventData?._links?.['accommodation-list'];
    const accommodationListUrl = accommodationListLink ? toHref(accommodationListLink) : null;

    const {data: listData, isLoading: listLoading, error: listError} =
        useAuthorizedQuery<AccommodationListResponse>(
            accommodationListUrl ?? '',
            {enabled: !!accommodationListUrl}
        );

    if (eventLoading || listLoading) {
        return <Skeleton/>;
    }

    const error = eventError ?? listError;
    if (error) {
        return <ErrorPage error={error}/>;
    }

    if (eventData && !accommodationListUrl) {
        return <ErrorPage error={{responseStatus: 403, message: 'HTTP 403: Forbidden'}}/>;
    }

    const items = listData?._embedded?.accommodationList ?? [];
    const eventName = eventData?.name ?? '';

    const handleDownloadCsv = async () => {
        if (!accommodationListUrl) return;
        setIsDownloading(true);
        try {
            const response = await authorizedFetch(
                accommodationListUrl,
                {headers: {'Accept': 'text/csv'}},
                true
            );
            const blob = await response.blob();
            const filename = parseFilenameFromContentDisposition(
                response.headers.get('Content-Disposition')
            );
            const objectUrl = URL.createObjectURL(blob);
            const anchor = document.createElement('a');
            anchor.href = objectUrl;
            anchor.download = filename;
            anchor.click();
            URL.revokeObjectURL(objectUrl);
        } finally {
            setIsDownloading(false);
        }
    };

    return (
        <div className="flex flex-col gap-6 print:gap-4">
            <div className="print:hidden">
                <Link to={`/events/${eventId}`} className="text-sm text-primary hover:text-primary-light">
                    {labels.ui.backToList}
                </Link>
            </div>

            <div className="flex flex-col gap-2 sm:flex-row sm:items-center sm:justify-between print:flex-col">
                <div>
                    <h1 className="text-2xl font-bold text-text-primary print:text-black">
                        {labels.sections.accommodationList}
                    </h1>
                    {eventName && (
                        <p className="text-text-secondary print:text-black">{eventName}</p>
                    )}
                </div>
                <div className="print:hidden flex gap-2">
                    <Button
                        variant="secondary"
                        onClick={handleDownloadCsv}
                        disabled={isDownloading}
                        startIcon={<Download className="w-4 h-4"/>}
                    >
                        {labels.buttons.downloadCsv}
                    </Button>
                    <Button
                        variant="primary"
                        onClick={() => window.print()}
                        startIcon={<Printer className="w-4 h-4"/>}
                    >
                        {labels.buttons.print}
                    </Button>
                </div>
            </div>

            <div className="overflow-x-auto">
                <table className="w-full border-collapse text-sm print:text-xs print:text-black">
                    <thead>
                        <tr className="bg-bg-secondary print:bg-white print:border-b-2 print:border-black">
                            <th className="border border-border px-3 py-2 text-left font-semibold print:border-black">{labels.fields.firstName}</th>
                            <th className="border border-border px-3 py-2 text-left font-semibold print:border-black">{labels.fields.lastName}</th>
                            <th className="border border-border px-3 py-2 text-left font-semibold print:border-black">{labels.tables.identityCardNumber}</th>
                            <th className="border border-border px-3 py-2 text-left font-semibold print:border-black">{labels.tables.identityCardValidityDate}</th>
                            <th className="border border-border px-3 py-2 text-left font-semibold print:border-black">{labels.tables.dateOfBirth}</th>
                            <th className="border border-border px-3 py-2 text-left font-semibold print:border-black">{labels.tables.address}</th>
                        </tr>
                    </thead>
                    <tbody>
                        {items.map((item, index) => {
                            const address = formatAddress(item);
                            return (
                                <tr key={index} className="even:bg-bg-secondary print:even:bg-white print:border-b print:border-black">
                                    <td className="border border-border px-3 py-2 print:border-black">{item.firstName}</td>
                                    <td className="border border-border px-3 py-2 print:border-black">{item.lastName}</td>
                                    <td className="border border-border px-3 py-2 print:border-black">{item.identityCardNumber ?? NOT_PROVIDED}</td>
                                    <td className="border border-border px-3 py-2 print:border-black">{item.identityCardValidityDate ? formatDate(item.identityCardValidityDate) : NOT_PROVIDED}</td>
                                    <td className="border border-border px-3 py-2 print:border-black">{item.dateOfBirth ? formatDate(item.dateOfBirth) : NOT_PROVIDED}</td>
                                    <td className="border border-border px-3 py-2 print:border-black">{address ?? NOT_PROVIDED}</td>
                                </tr>
                            );
                        })}
                    </tbody>
                </table>
            </div>
        </div>
    );
};
