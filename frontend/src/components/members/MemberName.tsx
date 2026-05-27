import {type ReactElement} from "react";
import {useHalRoute} from "../../contexts/HalRouteContext.tsx";

interface MemberNameProps {
    user?: { firstName: string, lastName: string }
}

export const MemberName = ({user}: MemberNameProps): ReactElement => {
    const {resourceData, error} = useHalRoute();

    if (!user) {
        const data = resourceData as { firstName?: string, lastName?: string } | null;
        if (error || !data?.firstName || !data?.lastName) {
            return <span className="text-text-primary">—</span>;
        }
        return <span className="text-text-primary">{data.firstName} {data.lastName}</span>;
    }

    if (!user.firstName || !user.lastName) {
        return <span className="text-text-primary">—</span>;
    }
    return <span className="text-text-primary">{user.firstName} {user.lastName}</span>;
}
