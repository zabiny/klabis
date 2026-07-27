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

export interface TrainingGroupTrainer {
    memberId: string;
    _links: { member?: HalResourceLinks; self?: HalResourceLinks };
    _templates?: { [name: string]: HalFormsTemplate };
}

export interface TrainingGroupAgeRange {
    minAge: number | null;
    maxAge: number | null;
}

export interface TrainingGroupDetail extends HalResponse {
    id: string;
    name: string;
    ageRange: TrainingGroupAgeRange | null;
    trainers?: TrainingGroupTrainer[];
    members?: TrainingGroupMember[];
}
