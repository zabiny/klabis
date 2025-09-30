import {type ReactNode} from "react";
import {Newspaper, SyncSharp} from "@mui/icons-material";
import {Tooltip} from "@mui/material";

const addTooltip = (text: string, node: ReactNode): ReactNode => {
    return <Tooltip title={text}><span>{node}</span></Tooltip>;
}

const createIcon = (actionName: string): ReactNode => {
    switch (actionName) {
        case 'synchronize':
            return addTooltip(actionName, <SyncSharp/>);
        case 'createRegistration':
            return addTooltip(actionName, <Newspaper/>);
        default:
            return <>{actionName}</>;
    }
}

export const Actions = ({value}: { value: string[] }): React.ReactNode => {
    return (
        <span>{value.map(s => <div key={s}><Tooltip title={s}><span>{createIcon(s)}</span></Tooltip></div>)}</span>
    );
}