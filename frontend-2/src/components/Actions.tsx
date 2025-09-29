import React from "react";

export const Actions = ({value}: { value: string[] }): React.ReactNode => {
    return (
        <span>{value.map(s => <div>{s}</div>)}</span>
    );
}