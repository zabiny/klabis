import type {HalResourceLinks} from '../../api';

export interface PendingInvitation {
    groupId: string;
    groupName: string;
    invitationId: string;
    invitedBy: string;
    _links: {
        self?: HalResourceLinks;
        accept?: { href: string };
        reject?: { href: string };
        invitedMember?: HalResourceLinks;
    };
}
