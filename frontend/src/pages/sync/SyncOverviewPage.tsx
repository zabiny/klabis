import {type ReactElement, useMemo} from 'react';
import {Link, useSearchParams} from 'react-router-dom';
import {HalEmbeddedTable} from '../../components/HalNavigator2/HalEmbeddedTable.tsx';
import {KlabisTableWithQuery, TableCell} from '../../components/KlabisTable';
import {Button, Card, TabButton} from '../../components/UI';
import {useTemplateDialog} from '../../components/HalNavigator2/useTemplateDialog.tsx';
import {halResource} from '../../api/halResource';
import {useAuthorizedQuery} from '../../hooks/useAuthorizedFetch.ts';
import {useRootResource} from '../../hooks/useRootNavigation.ts';
import {SyncStatusIndicator} from '../../components/sync/SyncStatusIndicator.tsx';
import type {components} from '../../api/klabisApi';
import type {HalResourceLinks, HalResponse} from '../../api';
import {asLinkArray, toHref} from '../../api/hateoas.ts';
import {toRoutePath} from '../../api/apiPath';
import {formatDateTimeSeconds} from '../../utils/dateUtils.ts';
import {labels} from '../../localization';
import {attemptColumns, enumCell} from '../../components/sync/syncAttemptColumns.tsx';
import {SyncAttemptFilterBar} from './SyncAttemptFilterBar.tsx';
import {toQuery, type SyncAttemptFilters} from './syncAttemptsQuery.ts';
import {useUrlFilters} from '../../hooks/useUrlFilters.ts';
import {HalPageGate} from '../HalPageGate.tsx';

type SyncRecordItem = components['schemas']['SyncRecordSummaryResponse'];
type SyncAttemptItem = components['schemas']['EntityModelSyncAttemptResponse'];

const syncStateIndicator = (item: {_links?: unknown}) => {
    const links = item._links as Record<string, HalResourceLinks> | undefined;
    const syncLink = links?.syncState ? asLinkArray(links.syncState)[0] : undefined;
    return syncLink
        ? <span onClick={(e) => e.stopPropagation()}>
            <SyncStatusIndicator syncLink={syncLink} mode="icon"/>
        </span>
        : null;
};

const entityLabelCell = ({value, item}: {value: unknown; item: {_links?: unknown}}) => {
    const links = item._links as Record<string, HalResourceLinks> | undefined;
    const label = typeof value === 'string' ? value : '';
    return links?.entity
        ? <Link to={toRoutePath(toHref(links.entity))}
                onClick={(e) => e.stopPropagation()}
                className="hover:text-primary hover:underline">{label}</Link>
        : label;
};

const AttemptsTab = (): ReactElement => {
    const {values, setValues} = useUrlFilters({
        entityType: {type: 'string'},
        outcome: {type: 'string'},
        dateFrom: {type: 'string'},
        dateTo: {type: 'string'},
        includeUnchanged: {type: 'boolean'},
    });
    const filters = values as SyncAttemptFilters;
    const extraParams = useMemo(() => toQuery(filters), [filters]);

    return (
        <>
            <SyncAttemptFilterBar value={filters} onChange={setValues}/>
            <HalEmbeddedTable<SyncAttemptItem>
                tableId="sync.attempts"
                emptyMessage={Object.keys(extraParams).length > 0
                    ? labels.syncOverview.emptyFiltered
                    : labels.syncOverview.empty}
                extraParams={extraParams}
            >
                {attemptColumns.startedAt}
                <TableCell column="entityLabel" dataRender={entityLabelCell}>
                    {labels.syncOverview.columns.entity}
                </TableCell>
                <TableCell column="entityType" dataRender={enumCell('syncEntityType')}>
                    {labels.syncOverview.columns.entityType}
                </TableCell>
                {attemptColumns.trigger}
                {attemptColumns.direction}
                {attemptColumns.outcome}
                {attemptColumns.failureReason}
                {attemptColumns.performedBy}
                <TableCell column="_links-syncState"
                           dataRender={({item}) => syncStateIndicator(item)}>
                    {labels.syncOverview.columns.status}
                </TableCell>
            </HalEmbeddedTable>
        </>
    );
};

