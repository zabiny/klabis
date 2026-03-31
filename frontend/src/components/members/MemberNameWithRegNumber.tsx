import {type ReactElement} from "react";
import {useHalRoute} from "../../contexts/HalRouteContext.tsx";

interface MemberData {
    firstName: string;
    lastName: string;
    registrationNumber?: string;
}

export const MemberNameWithRegNumber = (): ReactElement => {
    const {resourceData} = useHalRoute();
    const member = resourceData as MemberData | null;
    const firstName = member?.firstName || '-';
    const lastName = member?.lastName || '-';
    const regNumber = member?.registrationNumber;

    return (
        <span className="text-text-primary">
            {firstName} {lastName}{regNumber ? ` (${regNumber})` : ''}
        </span>
    );
};
