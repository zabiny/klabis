import {type ReactElement} from 'react';
import {useHalPageData} from '../../hooks/useHalPageData.ts';
import {HalEmbeddedTable} from '../../components/HalNavigator2/HalEmbeddedTable.tsx';
import {HalFormButton} from '../../components/HalNavigator2/HalFormButton.tsx';
import {TableCell} from '../../components/KlabisTable';
import {Alert, Skeleton} from '../../components/UI';
import type {EntityModel} from '../../api';
import {labels} from '../../localization';

type GroupSummary = EntityModel<{
    id: string;
    name: string;
}>;

export const GroupsPage = (): ReactElement => {
    const {isLoading, error, route} = useHalPageData();

    if (isLoading) {
        return <Skeleton/>;
    }

    if (error) {
        return <Alert severity="error">{error.message}</Alert>;
    }

    return (
        <div className="flex flex-col gap-8">
            <h1 className="text-3xl font-bold text-text-primary">{labels.sections.groups}</h1>

            <div className="flex flex-col gap-4">
                <div className="flex items-center justify-between">
                    <h2 className="text-xl font-bold text-text-primary">{labels.sections.groupsList}</h2>
                    <HalFormButton name="createGroup" modal={true}/>
                </div>
                <HalEmbeddedTable<GroupSummary>
                    collectionName="groupSummaryResponseList"
                    defaultOrderBy="name"
                    onRowClick={route.navigateToResource}
                    emptyMessage="Nejste členem žádné skupiny."
                >
                    <TableCell sortable column="name">{labels.fields.name}</TableCell>
                </HalEmbeddedTable>
            </div>
        </div>
    );
};
