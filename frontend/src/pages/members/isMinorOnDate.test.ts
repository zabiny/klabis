import {describe, expect, it} from 'vitest';
import {isMinorOnDate} from './registrationAge';

const today = new Date(2026, 8, 30);

describe('isMinorOnDate', () => {
    it('is minor the day before 18th birthday', () => expect(isMinorOnDate('2008-10-01', today)).toBe(true));
    it('is adult on 18th birthday', () => expect(isMinorOnDate('2008-09-30', today)).toBe(false));
    it('is undefined for incomplete or empty date', () => {
        expect(isMinorOnDate('2008-1', today)).toBeUndefined();
        expect(isMinorOnDate('', today)).toBeUndefined();
        expect(isMinorOnDate(undefined, today)).toBeUndefined();
    });
});
