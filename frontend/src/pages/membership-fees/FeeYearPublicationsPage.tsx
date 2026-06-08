import {type ReactElement} from 'react';
import {useHalPageData} from '../../hooks/useHalPageData.ts';
import {HalEmbeddedTable} from '../../components/HalNavigator2/HalEmbeddedTable.tsx';
import {HalFormButton} from '../../components/HalNavigator2/HalFormButton.tsx';
import {TableCell} from '../../components/KlabisTable';
import {Alert, Skeleton} from '../../components/UI';
import type {EntityModel} from '../../api';
import {labels} from '../../localization';
import {formatDate} from '../../utils/dateUtils.ts';

type FeeYearPublicationSummary = EntityModel<{
    id: string;
    year: number;
    choiceDeadline: string;
}>;

export const FeeYearPublicationsPage = (): ReactElement => {
    const {isLoading, error, route} = useHalPageData();

    if (isLoading) {
        return <Skeleton/>;
    }

    if (error) {
        return <Alert severity="error">{error.message}</Alert>;
    }

    return (
        <div className="flex flex-col gap-8">
            <h1 className="text-3xl font-bold text-text-primary">{labels.sections.feeYearPublicationsList}</h1>

            <div className="flex flex-col gap-4">
                <div className="flex items-center justify-between">
                    <h2 className="text-xl font-bold text-text-primary">{labels.sections.feeYearPublicationsList}</h2>
                    <HalFormButton name="publishFeeYear" modal={true}/>
                </div>
                <HalEmbeddedTable<FeeYearPublicationSummary>
                    collectionName="feeYearPublicationResponseList"
                    tableId="fee-year-publications"
                    defaultOrderBy="year"
                    defaultOrderDirection="desc"
                    onRowClick={route.navigateToResource}
                    emptyMessage="Žádná vypsání pro rok."
                >
                    <TableCell sortable column="year">Rok</TableCell>
                    <TableCell
                        sortable
                        column="choiceDeadline"
                        dataRender={({value}) => typeof value === 'string' ? formatDate(value) : String(value)}
                    >{labels.fields.choiceDeadline}</TableCell>
                </HalEmbeddedTable>
            </div>
        </div>
    );
};
