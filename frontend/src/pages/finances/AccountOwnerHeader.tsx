import {type ReactElement} from "react";
import {Skeleton} from "../../components/UI";
import {HalSubresourceProvider, useHalRoute} from "../../contexts/HalRouteContext.tsx";
import {useHalPageData} from "../../hooks/useHalPageData.ts";

interface OwnerIdentityProps {
    firstName: string;
    lastName: string;
    registrationNumber?: string;
}

const OwnerIdentity = ({firstName, lastName, registrationNumber}: OwnerIdentityProps): ReactElement => {
    return (
        <span className="text-text-primary font-semibold">
            {firstName} {lastName}{registrationNumber ? ` (${registrationNumber})` : ''}
        </span>
    );
};

const AccountOwnerContent = (): ReactElement => {
    const {resourceData, isLoading} = useHalRoute();

    if (isLoading) {
        return <Skeleton className="h-6 w-48" />;
    }

    if (!resourceData) {
        return <></>;
    }

    const owner = resourceData as {firstName?: string; lastName?: string; registrationNumber?: string};
    return (
        <OwnerIdentity
            firstName={owner.firstName ?? '-'}
            lastName={owner.lastName ?? '-'}
            registrationNumber={owner.registrationNumber}
        />
    );
};

/**
 * Displays the account owner's identity (name + registration number) by following
 * the `accountOwner` HAL link from the current resource context.
 * Renders nothing if the link is absent (graceful degradation).
 */
export const AccountOwnerHeader = (): ReactElement => {
    const {resourceData} = useHalPageData();
    const accountOwnerLink = resourceData?._links?.accountOwner;

    if (!accountOwnerLink) {
        return <></>;
    }

    return (
        <div data-testid="account-owner-header" className="flex items-center gap-2">
            <HalSubresourceProvider subresourceLinkName="accountOwner">
                <AccountOwnerContent />
            </HalSubresourceProvider>
        </div>
    );
};
