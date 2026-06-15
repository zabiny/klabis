import {type ReactElement} from 'react';
import {ArrowRight, Plus} from 'lucide-react';
import {useHalPageData} from '../../hooks/useHalPageData.ts';
import {HalEmbeddedTable} from '../HalNavigator2/HalEmbeddedTable.tsx';
import {HalFormButton} from '../HalNavigator2/HalFormButton.tsx';
import {TableCell} from '../KlabisTable';
import type {EntityModel} from '../../api';
import {labels} from '../../localization';

type FeeTierSummary = EntityModel<{
    id: string;
    name: string;
    yearlyFeeAmount: number;
    yearlyFeeCurrency: string;
}>;

export const TierCatalogSection = (): ReactElement => {
    const {route} = useHalPageData();

    return (
        <div className="flex flex-col gap-4">
            <div className="flex items-center justify-between">
                <h2 className="text-xl font-bold text-[#18181B]">{labels.sections.membershipFeeTiersList}</h2>
                <HalFormButton
                    name="createTier"
                    modal={true}
                    icon={<Plus size={16} className="text-white"/>}
                    className="flex items-center gap-1.5 bg-[#2563EB] hover:bg-[#1D4ED8] text-white text-[13px] font-bold h-[38px] px-3.5 rounded-lg"
                />
            </div>

            <div className="bg-white border border-[#E4E4E7] rounded-xl overflow-hidden">
                <HalEmbeddedTable<FeeTierSummary>
                    collectionName="membershipFeeTierSummaryResponseList"
                    tableId="membership-fee-tiers"
                    defaultOrderBy="name"
                    onRowClick={route.navigateToResource}
                    emptyMessage="Žádné tiery členských příspěvků."
                >
                    <TableCell
                        sortable
                        column="name"
                        dataRender={({item}) => {
                            const tier = item as unknown as FeeTierSummary;
                            return (
                                <span className="text-sm font-bold text-[#18181B]">{tier.name}</span>
                            );
                        }}
                    >
                        {labels.fields.name}
                    </TableCell>
                    <TableCell sortable column="yearlyFeeAmount">
                        {labels.fields.yearlyFeeAmount}
                    </TableCell>
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
