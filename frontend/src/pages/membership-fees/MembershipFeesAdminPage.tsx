import {type ReactElement} from 'react';
import {HalSubresourceProvider} from '../../contexts/HalRouteContext.tsx';
import {CampaignDetail} from '../../components/membership-fees/CampaignDetail.tsx';
import {TypeCatalogSection} from '../../components/membership-fees/TypeCatalogSection.tsx';
import {PastCampaignsSection} from '../../components/membership-fees/PastCampaignsSection.tsx';
import type {ListFeeTypesResource} from '../../api';
import {toHref} from '../../api/hateoas.ts';
import {labels} from '../../localization';
import {HalFormButton} from '../../components/HalNavigator2/HalFormButton.tsx';
import {HalPageGate} from '../HalPageGate.tsx';

const MembershipFeesAdminContent = ({resourceData}: {resourceData: ListFeeTypesResource}): ReactElement => {
    const activeCampaignHref = resourceData._links?.activeCampaign
        ? toHref(resourceData._links.activeCampaign) : null;
    const pastCampaignsHref = resourceData._links?.pastCampaigns
        ? toHref(resourceData._links.pastCampaigns) : null;

    return (
        <div className="flex flex-col gap-8">
            <h1 className="text-[28px] font-bold text-text-primary">{labels.nav['membership-fees']}</h1>

            {activeCampaignHref ? (
                <div className="flex flex-col gap-4">
                    <h2 className="text-xl font-bold text-text-primary">{labels.sections.activeCampaign}</h2>
                    <HalSubresourceProvider subresourceLinkName="activeCampaign">
                        <CampaignDetail/>
                    </HalSubresourceProvider>
                </div>
            ) : (
                <div className="flex">
                    <HalFormButton name="publishYear"/>
                </div>
            )}

            <TypeCatalogSection/>

            <PastCampaignsSection pastCampaignsHref={pastCampaignsHref}/>
        </div>
    );
};

export const MembershipFeesAdminPage = (): ReactElement => (
    <HalPageGate<ListFeeTypesResource>>
        {(resourceData) => <MembershipFeesAdminContent resourceData={resourceData}/>}
    </HalPageGate>
);