const AttentionTab = ({href}: {href: string}): ReactElement => (
    <KlabisTableWithQuery<SyncRecordItem>
        link={{href}}
        tableId="sync.attention"
        emptyMessage={labels.syncOverview.attentionEmpty}
    >
        <TableCell column="entityLabel" dataRender={entityLabelCell}>
            {labels.syncOverview.columns.entity}
        </TableCell>
        <TableCell column="entityType" dataRender={enumCell('syncEntityType')}>
            {labels.syncOverview.columns.entityType}
        </TableCell>
        <TableCell column="status" dataRender={enumCell('syncStatus')}>
            {labels.syncOverview.columns.currentStatus}
        </TableCell>
        <TableCell column="lastSuccessfulSyncAt"
                   dataRender={({value}) => typeof value === 'string'
                       ? formatDateTimeSeconds(value)
                       : labels.sync.neverSynced}>
            {labels.syncOverview.columns.lastSuccessfulSyncAt}
        </TableCell>
        <TableCell column="failedAttemptsSinceLastSuccess">
            {labels.syncOverview.columns.failedAttempts}
        </TableCell>
        <TableCell column="_links-syncState" dataRender={({item}) => syncStateIndicator(item)}>
            {labels.syncOverview.columns.status}
        </TableCell>
    </KlabisTableWithQuery>
);

const ClubKeySection = ({href}: {href: string}): ReactElement => {
    const {data} = useAuthorizedQuery(href);
    const state = data as (components['schemas']['ClubKeyStateResponse'] & HalResponse) | undefined;
    const resource = halResource(state);
    const setAction = resource.template('setClubKey');
    const clearAction = resource.template('clearClubKey');
    const dialog = useTemplateDialog({navigateOnSuccess: false});

    return (
        <Card className="p-6">
            <h3 className="text-xs uppercase font-semibold text-text-secondary mb-4">
                {labels.syncOverview.clubKey.title}
            </h3>
            {state && (
                <p className="text-sm text-text-primary mb-4">
                    {state.isSet ? labels.syncOverview.clubKey.held : labels.syncOverview.clubKey.notHeld}
                </p>
            )}
            <div className="flex gap-2">
                {setAction && <Button variant="secondary" onClick={() => dialog.open(setAction)}>{setAction.label}</Button>}
                {clearAction && <Button variant="secondary" onClick={() => dialog.open(clearAction)}>{clearAction.label}</Button>}
            </div>
            {dialog.dialog}
        </Card>
    );
};

type TabId = 'attention' | 'attempts' | 'settings';

const SyncOverviewContent = ({resourceData}: {resourceData: HalResponse}): ReactElement => {
    const [searchParams, setSearchParams] = useSearchParams();
    const {data: root} = useRootResource();

    const attentionLink = resourceData._links?.recordsNeedingAttention;
    const attentionHref = attentionLink ? toHref(attentionLink) : undefined;
    const clubKeyLink = root?._links?.orisClubKey;
    const clubKeyHref = clubKeyLink ? toHref(clubKeyLink) : undefined;

    const tabs: {id: TabId; label: string}[] = [
        ...(attentionHref ? [{id: 'attention' as const, label: labels.syncOverview.tabAttention}] : []),
        {id: 'attempts', label: labels.syncOverview.tabAttempts},
        ...(clubKeyHref ? [{id: 'settings' as const, label: labels.syncOverview.tabSettings}] : []),
    ];

    const requested = searchParams.get('tab');
    const activeTab = tabs.find(tab => tab.id === requested)?.id ?? tabs[0].id;

    const setActiveTab = (tab: TabId) => setSearchParams((prev) => {
        const params = new URLSearchParams(prev);
        if (tab === tabs[0].id) params.delete('tab'); else params.set('tab', tab);
        return params;
    });

    return (
        <div className="flex flex-col gap-8">
            <h1 className="text-3xl font-bold text-text-primary">{labels.syncOverview.title}</h1>

            <div className="flex flex-col gap-4">
                {tabs.length > 1 && (
                    <div role="tablist" className="flex border-b border-border">
                        {tabs.map(tab => (
                            <TabButton key={tab.id} active={activeTab === tab.id} onClick={() => setActiveTab(tab.id)}
                                       id={`sync-tab-${tab.id}`} controls={`sync-tabpanel-${tab.id}`}
                                       label={tab.label}/>
                        ))}
                    </div>
                )}
                <div role="tabpanel" id={`sync-tabpanel-${activeTab}`} aria-labelledby={`sync-tab-${activeTab}`}>
                    {activeTab === 'attention' && attentionHref && <AttentionTab href={attentionHref}/>}
                    {activeTab === 'attempts' && <AttemptsTab/>}
                    {activeTab === 'settings' && clubKeyHref && <ClubKeySection href={clubKeyHref}/>}
                </div>
            </div>
        </div>
    );
};

export const SyncOverviewPage = (): ReactElement => (
    <HalPageGate>
        {(resourceData) => <SyncOverviewContent resourceData={resourceData}/>}
    </HalPageGate>
);
