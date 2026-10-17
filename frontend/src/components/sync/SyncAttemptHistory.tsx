import {type ReactElement, useState} from 'react';
import {labels} from '../../localization';
import {KlabisTableWithQuery} from '../KlabisTable';
import {parseHalHref, serializeHalHref} from '../../api/hateoas';
import {stripUriTemplate} from '../../utils/halFormsUtils';
import type {components} from '../../api/klabisApi';
import {attemptColumns} from './syncAttemptColumns';

type SyncAttempt = components['schemas']['EntityModelSyncAttemptResponse'];

const HISTORY_SIZE = 10;

const buildHistoryHref = (href: string, includeUnchanged: boolean): string => {
    const target = parseHalHref(stripUriTemplate(href));
    if (includeUnchanged) target.searchParams.set('includeUnchanged', 'true');
    return serializeHalHref(target);
};

export const SyncAttemptHistory = ({href}: {href: string}): ReactElement => {
    const [includeUnchanged, setIncludeUnchanged] = useState(false);

    return (
        <div className="flex flex-col gap-2" data-testid="sync-overlay-history">
            <p className="text-sm font-medium text-text-secondary">{labels.sync.historyHeading}</p>
            <label className="flex items-center gap-2 text-sm text-text-primary">
                <input type="checkbox" checked={includeUnchanged}
                       onChange={(e) => setIncludeUnchanged(e.target.checked)}/>
                {labels.syncOverview.filters.includeUnchanged}
            </label>
            <div className="max-h-64 overflow-auto">
                <KlabisTableWithQuery<SyncAttempt>
                    key={String(includeUnchanged)}
                    link={{href: buildHistoryHref(href, includeUnchanged)}}
                    collectionName="syncAttempts"
                    defaultRowsPerPage={HISTORY_SIZE}
                    emptyMessage={labels.sync.historyEmpty}
                >
                    {attemptColumns.startedAt}
                    {attemptColumns.trigger}
                    {attemptColumns.direction}
                    {attemptColumns.outcome}
                    {attemptColumns.failureReason}
                    {attemptColumns.performedBy}
                </KlabisTableWithQuery>
            </div>
        </div>
    );
};
