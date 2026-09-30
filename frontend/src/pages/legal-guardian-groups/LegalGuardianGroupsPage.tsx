import {type ReactElement} from 'react';
import {useHalPageData} from '../../hooks/useHalPageData.ts';
import {HalEmbeddedTable} from '../../components/HalNavigator2/HalEmbeddedTable.tsx';
import {TableCell} from '../../components/KlabisTable';
import {Alert, Skeleton} from '../../components/UI';
import type {components} from '../../api/klabisApi';
import {labels} from '../../localization';

type LegalGuardianGroupSummaryItem = components['schemas']['EntityModelLegalGuardianGroupSummaryResponse'];

const formatGuardianNames = ({value}: {value: unknown}): string =>
    ((value as LegalGuardianGroupSummaryItem['guardians']) ?? [])
        .map(guardian => `${guardian.firstName ?? ''} ${guardian.lastName ?? ''}`.trim())
        .filter(Boolean)
        .join(', ');

export const LegalGuardianGroupsPage = (): ReactElement => {
    const {isLoading, error, route} = useHalPageData();

    if (isLoading) {
        return <Skeleton/>;
    }

    if (error) {
        return <Alert severity="error">{error.message}</Alert>;
    }

    return (
        <div className="flex flex-col gap-8">
            <h1 className="text-3xl font-bold text-text-primary">{labels.sections.legalGuardianGroups}</h1>

            <HalEmbeddedTable<LegalGuardianGroupSummaryItem>
                collectionName="legalGuardianGroupSummaryResponseList"
                tableId="groups.legalGuardian"
                defaultOrderBy="name"
                onRowClick={route.navigateToResource}
                emptyMessage={labels.ui.noLegalGuardianGroups}
            >
                <TableCell sortable column="name">{labels.fields.name}</TableCell>
                <TableCell column="guardians" dataRender={formatGuardianNames}>{labels.fields.guardians}</TableCell>
                <TableCell sortable column="minorCount">{labels.fields.minorCount}</TableCell>
            </HalEmbeddedTable>
        </div>
    );
};
