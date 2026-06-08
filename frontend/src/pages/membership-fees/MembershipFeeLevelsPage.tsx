import {type ReactElement} from 'react';
import {useHalPageData} from '../../hooks/useHalPageData.ts';
import {HalEmbeddedTable} from '../../components/HalNavigator2/HalEmbeddedTable.tsx';
import {HalFormButton} from '../../components/HalNavigator2/HalFormButton.tsx';
import {TableCell} from '../../components/KlabisTable';
import {Alert, Skeleton} from '../../components/UI';
import type {EntityModel} from '../../api';
import {labels} from '../../localization';

type FeeLevelSummary = EntityModel<{
    id: string;
    name: string;
    annualFee: number;
}>;

export const MembershipFeeLevelsPage = (): ReactElement => {
    const {isLoading, error, route} = useHalPageData();

    if (isLoading) {
        return <Skeleton/>;
    }

    if (error) {
        return <Alert severity="error">{error.message}</Alert>;
    }

    return (
        <div className="flex flex-col gap-8">
            <h1 className="text-3xl font-bold text-text-primary">{labels.sections.membershipFeeLevelsList}</h1>

            <div className="flex flex-col gap-4">
                <div className="flex items-center justify-between">
                    <h2 className="text-xl font-bold text-text-primary">{labels.sections.membershipFeeLevelsList}</h2>
                    <HalFormButton name="createMembershipFeeLevel" modal={true}/>
                </div>
                <HalEmbeddedTable<FeeLevelSummary>
                    collectionName="membershipFeeLevelResponseList"
                    tableId="membership-fee-levels"
                    defaultOrderBy="name"
                    onRowClick={route.navigateToResource}
                    emptyMessage="Žádné úrovně členských příspěvků."
                >
                    <TableCell sortable column="name">{labels.fields.name}</TableCell>
                    <TableCell sortable column="annualFee">{labels.fields.annualFee}</TableCell>
                </HalEmbeddedTable>
            </div>
        </div>
    );
};
