import {type ReactElement} from 'react';
import {useHalPageData} from '../../hooks/useHalPageData.ts';
import {HalEmbeddedTable} from '../../components/HalNavigator2/HalEmbeddedTable.tsx';
import {TableCell} from '../../components/KlabisTable';
import {Alert, Skeleton} from '../../components/UI';
import {SyncStatusIndicator} from '../../components/sync/SyncStatusIndicator.tsx';
import type {components} from '../../api/klabisApi';
import type {HalResourceLinks} from '../../api';
import {asLinkArray} from '../../api/hateoas.ts';
import {formatDateTimeSeconds} from '../../utils/dateUtils.ts';
import {getEnumLabel, labels} from '../../localization';

type SyncAttemptItem = components['schemas']['EntityModelSyncAttemptResponse'];

const enumCell = (enumType: string) =>
    ({value}: {value: unknown}) => typeof value === 'string' ? getEnumLabel(enumType, value) : null;

const AttemptsTab = (): ReactElement => (
    <HalEmbeddedTable<SyncAttemptItem>
        collectionName="syncAttempts"
        tableId="sync.attempts"
        emptyMessage={labels.syncOverview.empty}
    >
        <TableCell column="startedAt"
                   dataRender={({value}) => typeof value === 'string' ? formatDateTimeSeconds(value) : ''}>
            {labels.syncOverview.columns.startedAt}
        </TableCell>
        <TableCell column="entityLabel">{labels.syncOverview.columns.entity}</TableCell>
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
                   dataRender={({item}) => {
                       const links = item._links as Record<string, HalResourceLinks> | undefined;
                       const syncLink = links?.syncState ? asLinkArray(links.syncState)[0] : undefined;
                       return syncLink
                           ? <span onClick={(e) => e.stopPropagation()}>
                               <SyncStatusIndicator syncLink={syncLink} mode="icon"/>
                           </span>
                           : null;
                   }}>
            {labels.syncOverview.columns.status}
        </TableCell>
    </HalEmbeddedTable>
);

export const SyncOverviewPage = (): ReactElement => {
    const {isLoading, error} = useHalPageData();

    if (isLoading) {
        return <Skeleton/>;
    }

    if (error) {
        return <Alert severity="error">{error.message}</Alert>;
    }

    return (
        <div className="flex flex-col gap-8">
            <h1 className="text-3xl font-bold text-text-primary">{labels.syncOverview.title}</h1>

            <div className="flex flex-col gap-4">
                <h2 className="text-xl font-bold text-text-primary">{labels.syncOverview.attemptsHeading}</h2>
                <AttemptsTab/>
            </div>
        </div>
    );
};
