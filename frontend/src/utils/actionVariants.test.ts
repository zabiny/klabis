import {describe, it, expect} from 'vitest';
import {getActionVariant} from './actionVariants';

describe('getActionVariant', () => {
    it('maps registerForEvent to primary-ghost', () => {
        expect(getActionVariant('registerForEvent')).toBe('primary-ghost');
    });

    it('maps publishEvent to primary-ghost', () => {
        expect(getActionVariant('publishEvent')).toBe('primary-ghost');
    });

    it('maps newRegistration to primary-ghost', () => {
        expect(getActionVariant('newRegistration')).toBe('primary-ghost');
    });

    it('maps unregisterFromEvent to warning-ghost', () => {
        expect(getActionVariant('unregisterFromEvent')).toBe('warning-ghost');
    });

    it('maps cancelEvent to danger-ghost', () => {
        expect(getActionVariant('cancelEvent')).toBe('danger-ghost');
    });

    it('maps updateEvent to ghost', () => {
        expect(getActionVariant('updateEvent')).toBe('ghost');
    });

    it('maps syncEventFromOris to ghost', () => {
        expect(getActionVariant('syncEventFromOris')).toBe('ghost');
    });

    it('falls back to ghost for unknown affordance names', () => {
        expect(getActionVariant('totallyUnknown')).toBe('ghost');
    });
});
