import {type ReactElement, useState} from 'react';
import {Calendar, Copy, Check} from 'lucide-react';
import {Button} from '../../components/UI';
import {labels} from '../../localization';
import {useAuthorizedQuery, useAuthorizedMutation} from '../../hooks/useAuthorizedFetch';
import {formatDate} from '../../utils/dateUtils';

interface IcalTokenState {
    url: string | null;
    lastSetAt: string | null;
}

interface CalendarFeedSectionProps {
    icalTokenHref: string;
}

const l = labels.calendarFeed;

const CopyButton = ({url}: {url: string}): ReactElement => {
    const [copied, setCopied] = useState(false);

    const handleCopy = async () => {
        await navigator.clipboard.writeText(url);
        setCopied(true);
        setTimeout(() => setCopied(false), 2000);
    };

    return (
        <Button
            variant="secondary"
            size="sm"
            onClick={handleCopy}
            startIcon={copied ? <Check className="w-4 h-4"/> : <Copy className="w-4 h-4"/>}
        >
            {copied ? l.copiedButton : l.copyButton}
        </Button>
    );
};

const HelpText = (): ReactElement => (
    <div className="mt-4 p-4 bg-surface-base rounded-md border border-border text-sm text-text-secondary space-y-2">
        <p className="font-medium text-text-primary">{l.helpTitle}</p>
        <p>{l.helpText}</p>
        <ul className="list-disc list-inside space-y-1 mt-2">
            <li>{l.helpGoogle}</li>
            <li>{l.helpApple}</li>
            <li>{l.helpOutlook}</li>
        </ul>
        <p className="mt-2 text-xs text-text-tertiary">{l.helpMyScheduleNote}</p>
    </div>
);

export const CalendarFeedSection = ({icalTokenHref}: CalendarFeedSectionProps): ReactElement => {
    const [confirming, setConfirming] = useState(false);
    const [revealedUrl, setRevealedUrl] = useState<string | null>(null);

    const {data: tokenState, isLoading} = useAuthorizedQuery<IcalTokenState>(icalTokenHref, {
        staleTime: 0,
    });

    const {mutate, isPending} = useAuthorizedMutation({method: 'POST'});

    const handleGenerate = () => {
        mutate({url: icalTokenHref}, {
            onSuccess: (result) => {
                const responseData = result.data as IcalTokenState | null;
                if (responseData?.url) {
                    setRevealedUrl(responseData.url);
                }
            },
        });
    };

    const handleConfirmRegenerate = () => {
        setConfirming(false);
        handleGenerate();
    };

    const hasToken = tokenState?.url != null;
    const showFullUrl = revealedUrl !== null;

    const regenerateControl = confirming ? (
        <div className="flex flex-col gap-2 mt-2 p-3 rounded-md border border-border bg-surface-base">
            <p className="text-sm font-medium text-text-primary">{l.confirmRegenerateTitle}</p>
            <p className="text-sm text-text-secondary">{l.confirmRegenerateMessage}</p>
            <div className="flex gap-2">
                <Button variant="secondary" size="sm" onClick={() => setConfirming(false)}>
                    {labels.buttons.cancel}
                </Button>
                <Button variant="danger" size="sm" onClick={handleConfirmRegenerate} loading={isPending}>
                    {l.confirmRegenerateAction}
                </Button>
            </div>
        </div>
    ) : (
        <Button
            variant="secondary"
            size="sm"
            onClick={() => setConfirming(true)}
            disabled={isPending}
            loading={isPending}
            className="self-start mt-2"
        >
            {l.regenerateButton}
        </Button>
    );

    return (
        <div className="flex flex-col gap-4">
            <div className="flex items-center gap-2 text-text-secondary">
                <Calendar className="w-4 h-4 flex-shrink-0"/>
                <span className="text-sm">{l.intro}</span>
            </div>

            {isLoading && (
                <div className="text-sm text-text-tertiary">{labels.ui.loading}</div>
            )}

            {!isLoading && showFullUrl && (
                <div className="flex flex-col gap-2">
                    <p className="text-sm font-medium text-text-primary">{l.fullUrlLabel}</p>
                    <div className="flex items-start gap-2">
                        <code className="flex-1 text-xs bg-surface-base border border-border rounded px-3 py-2 break-all text-text-primary">
                            {revealedUrl}
                        </code>
                        <CopyButton url={revealedUrl!}/>
                    </div>
                    <p className="text-xs text-warning">{l.fullUrlNote}</p>
                    {regenerateControl}
                </div>
            )}

            {!isLoading && !showFullUrl && hasToken && (
                <div className="flex flex-col gap-2">
                    <p className="text-sm font-medium text-text-primary">{l.maskedUrlLabel}</p>
                    <div className="flex items-start gap-2">
                        <code className="flex-1 text-xs bg-surface-base border border-border rounded px-3 py-2 break-all text-text-secondary">
                            {tokenState.url}
                        </code>
                    </div>
                    {tokenState.lastSetAt && (
                        <p className="text-xs text-text-tertiary">
                            {l.generatedAt}: {formatDate(tokenState.lastSetAt)}
                        </p>
                    )}
                    {regenerateControl}
                </div>
            )}

            {!isLoading && !showFullUrl && !hasToken && (
                <Button
                    variant="primary"
                    size="sm"
                    onClick={handleGenerate}
                    disabled={isPending}
                    loading={isPending}
                    startIcon={<Calendar className="w-4 h-4"/>}
                    className="self-start"
                >
                    {l.createButton}
                </Button>
            )}

            <HelpText/>
        </div>
    );
};
