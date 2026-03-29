import {type ReactElement} from "react";
import {useHalRoute} from "../../contexts/HalRouteContext.tsx";

interface MemberNameProps {
    user?: { firstName: string, lastName: string }
}

export const MemberName = ({user}: MemberNameProps): ReactElement => {
    const {resourceData} = useHalRoute();
    if (!user) {
        user = resourceData as { firstName: string, lastName: string };
    }
    return <span className="text-text-primary">{user?.firstName || '-'} {user?.lastName || '-'}</span>
}
