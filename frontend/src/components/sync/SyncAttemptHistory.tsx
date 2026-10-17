import {type ReactElement, useState} from 'react';
import {useAuthorizedQuery} from '../../hooks/useAuthorizedFetch';
import {formatDateTimeSeconds} from '../../utils/dateUtils';
import {labels} from '../../localization';
import {Skeleton} from '../UI';
import type {components} from '../../api/klabisApi';
import {parseHalHref, serializeHalHref} from '../../api/hateoas';
import {enumLabelOrEmpty} from './syncAttemptFormat';

type SyncAttempt = components['schemas']['SyncAttemptResponse'];
type AttemptPage = {_embedded?: {syncAttempts?: SyncAttempt[]}};

const HISTORY_SIZE = 10;

const buildHistoryUrl = (href: string, includeUnchanged: boolean): string => {
    const target = parseHalHref(href.replace(/\{[^}]*\}$/, ''));
    target.searchParams.set('size', String(HISTORY_SIZE));
    target.searchParams.set('page', '0');
    if (includeUnchanged) target.searchParams.set('includeUnchanged', 'true');
    return serializeHalHref(target);
};

export const SyncAttemptHistory = ({href}: {href: string}): ReactElement => {
    const [includeUnchanged, setIncludeUnchanged] = useState(false);
    const url = buildHistoryUrl(href, includeUnchanged);
    const {data, isLoading, error} = useAuthorizedQuery<AttemptPage>(url);
    const attempts = data?._embedded?.syncAttempts ?? [];

    return (
        <div className="flex flex-col gap-2" data-testid="sync-overlay-history">
            <p className="text-sm font-medium text-text-secondary">{labels.sync.historyHeading}</p>
            <label className="flex items-center gap-2 text-sm text-text-primary">
                <input type="checkbox" checked={includeUnchanged}
                       onChange={(e) => setIncludeUnchanged(e.target.checked)}/>
                {labels.syncOverview.filters.includeUnchanged}
            </label>
            {isLoading ? <Skeleton/> : error ? (
                <p className="text-sm text-red-600">{labels.sync.historyLoadError}</p>
            ) : attempts.length === 0 ? (
                <p className="text-sm text-text-secondary" data-testid="sync-history-empty">{labels.sync.historyEmpty}</p>
            ) : (
                <div className="max-h-64 overflow-auto">
                    <table className="w-full text-sm border-collapse">
                        <thead>
                            <tr className="text-left text-text-secondary">
                                <th className="border-b border-border py-1 pr-3">{labels.syncOverview.columns.startedAt}</th>
                                <th className="border-b border-border py-1 pr-3">{labels.syncOverview.columns.trigger}</th>
                                <th className="border-b border-border py-1 pr-3">{labels.syncOverview.columns.direction}</th>
                                <th className="border-b border-border py-1 pr-3">{labels.syncOverview.columns.outcome}</th>
                                <th className="border-b border-border py-1 pr-3">{labels.syncOverview.columns.failureReason}</th>
                                <th className="border-b border-border py-1">{labels.syncOverview.columns.performedBy}</th>
                            </tr>
                        </thead>
                        <tbody>
                            {attempts.map((a, i) => (
                                <tr key={`${a.startedAt}-${i}`} data-testid="sync-history-row">
                                    <td className="border-b border-border py-1 pr-3 whitespace-nowrap">{formatDateTimeSeconds(a.startedAt)}</td>
                                    <td className="border-b border-border py-1 pr-3">{enumLabelOrEmpty('syncTrigger', a.trigger)}</td>
                                    <td className="border-b border-border py-1 pr-3">{enumLabelOrEmpty('syncDirection', a.direction)}</td>
                                    <td className="border-b border-border py-1 pr-3">{enumLabelOrEmpty('syncOutcome', a.outcome)}</td>
                                    <td className="border-b border-border py-1 pr-3">{a.failureReason}</td>
                                    <td className="border-b border-border py-1">{a.performedBy}</td>
                                </tr>
                            ))}
                        </tbody>
                    </table>
                </div>
            )}
        </div>
    );
};
