import type {ReactElement} from 'react';
import {getPermissionInfo, labels} from '../../localization';

const toLabels = (authorities: readonly string[]): string =>
    authorities.map(authority => getPermissionInfo(authority)?.label ?? authority).join(', ');

interface DelegatedAuthoritiesNoticeProps {
    authorities: readonly string[] | undefined;
    audience: 'owners' | 'invitee';
}

/**
 * Renders nothing when the backend sent no list at all: an absent value must not read as
 * the explicit statement "owners gain nothing".
 */
export const DelegatedAuthoritiesNotice = ({authorities, audience}: DelegatedAuthoritiesNoticeProps): ReactElement | null => {
    if (!authorities) return null;

    const {ownersMayPrefix, ownersGainNothing, invitationGrantsPrefix, invitationGrantsNothing} = labels.groupDelegation;
    const text = authorities.length === 0
        ? (audience === 'invitee' ? invitationGrantsNothing : ownersGainNothing)
        : `${audience === 'invitee' ? invitationGrantsPrefix : ownersMayPrefix} ${toLabels(authorities)}`;

    return <p className="text-sm text-text-secondary" data-testid="delegated-authorities-notice">{text}</p>;
};

export const DelegatedAuthoritiesReadOnly = ({authorities}: {authorities: readonly string[] | undefined}): ReactElement | null => {
    if (!authorities) return null;

    return (
        <div className="flex flex-col gap-1 mb-4" data-testid="delegated-authorities-readonly">
            <span className="text-sm font-medium text-text-primary">{labels.fields.delegatedAuthorities}</span>
            <span className="text-sm text-text-primary">
                {authorities.length === 0 ? labels.groupDelegation.noneDelegated : toLabels(authorities)}
            </span>
            <span className="text-xs text-text-secondary">{labels.groupDelegation.fixedAtCreation}</span>
        </div>
    );
};
