import {describe, expect, it} from 'vitest';
import {toQuery, type SyncAttemptFilters} from './syncAttemptsQuery';

const empty: SyncAttemptFilters = {entityType: '', outcome: '', dateFrom: '', dateTo: '', includeUnchanged: false};

describe('sync attempts toQuery', () => {
    it('sends nothing for empty filters', () => {
        expect(toQuery(empty)).toEqual({});
    });

    it('sends entityType, outcome and includeUnchanged when set', () => {
        expect(toQuery({...empty, entityType: 'MEMBER', outcome: 'FAILED', includeUnchanged: true}))
            .toEqual({entityType: 'MEMBER', outcome: 'FAILED', includeUnchanged: 'true'});
    });

    it('converts dates to local day instants, with an exclusive end', () => {
        expect(toQuery({...empty, dateFrom: '2026-01-10', dateTo: '2026-01-12'})).toEqual({
            from: new Date(2026, 0, 10).toISOString(),
            to: new Date(2026, 0, 13).toISOString(),
        });
    });

    it('ignores invalid dates', () => {
        expect(toQuery({...empty, dateFrom: 'garbage', dateTo: 'nope'})).toEqual({});
    });
});
