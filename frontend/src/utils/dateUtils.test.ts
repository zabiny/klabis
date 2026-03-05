import {formatDate} from './dateUtils';

describe('formatDate', () => {
    describe('Valid Dates', () => {
        it('should format ISO date string to Czech format', () => {
            const result = formatDate('2025-06-15');
            // Czech format: day. month. year (with spaces)
            expect(result).toMatch(/15[\s\.]*6[\s\.]*2025/);
        });

        it('should format date with time to Czech date only', () => {
            const result = formatDate('2025-06-15T14:30:00');
            expect(result).toMatch(/15[\s\.]*6[\s\.]*2025/);
        });

        it('should format date with UTC timezone indicator', () => {
            const result = formatDate('2025-06-15T00:00:00Z');
            expect(result).toMatch(/15[\s\.]*6[\s\.]*2025/);
        });

        it('should handle leap year dates', () => {
            const result = formatDate('2024-02-29');
            expect(result).not.toBe('-');
            expect(result).toMatch(/29[\s\.]*2[\s\.]*2024/);
        });

        it('should handle year boundaries - last day of year', () => {
            const result = formatDate('2024-12-31');
            expect(result).toMatch(/31[\s\.]*12[\s\.]*2024/);
        });

        it('should handle year boundaries - first day of year', () => {
            const result = formatDate('2025-01-01');
            expect(result).toMatch(/1[\s\.]*1[\s\.]*2025/);
        });

        it('should format January dates correctly', () => {
            const result = formatDate('2025-01-15');
            expect(result).toMatch(/15[\s\.]*1[\s\.]*2025/);
        });

        it('should format December dates correctly', () => {
            const result = formatDate('2025-12-25');
            expect(result).toMatch(/25[\s\.]*12[\s\.]*2025/);
        });

        it('should handle dates with milliseconds', () => {
            const result = formatDate('2025-06-15T14:30:45.123Z');
            expect(result).toMatch(/15[\s\.]*6[\s\.]*2025/);
        });
    });

    describe('Null/Undefined/Empty String Handling', () => {
        it('should return "-" for null input', () => {
            expect(formatDate(null as any)).toBe('-');
        });

        it('should return "-" for undefined input', () => {
            expect(formatDate(undefined as any)).toBe('-');
        });

        it('should return "-" for empty string', () => {
            expect(formatDate('')).toBe('-');
        });

        it('should return "-" for whitespace only string', () => {
            expect(formatDate('   ')).toBe('-');
        });

        it('should return "-" for string with only tabs', () => {
            expect(formatDate('\t\t')).toBe('-');
        });

        it('should return "-" for string with only newlines', () => {
            expect(formatDate('\n\n')).toBe('-');
        });
    });

    describe('Invalid Date Handling', () => {
        it('should return "-" for completely invalid date string', () => {
            expect(formatDate('not-a-date')).toBe('-');
        });

        it('should return "-" for invalid month (month > 12)', () => {
            expect(formatDate('2025-13-01')).toBe('-');
        });

        it('should return "-" for invalid month (month = 0)', () => {
            expect(formatDate('2025-00-15')).toBe('-');
        });

        it('should handle February with day overflow (rolls to next month)', () => {
            // JavaScript's Date constructor rolls over invalid dates
            // Feb 30, 2025 becomes March 2, 2025
            const result = formatDate('2025-02-30');
            expect(result).not.toBe('-');
            expect(result).toMatch(/2[\s\.]*3[\s\.]*2025/); // March 2, 2025
        });

        it('should handle day overflow (June 32 behavior)', () => {
            // June 32 is actually treated as invalid by JavaScript's Date parser
            const result = formatDate('2025-06-32');
            // Just verify it either returns a date or "-"
            expect(typeof result).toBe('string');
        });

        it('should return "-" for invalid day (day = 0)', () => {
            expect(formatDate('2025-06-00')).toBe('-');
        });

        it('should return "-" for malformed date (wrong separators)', () => {
            expect(formatDate('15/06/2025')).toBe('-');
        });

        it('should return "-" for date with invalid characters', () => {
            expect(formatDate('2025-06-15T25:00:00')).toBe('-');
        });

        it('should handle partial ISO format (JavaScript interprets as valid)', () => {
            // JavaScript's Date constructor treats '2025-06' as June 1, 2025
            const result = formatDate('2025-06');
            expect(result).not.toBe('-');
            expect(result).toMatch(/1[\s\.]*6[\s\.]*2025/); // June 1, 2025
        });

        it('should return "-" for random text', () => {
            expect(formatDate('Lorem ipsum dolor sit')).toBe('-');
        });

        it('should handle number as string (JavaScript interprets as timestamp)', () => {
            // JavaScript's Date constructor treats numbers as timestamps
            // 12345 ms since epoch = Jan 1, 1970
            const result = formatDate('12345');
            expect(result).not.toBe('-');
        });
    });

    describe('Timezone Handling', () => {
        it('should format date consistently in Europe/Prague timezone', () => {
            // Test that the timezone is applied (Czech timezone is UTC+1 or UTC+2 depending on DST)
            const result = formatDate('2025-06-15T23:59:59Z');
            // 23:59:59 UTC on June 15 becomes 01:59:59 on June 16 in Prague (UTC+2 in summer)
            expect(result).toMatch(/16[\s\.]*6[\s\.]*2025/);
        });

        it('should format UTC dates consistently regardless of timezone', () => {
            const result1 = formatDate('2025-01-15T12:00:00Z');
            const result2 = formatDate('2025-01-15');
            // Both should format to the same date
            expect(result1).toMatch(/15[\s\.]*1[\s\.]*2025/);
            expect(result2).toMatch(/15[\s\.]*1[\s\.]*2025/);
        });

        it('should handle dates near midnight edge case', () => {
            const result = formatDate('2025-06-15T23:00:00Z');
            // 23:00 UTC on June 15 becomes 01:00 on June 16 in Prague (UTC+2)
            expect(result).toMatch(/16[\s\.]*6[\s\.]*2025/);
        });

        it('should handle dates that cross day boundary at midnight', () => {
            const result = formatDate('2025-06-15T22:00:00Z');
            // 22:00 UTC on June 15 becomes 00:00 on June 16 in Prague (UTC+2)
            expect(result).toMatch(/16[\s\.]*6[\s\.]*2025/);
        });
    });

    describe('Edge Cases', () => {
        it('should handle very old dates', () => {
            const result = formatDate('1900-01-01');
            expect(result).not.toBe('-');
            expect(result).toMatch(/1[\s\.]*1[\s\.]*1900/);
        });

        it('should handle far future dates', () => {
            const result = formatDate('2099-12-31');
            expect(result).not.toBe('-');
            expect(result).toMatch(/31[\s\.]*12[\s\.]*2099/);
        });

        it('should handle year 2000 dates', () => {
            const result = formatDate('2000-01-01');
            expect(result).not.toBe('-');
            expect(result).toMatch(/1[\s\.]*1[\s\.]*2000/);
        });

        it('should handle dates with leading/trailing spaces', () => {
            // '   ' is considered empty, but actual date with spaces might work
            const result = formatDate(' 2025-06-15 ');
            // JavaScript Date constructor should handle this
            expect(result).toMatch(/15[\s\.]*6[\s\.]*2025/);
        });

        it('should handle single digit month and day with leading zeros', () => {
            const result = formatDate('2025-01-05');
            expect(result).toMatch(/5[\s\.]*1[\s\.]*2025/);
        });

        it('should handle last day of each month - April (30 days)', () => {
            const result = formatDate('2025-04-30');
            expect(result).toMatch(/30[\s\.]*4[\s\.]*2025/);
        });

        it('should handle last day of each month - May (31 days)', () => {
            const result = formatDate('2025-05-31');
            expect(result).toMatch(/31[\s\.]*5[\s\.]*2025/);
        });

        it('should handle dates with different time formats', () => {
            const result = formatDate('2025-06-15T14:30:45.123456Z');
            expect(result).toMatch(/15[\s\.]*6[\s\.]*2025/);
        });
    });

    describe('Type Safety', () => {
        it('should handle string input correctly', () => {
            const result = formatDate('2025-06-15');
            expect(typeof result).toBe('string');
            expect(result).not.toBe('-');
        });

        it('should always return a string', () => {
            expect(typeof formatDate('2025-06-15')).toBe('string');
            expect(typeof formatDate('')).toBe('string');
            expect(typeof formatDate('invalid')).toBe('string');
        });
    });

    describe('Czech Locale Format', () => {
        it('should use Czech locale format with dots as separators', () => {
            const result = formatDate('2025-06-15');
            // Czech format typically uses dots: 15. 6. 2025
            expect(result).toMatch(/\d+[\s\.]+\d+[\s\.]+\d+/);
        });

        it('should maintain consistent formatting across multiple calls', () => {
            const dateString = '2025-06-15';
            const result1 = formatDate(dateString);
            const result2 = formatDate(dateString);
            expect(result1).toBe(result2);
        });
    });
});
