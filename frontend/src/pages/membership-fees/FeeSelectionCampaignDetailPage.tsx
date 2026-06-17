import {type ReactElement} from 'react';
import {Link} from 'react-router-dom';
import {useHalPageData} from '../../hooks/useHalPageData.ts';
import {Alert, Skeleton} from '../../components/UI';
import {CampaignDetail} from '../../components/membership-fees/CampaignDetail.tsx';
import type {FeeSelectionCampaignDetail} from '../../components/membership-fees/CampaignDetail.tsx';
import {labels} from '../../localization';

const FeeSelectionCampaignDetailContent = (): ReactElement => {
    const {resourceData} = useHalPageData<FeeSelectionCampaignDetail>();

    return (
        <div className="flex flex-col gap-6">
            <div className="flex items-center gap-2 text-sm text-[#71717A]">
                <Link to="/membership-fee-tiers" className="hover:text-[#18181B]">
                    {labels.ui.backToList}
                </Link>
                <span>/</span>
                <span className="text-[#18181B] font-medium">{resourceData?.year}</span>
            </div>

            <h1 className="text-[28px] font-bold text-[#18181B]">{resourceData?.year}</h1>

            <CampaignDetail/>
        </div>
    );
};

export const FeeSelectionCampaignDetailPage = (): ReactElement => {
    const {isLoading, error, resourceData} = useHalPageData<FeeSelectionCampaignDetail>();

    if (isLoading) {
        return <Skeleton/>;
    }

    if (error) {
        return <Alert severity="error">{error.message}</Alert>;
    }

    if (!resourceData) {
        return <Skeleton/>;
    }

    return <FeeSelectionCampaignDetailContent/>;
};
