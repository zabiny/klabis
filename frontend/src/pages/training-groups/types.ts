import type {HalFormsTemplate, HalResourceLinks, HalResponse} from '../../api';

export interface TrainingGroupSummary {
    id: string;
    name: string;
    minAge: number;
    maxAge: number;
    memberCount: number;
    _links: { self: { href: string } };
}

export interface TrainingGroupMember {
    memberId: string;
    joinedAt: string;
    _links: { member?: HalResourceLinks; self?: HalResourceLinks };
    _templates?: { [name: string]: HalFormsTemplate };
}

export interface TrainingGroupOwner {
    memberId: string;
    _links: { member?: HalResourceLinks };
}

export interface TrainingGroupDetail extends HalResponse {
    id: string;
    name: string;
    minAge: number;
    maxAge: number;
    owners?: TrainingGroupOwner[];
    members?: TrainingGroupMember[];
}
