import {describe, expect, it} from 'vitest';
import {toQuery} from './membersQuery';

describe('members toQuery', () => {
    it('always sends status', () => {
        expect(toQuery({status: 'ACTIVE', q: '', incomplete: false})).toEqual({status: 'ACTIVE'});
    });

    it('sends q only with at least 2 characters', () => {
        expect(toQuery({status: 'ALL', q: 'n', incomplete: false})).toEqual({status: 'ALL'});
        expect(toQuery({status: 'ALL', q: 'no', incomplete: false})).toEqual({status: 'ALL', q: 'no'});
    });

    it('sends incomplete only when set', () => {
        expect(toQuery({status: 'INACTIVE', q: '', incomplete: true})).toEqual({status: 'INACTIVE', incomplete: 'true'});
    });
});
