import {type ReactElement} from 'react';
import {Link, useSearchParams} from 'react-router-dom';
import {useHalPageData} from '../../hooks/useHalPageData.ts';
import {HalEmbeddedTable} from '../../components/HalNavigator2/HalEmbeddedTable.tsx';
import {KlabisTableWithQuery, TableCell} from '../../components/KlabisTable';
import {Alert, Skeleton} from '../../components/UI';
import {SyncStatusIndicator} from '../../components/sync/SyncStatusIndicator.tsx';
import type {components} from '../../api/klabisApi';
import type {HalResourceLinks} from '../../api';
import {asLinkArray, toHref} from '../../api/hateoas.ts';
import {extractNavigationPath} from '../../utils/navigationPath.ts';
import {formatDateTimeSeconds} from '../../utils/dateUtils.ts';
import {getEnumLabel, labels} from '../../localization';
import {SyncAttemptFilterBar} from './SyncAttemptFilterBar.tsx';
import {useSyncAttemptFilters} from './useSyncAttemptFilters.ts';

type SyncRecordItem = components['schemas']['SyncRecordSummaryResponse'];
type SyncAttemptItem = components['schemas']['EntityModelSyncAttemptResponse'];

const enumCell = (enumType: string) =>
    ({value}: {value: unknown}) => typeof value === 'string' ? getEnumLabel(enumType, value) : null;

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
        ? <Link to={extractNavigationPath(toHref(links.entity))}
                onClick={(e) => e.stopPropagation()}
                className="hover:text-primary hover:underline">{label}</Link>
        : label;
};

const AttemptsTab = (): ReactElement => {
    const {filters, extraParams, handleFilterChange} = useSyncAttemptFilters();

    return (
        <>
            <SyncAttemptFilterBar value={filters} onChange={handleFilterChange}/>
            {/* remount on filter change so the table's internal page state returns to the first page */}
            <HalEmbeddedTable<SyncAttemptItem>
                key={JSON.stringify(extraParams)}
                collectionName="syncAttempts"
                tableId="sync.attempts"
                emptyMessage={labels.syncOverview.empty}
                extraParams={extraParams}
            >
                <TableCell column="startedAt"
                           dataRender={({value}) => typeof value === 'string' ? formatDateTimeSeconds(value) : ''}>
                    {labels.syncOverview.columns.startedAt}
                </TableCell>
                <TableCell column="entityLabel"
                           dataRender={entityLabelCell}>
                    {labels.syncOverview.columns.entity}
                </TableCell>
                <TableCell column="entityType" dataRender={enumCell('syncEntityType')}>
                    {labels.syncOverview.columns.entityType}
                </TableCell>
                <TableCell column="trigger" dataRender={enumCell('syncTrigger')}>
                    {labels.syncOverview.columns.trigger}
                </TableCell>
                <TableCell column="direction" dataRender={enumCell('syncDirection')}>
                    {labels.syncOverview.columns.direction}
                </TableCell>
                <TableCell column="outcome" dataRender={enumCell('syncOutcome')}>
                    {labels.syncOverview.columns.outcome}
                </TableCell>
                <TableCell column="failureReason">{labels.syncOverview.columns.failureReason}</TableCell>
                <TableCell column="performedBy">{labels.syncOverview.columns.performedBy}</TableCell>
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
        collectionName="syncRecords"
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

type TabId = 'attempts' | 'attention';

const TabButton = ({tabId, activeTab, label, onClick}: {
    tabId: TabId;
    activeTab: TabId;
    label: string;
    onClick: (tab: TabId) => void;
}): ReactElement => {
    const isActive = activeTab === tabId;
    return (
        <button
            role="tab"
            id={`sync-tab-${tabId}`}
            aria-controls={`sync-tabpanel-${tabId}`}
            aria-selected={isActive}
            onClick={() => onClick(tabId)}
            className={`px-4 py-3 text-sm font-medium transition-colors ${
                isActive
                    ? 'text-primary border-b-2 border-primary'
                    : 'text-text-secondary hover:text-text-primary'
            }`}
        >
            {label}
        </button>
    );
};

export const SyncOverviewPage = (): ReactElement => {
    const {resourceData, isLoading, error} = useHalPageData();
    const [searchParams, setSearchParams] = useSearchParams();
    const activeTab: TabId = searchParams.get('tab') === 'attention' ? 'attention' : 'attempts';
    const setActiveTab = (tab: TabId) => setSearchParams((prev) => {
        const params = new URLSearchParams(prev);
        if (tab === 'attention') params.set('tab', 'attention'); else params.delete('tab');
        return params;
    });

    if (isLoading) {
        return <Skeleton/>;
    }

    if (error) {
        return <Alert severity="error">{error.message}</Alert>;
    }

    const attentionLink = resourceData?._links?.recordsNeedingAttention;
    const showAttention = activeTab === 'attention';
    const attentionHref = attentionLink ? toHref(attentionLink) : undefined;

    return (
        <div className="flex flex-col gap-8">
            <h1 className="text-3xl font-bold text-text-primary">{labels.syncOverview.title}</h1>

            <div className="flex flex-col gap-4">
                {attentionHref && (
                    <div role="tablist" className="flex border-b border-border">
                        <TabButton tabId="attempts" activeTab={activeTab} onClick={setActiveTab}
                                   label={labels.syncOverview.tabAttempts}/>
                        <TabButton tabId="attention" activeTab={activeTab} onClick={setActiveTab}
                                   label={labels.syncOverview.tabAttention}/>
                    </div>
                )}
                <div role="tabpanel" id={`sync-tabpanel-${showAttention ? 'attention' : 'attempts'}`}
                     aria-labelledby={`sync-tab-${showAttention ? 'attention' : 'attempts'}`}>
                    {showAttention && attentionHref ? <AttentionTab href={attentionHref}/> : <AttemptsTab/>}
                </div>
            </div>
        </div>
    );
};
