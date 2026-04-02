import {type ReactElement} from 'react';
import {useHalPageData} from '../../hooks/useHalPageData.ts';
import {HalEmbeddedTable} from '../../components/HalNavigator2/HalEmbeddedTable.tsx';
import {HalFormButton} from '../../components/HalNavigator2/HalFormButton.tsx';
import {TableCell} from '../../components/KlabisTable';
import {Alert, Skeleton} from '../../components/UI';
import type {EntityModel} from '../../api';
import {labels} from '../../localization';

type FamilyGroupSummaryItem = EntityModel<{
    id: string;
    name: string;
    memberCount: number;
}>;

export const FamilyGroupsPage = (): ReactElement => {
    const {isLoading, error, route} = useHalPageData();

    if (isLoading) {
        return <Skeleton/>;
    }

    if (error) {
        return <Alert severity="error">{error.message}</Alert>;
    }

    return (
        <div className="flex flex-col gap-8">
            <h1 className="text-3xl font-bold text-text-primary">{labels.sections.familyGroups}</h1>

            <div className="flex flex-col gap-4">
                <div className="flex items-center justify-between">
                    <h2 className="text-xl font-bold text-text-primary">{labels.sections.familyGroupsList}</h2>
                    <HalFormButton name="createFamilyGroup" modal={true}/>
                </div>
                <HalEmbeddedTable<FamilyGroupSummaryItem>
                    collectionName="familyGroupSummaryResponseList"
                    defaultOrderBy="name"
                    onRowClick={route.navigateToResource}
                    emptyMessage="Nejste členem žádné rodinné skupiny."
                >
                    <TableCell sortable column="name">{labels.fields.name}</TableCell>
                    <TableCell sortable column="memberCount">{labels.fields.memberCount}</TableCell>
                </HalEmbeddedTable>
            </div>
        </div>
    );
};
