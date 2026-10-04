import {renderHook} from '@testing-library/react';
import type {HalFormsTemplate} from '../api';
import {useInlineEditing} from './useInlineEditing';

const template: HalFormsTemplate = {
    method: 'PATCH',
    properties: [
        {name: 'firstName', type: 'text', readOnly: true},
        {name: 'gender', type: 'Gender', readOnly: true, options: {inline: ['MALE', 'FEMALE']}},
        {name: 'email', type: 'email'},
    ],
};

const resourceData = {firstName: 'Jan', gender: 'MALE', email: 'a@b.cz', registrationNumber: 'ZBM9500'};

describe('useInlineEditing', () => {
    it('treats only non-readOnly template properties as editable', () => {
        const {result} = renderHook(() => useInlineEditing(template, resourceData));

        expect([...result.current.editableFieldNames]).toEqual(['email']);
    });

    it('drops readOnly and non-template fields from the submitted payload', () => {
        const {result} = renderHook(() => useInlineEditing(template, resourceData));

        const payload = result.current.postprocessPayload({
            firstName: 'Jan',
            gender: 'MALE',
            email: 'new@b.cz',
            registrationNumber: 'ZBM9500',
        });

        expect(payload).toEqual({email: 'new@b.cz'});
    });

    it('keeps readOnly template properties in the enriched template so they can be displayed', () => {
        const {result} = renderHook(() => useInlineEditing(template, resourceData, {initialEditing: true}));

        expect(result.current.enrichedFieldNames.has('firstName')).toBe(true);
        expect(result.current.enrichedFieldNames.has('gender')).toBe(true);
    });
});
