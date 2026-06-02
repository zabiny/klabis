import React, {type ReactElement} from 'react';

export const FormGroupWrapper: React.FC<{ label: string; children: ReactElement | ReactElement[] }> = ({label, children}) => (
    <div className="rounded p-4 mb-4">
        <label className="block text-sm font-semibold mb-2">{label}</label>
        <div className="space-y-3">{children}</div>
    </div>
);
