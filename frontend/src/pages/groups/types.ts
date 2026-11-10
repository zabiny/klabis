import type {HalResourceLinks, HalResponse} from '../../api';

export interface PendingInvitation extends HalResponse {
    groupId: string;
    groupName: string;
    invitationId: string;
    invitedBy: string;
    delegatedAuthorities?: string[];
    _links: {
        self?: HalResourceLinks;
        accept?: { href: string };
        reject?: { href: string };
        invitedMember?: HalResourceLinks;
    };
}
