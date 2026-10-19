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
            <div className="flex items-center gap-2 text-sm text-text-secondary">
                <Link to="/membership-fee-tiers" className="hover:text-text-primary">
                    {labels.ui.backToList}
                </Link>
                <span>/</span>
                <span className="text-text-primary font-medium">{resourceData?.year}</span>
            </div>

            <h1 className="text-[28px] font-bold text-text-primary">{resourceData?.year}</h1>

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
