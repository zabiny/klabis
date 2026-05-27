import {type ReactElement} from "react";
import {HalSubresourceProvider} from "../../contexts/HalRouteContext.tsx";
import {useHalPageData} from "../../hooks/useHalPageData.ts";
import {MemberNameWithRegNumber} from "../../components/members/MemberNameWithRegNumber.tsx";

export const AccountOwnerHeader = (): ReactElement | null => {
    const {resourceData} = useHalPageData();
    const accountOwnerLink = resourceData?._links?.accountOwner;

    if (!accountOwnerLink) {
        return null;
    }

    return (
        <div data-testid="account-owner-header">
            <HalSubresourceProvider subresourceLinkName="accountOwner">
                <MemberNameWithRegNumber />
            </HalSubresourceProvider>
        </div>
    );
};
