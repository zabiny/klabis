import {type ReactElement} from 'react';
import {useHalPageData} from '../../hooks/useHalPageData.ts';
import {Alert, Skeleton} from '../../components/UI';
import {ActiveCampaignSection} from '../../components/membership-fees/ActiveCampaignSection.tsx';
import {TierCatalogSection} from '../../components/membership-fees/TierCatalogSection.tsx';
import {PastCampaignsSection} from '../../components/membership-fees/PastCampaignsSection.tsx';
import type {HalResponse} from '../../api';
import {labels} from '../../localization';
import {HalFormButton} from "../../components/HalNavigator2/HalFormButton.tsx";

interface TierCatalogResource extends HalResponse {
    _links: {
        self: { href: string };
        activeCampaign?: { href: string };
        pastCampaigns?: { href: string };
    };
}

export const MembershipFeesAdminPage = (): ReactElement => {
    const {resourceData, isLoading, error} = useHalPageData<TierCatalogResource>();

    if (isLoading) {
        return <Skeleton/>;
    }

    if (error) {
        return <Alert severity="error">{error.message}</Alert>;
    }

    if (!resourceData) {
        return <Skeleton/>;
    }

    const activeCampaignHref = resourceData._links?.activeCampaign?.href ?? null;
    const pastCampaignsHref = resourceData._links?.pastCampaigns?.href ?? null;

    const activeCampaign = () => {
        if (activeCampaignHref) {
            return <ActiveCampaignSection activeCampaignHref={activeCampaignHref}/>;
        } else {
            return <HalFormButton name={"publishYear"}/>;
        }
    }

    return (
        <div className="flex flex-col gap-8">
            <h1 className="text-[28px] font-bold text-[#18181B]">{labels.nav['membership-fees']}</h1>

            {activeCampaign()}

            <TierCatalogSection/>

            <PastCampaignsSection pastCampaignsHref={pastCampaignsHref}/>
        </div>
    );
};
