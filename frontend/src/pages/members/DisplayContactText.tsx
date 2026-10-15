import {labels} from "../../localization";
import {InfoHint} from "../../components/UI/InfoHint";
import type {components} from "../../api/klabisApi";

export type DisplayContactValue = components['schemas']['DisplayContactValue'];

// fromGuardian/guardianName are sent only to MEMBERS:MANAGE, so the marker is naturally admin-only.
export const DisplayContactText = ({contact}: { contact?: DisplayContactValue }) => {
    if (!contact?.value) return <>{'\u2014'}</>;
    return (
        <>
            {contact.value}
            {contact.fromGuardian && (
                <InfoHint className="ml-1.5" text={labels.ui.guardianContact(contact.guardianName)}/>
            )}
        </>
    );
};
