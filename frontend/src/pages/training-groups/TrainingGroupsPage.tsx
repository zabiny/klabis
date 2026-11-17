import {type ReactElement} from 'react';
import {useHalPageData} from '../../hooks/useHalPageData.ts';
import {HalEmbeddedTable} from '../../components/HalNavigator2/HalEmbeddedTable.tsx';
import {HalFormButton} from '../../components/HalNavigator2/HalFormButton.tsx';
import {TableCell} from '../../components/KlabisTable';
import type {components} from '../../api/klabisApi';
import {labels} from '../../localization';
import {HalPageGate} from '../HalPageGate.tsx';

type TrainingGroupSummaryItem = components['schemas']['EntityModelTrainingGroupSummaryResponse'];

const TrainingGroupsPageContent = (): ReactElement => {
    const {navigateToResource} = useHalPageData();

    return (
        <div className="flex flex-col gap-8">
            <h1 className="text-3xl font-bold text-text-primary">{labels.sections.trainingGroupsList}</h1>

            <div className="flex flex-col gap-4">
                <div className="flex items-center justify-between">
                    <h2 className="text-xl font-bold text-text-primary">{labels.sections.trainingGroupsList}</h2>
                    <HalFormButton name="createTrainingGroup" modal={true}/>
                </div>
                <HalEmbeddedTable<TrainingGroupSummaryItem>
                    tableId="groups.training"
                    defaultOrderBy="name"
                    onRowClick={navigateToResource}
                    emptyMessage="Nejste členem žádné tréninkové skupiny."
                >
                    <TableCell sortable column="name">{labels.fields.name}</TableCell>
                    <TableCell sortable column="minAge">{labels.fields.minAge}</TableCell>
                    <TableCell sortable column="maxAge">{labels.fields.maxAge}</TableCell>
                    <TableCell sortable column="memberCount">{labels.fields.memberCount}</TableCell>
                </HalEmbeddedTable>
            </div>
        </div>
    );
};

export const TrainingGroupsPage = (): ReactElement => (
    <HalPageGate>
        {() => <TrainingGroupsPageContent/>}
    </HalPageGate>
);
