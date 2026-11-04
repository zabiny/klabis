import type {GetTrainingGroupResource, HalResourceLinks, HalResponse} from '../../api';

export interface TrainingGroupSummary {
    id: string;
    name: string;
    minAge: number;
    maxAge: number;
    memberCount: number;
    _links: { self: { href: string } };
}

export interface TrainingGroupMember extends HalResponse {
    memberId: string;
    joinedAt: string;
    _links: { member?: HalResourceLinks; self?: HalResourceLinks };
}

export interface TrainingGroupTrainer extends HalResponse {
    memberId: string;
    _links: { member?: HalResourceLinks; self?: HalResourceLinks };
}

// GetTrainingGroupResource types name / ageRange / _templates from the spec. Its
// trainers/members arrays carry per-row _links / _templates added at runtime
// (member link, removeTrainingGroupMember, removeTrainer) that the generated schema does not
// describe, so those rows keep the view types above.
export type TrainingGroupDetail = Omit<GetTrainingGroupResource, 'trainers' | 'members'> & {
    trainers?: TrainingGroupTrainer[];
    members?: TrainingGroupMember[];
};
