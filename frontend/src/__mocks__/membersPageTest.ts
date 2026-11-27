import {mockHalFormsTemplate} from './halData';
import type {HalResponse} from '../api';

export const MEMBERS_URL = 'http://localhost/api/members';

export const plainMembersResource: HalResponse = {_links: {self: {href: MEMBERS_URL}}};

export const manageMembersResource: HalResponse = {
    _links: {self: {href: MEMBERS_URL}},
    _templates: {
        default: mockHalFormsTemplate({title: 'Get Members', method: 'PATCH'}),
        registerMember: mockHalFormsTemplate({title: 'Registrovat člena', method: 'POST'}),
    },
};

export const buildMemberRow = (overrides?: Record<string, unknown>) => ({
    id: 'member-1',
    registrationNumber: 'REG-001',
    lastName: 'Novák',
    firstName: 'Jan',
    active: true,
    _links: {self: {href: '/api/members/member-1'}},
    ...overrides,
});

export const membersCollection = (members: unknown[], base: HalResponse = {_links: {self: {href: MEMBERS_URL}}}): HalResponse => ({
    ...base,
    _embedded: {memberSummaryResponseList: members},
    page: {size: 10, totalElements: members.length, totalPages: 1, number: 0},
});
