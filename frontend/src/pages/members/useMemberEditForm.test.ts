import {renderHook, act} from '@testing-library/react';
import {vi, describe, it, expect, beforeEach} from 'vitest';
import type {HalFormsTemplate, HalResponse} from '../../api';
import {useMemberEditForm} from './useMemberEditForm';

vi.mock('../../hooks/useAuthorizedFetch', () => ({
    useAuthorizedMutation: vi.fn(() => ({
        mutate: vi.fn(),
        isPending: false,
        error: null,
    })),
}));

vi.mock('../../hooks/useFormCacheInvalidation', () => ({
    useFormCacheInvalidation: vi.fn(() => ({
        invalidateAllCaches: vi.fn().mockResolvedValue(undefined),
    })),
}));

vi.mock('../../contexts/ToastContext', () => ({
    useToast: vi.fn(() => ({
        addToast: vi.fn(),
    })),
}));

vi.mock('../../hooks/useHalPageData', () => ({
    useHalPageData: vi.fn(() => ({
        route: {
            pathname: '/members/123',
            refetch: vi.fn().mockResolvedValue(undefined),
        },
    })),
}));

const makeTemplate = (properties: HalFormsTemplate['properties']): HalFormsTemplate => ({
    method: 'PUT',
    target: '/api/members/123',
    properties,
});

const makeMemberData = (overrides?: Partial<HalResponse>): HalResponse => ({
    firstName: 'Jan',
    lastName: 'Novák',
    email: 'jan@test.cz',
    _links: {self: {href: '/api/members/123'}},
    _templates: {
        default: makeTemplate([
            {name: 'firstName', type: 'text', required: true},
            {name: 'lastName', type: 'text', required: true},
            {name: 'email', type: 'email'},
        ]),
    },
    ...overrides,
});

describe('useMemberEditForm', () => {
    beforeEach(() => {
        vi.clearAllMocks();
    });

    it('starts in non-editing mode', () => {
        const {result} = renderHook(() => useMemberEditForm(makeMemberData()));
        expect(result.current.isEditing).toBe(false);
    });

    it('enters edit mode when startEditing is called', () => {
        const {result} = renderHook(() => useMemberEditForm(makeMemberData()));
        act(() => result.current.startEditing());
        expect(result.current.isEditing).toBe(true);
    });

    it('exits edit mode when cancelEditing is called', () => {
        const {result} = renderHook(() => useMemberEditForm(makeMemberData()));
        act(() => result.current.startEditing());
        expect(result.current.isEditing).toBe(true);
        act(() => result.current.cancelEditing());
        expect(result.current.isEditing).toBe(false);
    });

    it('returns template from resourceData._templates.default', () => {
        const data = makeMemberData();
        const {result} = renderHook(() => useMemberEditForm(data));
        expect(result.current.template).toEqual(data._templates!.default);
    });

    it('returns null template when no _templates.default', () => {
        const data = makeMemberData({_templates: undefined});
        const {result} = renderHook(() => useMemberEditForm(data));
        expect(result.current.template).toBeNull();
    });

    it('builds initial values from member data based on template properties', () => {
        const data = makeMemberData();
        const {result} = renderHook(() => useMemberEditForm(data));
        expect(result.current.initialValues).toEqual({
            firstName: 'Jan',
            lastName: 'Novák',
            email: 'jan@test.cz',
        });
    });

    it('builds initial values with nested composites', () => {
        const data = makeMemberData({
            address: {street: 'Hlavní 15', city: 'Praha', postalCode: '11000', country: 'CZ'},
            _templates: {
                default: makeTemplate([
                    {name: 'firstName', type: 'text'},
                    {name: 'address', type: 'AddressRequest'},
                ]),
            },
        });
        const {result} = renderHook(() => useMemberEditForm(data));
        expect(result.current.initialValues.address).toEqual({
            street: 'Hlavní 15',
            city: 'Praha',
            postalCode: '11000',
            country: 'CZ',
        });
    });

    it('provides handleSubmit function', () => {
        const {result} = renderHook(() => useMemberEditForm(makeMemberData()));
        expect(typeof result.current.handleSubmit).toBe('function');
    });

    it('returns hasTemplate true when default template exists', () => {
        const {result} = renderHook(() => useMemberEditForm(makeMemberData()));
        expect(result.current.hasTemplate).toBe(true);
    });

    it('returns hasTemplate false when no default template', () => {
        const data = makeMemberData({_templates: undefined});
        const {result} = renderHook(() => useMemberEditForm(data));
        expect(result.current.hasTemplate).toBe(false);
    });
});
