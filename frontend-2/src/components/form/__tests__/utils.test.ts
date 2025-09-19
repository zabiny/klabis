import {getNestedValue, setNestedValue} from '../utils';

describe('Form Utils', () => {
    describe('getNestedValue', () => {
        const testObject = {
            firstName: 'Jan',
            lastName: 'Novák',
            address: {
                street: 'Hlavní 123',
                city: 'Praha',
                coordinates: {
                    lat: 50.0755,
                    lng: 14.4378
                }
            },
            contact: {
                email: 'jan@example.com'
            }
        };

        it('should get simple property value', () => {
            expect(getNestedValue(testObject, 'firstName')).toBe('Jan');
        });

        it('should get nested property value', () => {
            expect(getNestedValue(testObject, 'address.city')).toBe('Praha');
        });

        it('should get deeply nested property value', () => {
            expect(getNestedValue(testObject, 'address.coordinates.lat')).toBe(50.0755);
        });

        it('should return undefined for non-existent property', () => {
            expect(getNestedValue(testObject, 'nonExistent')).toBeUndefined();
        });

        it('should return undefined for non-existent nested property', () => {
            expect(getNestedValue(testObject, 'address.nonExistent')).toBeUndefined();
        });

        it('should return undefined when path leads through non-object', () => {
            expect(getNestedValue(testObject, 'firstName.someProperty')).toBeUndefined();
        });
    });

    describe('setNestedValue', () => {
        it('should set simple property value', () => {
            const original = {name: 'Jan'};
            const result = setNestedValue(original, 'name', 'Petr');

            expect(result).toEqual({name: 'Petr'});
            expect(original).toEqual({name: 'Jan'}); // Original should not be mutated
        });

        it('should set nested property value', () => {
            const original = {
                user: {
                    name: 'Jan',
                    email: 'jan@example.com'
                }
            };
            const result = setNestedValue(original, 'user.name', 'Petr');

            expect(result).toEqual({
                user: {
                    name: 'Petr',
                    email: 'jan@example.com'
                }
            });
            expect(original.user.name).toBe('Jan'); // Original should not be mutated
        });

        it('should create nested structure if it does not exist', () => {
            const original = {};
            const result = setNestedValue(original, 'user.address.city', 'Praha');

            expect(result).toEqual({
                user: {
                    address: {
                        city: 'Praha'
                    }
                }
            });
        });

        it('should handle deeply nested paths', () => {
            const original = {data: {}};
            const result = setNestedValue(original, 'data.user.profile.settings.theme', 'dark');

            expect(result).toEqual({
                data: {
                    user: {
                        profile: {
                            settings: {
                                theme: 'dark'
                            }
                        }
                    }
                }
            });
        });
    });
});
