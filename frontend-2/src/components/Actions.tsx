import {type ReactNode, useCallback} from "react";
import {Delete, Edit, Newspaper, SyncSharp} from "@mui/icons-material";
import {Link, Stack, Tooltip} from "@mui/material";

const createIcon = (actionName: string): ReactNode | undefined => {
    switch (actionName) {
        case 'synchronize':
            return <SyncSharp/>;
        case 'createRegistration':
            return <Newspaper/>;
        case 'updateRegistration':
            return <Edit/>;
        case 'cancelRegistration':
            return <Delete/>;
        default:
            return undefined;
    }
}

type OnClickHandler = (s: string) => void;

export const Actions = ({
                            value, onClick
                        }: { value?: string[], onClick?: OnClickHandler }): React.ReactNode => {
    return (
        <Stack direction={"row"} spacing={1}>{value && value.map(s => <Action actionName={s}
                                                                              onClick={onClick}/>)}</Stack>
    );
}

const Action = ({actionName, onClick}: { actionName: string, onClick?: OnClickHandler }): ReactNode => {
    const icon = createIcon(actionName);

    const event = useCallback(() => {
        if (onClick) onClick(actionName);
    }, [actionName, onClick]);

    if (icon) {
        return <Tooltip key={actionName} title={actionName}><Link onClick={event}>{icon}</Link></Tooltip>
    } else {
        return <span onClick={event}>{actionName}</span>
    }
}
