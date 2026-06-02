import {type ReactElement} from "react";
import {useHalRoute} from "../../contexts/halRouteContext.ts";

interface MemberNameProps {
    user?: { firstName: string, lastName: string }
}

export const MemberName = ({user}: MemberNameProps): ReactElement => {
    const {resourceData, error} = useHalRoute();
    const source = user ?? (error ? null : resourceData as { firstName?: string, lastName?: string } | null);
    const display = source?.firstName && source?.lastName
        ? `${source.firstName} ${source.lastName}`
        : '—';
    return <span className="text-text-primary">{display}</span>;
}
