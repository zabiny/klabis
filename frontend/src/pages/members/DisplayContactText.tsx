import {labels} from "../../localization";
import type {components} from "../../api/klabisApi";

export type DisplayContactValue = components['schemas']['DisplayContactValue'];

// fromGuardian/guardianName are sent only to MEMBERS:MANAGE, so the marker is naturally admin-only.
export const DisplayContactText = ({contact}: { contact?: DisplayContactValue }) => {
    if (!contact?.value) return <>{'\u2014'}</>;
    return (
        <>
            {contact.value}
            {contact.fromGuardian && (
                <span className="ml-2 text-xs text-text-tertiary">{labels.ui.guardianContact(contact.guardianName)}</span>
            )}
        </>
    );
};
