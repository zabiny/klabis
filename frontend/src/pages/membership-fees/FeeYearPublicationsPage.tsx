import {type ReactElement} from 'react';
import {ArrowRight, Plus} from 'lucide-react';
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
    votingDeadline: string;
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
        <div className="flex flex-col gap-6">
            <div className="flex items-center justify-between">
                <div>
                    <h1 className="text-[28px] font-bold text-[#18181B]">{labels.sections.feeYearPublicationsList}</h1>
                    <p className="text-sm text-[#71717A] mt-0.5">Vypsání výběru úrovní příspěvků pro jednotlivé roky</p>
                </div>
                <HalFormButton
                    name="publishYear"
                    modal={true}
                    icon={<Plus size={16} className="text-white"/>}
                    className="flex items-center gap-1.5 bg-[#2563EB] hover:bg-[#1D4ED8] text-white text-[13px] font-bold h-[38px] px-3.5 rounded-lg"
                />
            </div>

            <div className="bg-white border border-[#E4E4E7] rounded-xl overflow-hidden">
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
                        column="votingDeadline"
                        dataRender={({value}) => typeof value === 'string' ? formatDate(value) : String(value)}
                    >{labels.fields.votingDeadline}</TableCell>
                    <TableCell
                        column="_links"
                        dataRender={() => (
                            <button
                                type="button"
                                className="flex items-center justify-center w-8 h-8 rounded-[6px] bg-[#F8FAFC] text-[#71717A] hover:bg-[#F1F5F9]"
                                aria-label="Otevřít detail"
                            >
                                <ArrowRight size={16}/>
                            </button>
                        )}
                    >
                        {''}
                    </TableCell>
                </HalEmbeddedTable>
            </div>
        </div>
    );
};
