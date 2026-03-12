import {describe, expect, it} from 'vitest';
import {sanitizeFormValues} from './utils.ts';

describe('sanitizeFormValues', () => {
    it('converts empty string to null for simple optional text field', () => {
        const result = sanitizeFormValues({bankAccountNumber: ''});
        expect(result.bankAccountNumber).toBeNull();
    });

    it('converts empty string to null for simple optional select/enum field', () => {
        const result = sanitizeFormValues({drivingLicenseGroup: ''});
        expect(result.drivingLicenseGroup).toBeNull();
    });

    it('converts empty string to null for complex object type field (GuardianDTO)', () => {
        const result = sanitizeFormValues({guardian: ''});
        expect(result.guardian).toBeNull();
    });

    it('converts empty string to null for complex object type field (IdentityCardDto)', () => {
        const result = sanitizeFormValues({identityCard: ''});
        expect(result.identityCard).toBeNull();
    });

    it('converts empty string to null for complex object type field (MedicalCourseDto)', () => {
        const result = sanitizeFormValues({medicalCourse: ''});
        expect(result.medicalCourse).toBeNull();
    });

    it('preserves non-empty string values unchanged', () => {
        const result = sanitizeFormValues({firstName: 'Jan'});
        expect(result.firstName).toBe('Jan');
    });

    it('preserves null values unchanged', () => {
        const result = sanitizeFormValues({bankAccountNumber: null});
        expect(result.bankAccountNumber).toBeNull();
    });

    it('preserves object values unchanged', () => {
        const obj = {name: 'John', birthDate: '2000-01-01'};
        const result = sanitizeFormValues({guardian: obj});
        expect(result.guardian).toBe(obj);
    });

    it('preserves boolean values unchanged', () => {
        const result = sanitizeFormValues({active: false});
        expect(result.active).toBe(false);
    });

    it('preserves array values unchanged', () => {
        const arr = ['a', 'b'];
        const result = sanitizeFormValues({tags: arr});
        expect(result.tags).toBe(arr);
    });

    it('handles multiple fields in one call', () => {
        const result = sanitizeFormValues({
            firstName: 'Jan',
            bankAccountNumber: '',
            guardian: '',
            identityCard: '',
        });

        expect(result.firstName).toBe('Jan');
        expect(result.bankAccountNumber).toBeNull();
        expect(result.guardian).toBeNull();
        expect(result.identityCard).toBeNull();
    });

    it('converts empty string to null for birthNumber optional field', () => {
        const result = sanitizeFormValues({birthNumber: ''});
        expect(result.birthNumber).toBeNull();
    });
});
