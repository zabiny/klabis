import {describe, it, expect} from 'vitest';
import {getActionVariant} from './actionVariants';

describe('getActionVariant', () => {
    it('maps registerForEvent to primary', () => {
        expect(getActionVariant('registerForEvent')).toBe('primary');
    });

    it('maps publishEvent to primary', () => {
        expect(getActionVariant('publishEvent')).toBe('primary');
    });

    it('maps newRegistration to primary', () => {
        expect(getActionVariant('newRegistration')).toBe('primary');
    });

    it('maps unregisterFromEvent to warning', () => {
        expect(getActionVariant('unregisterFromEvent')).toBe('warning');
    });

    it('maps cancelEvent to danger', () => {
        expect(getActionVariant('cancelEvent')).toBe('danger');
    });

    it('maps updateEvent to secondary', () => {
        expect(getActionVariant('updateEvent')).toBe('secondary');
    });

    it('maps syncEventFromOris to secondary', () => {
        expect(getActionVariant('syncEventFromOris')).toBe('secondary');
    });

    it('falls back to secondary for unknown affordance names', () => {
        expect(getActionVariant('totallyUnknown')).toBe('secondary');
    });
});
