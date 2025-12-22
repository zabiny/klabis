import type {HalCollectionResponse, HalFormsTemplate, HalResponse, Link} from '../api';

/**
 * Mock HAL data factories for testing
 */

export const mockLink = (href: string = '/api/items/1', title?: string): Link => ({
    href,
    ...(title && {title}),
});

export const mockHalFormsTemplate = (overrides?: Partial<HalFormsTemplate>): HalFormsTemplate => ({
    method: 'POST',
    target: '/api/items',
    title: 'Create Item',
    properties: [
        {
            name: 'name',
            prompt: 'Item Name',
            type: 'text',
            required: true,
        },
        {
            name: 'description',
            prompt: 'Description',
            type: 'textarea',
        },
    ],
    ...overrides,
});

export const mockHalResponse = (overrides?: Partial<HalResponse>): HalResponse => ({
    _links: {
        self: mockLink('/api/items/1', 'Get Item'),
        all: mockLink('/api/items', 'All Items'),
    },
    name: 'Test Item',
    description: 'A test item',
    ...overrides,
});

export const mockHalCollectionResponse = (
    itemCount: number = 3,
    overrides?: Partial<HalCollectionResponse>,
): HalCollectionResponse => {
    const items = Array.from({length: itemCount}, (_, i) => ({
        id: i + 1,
        name: `Item ${i + 1}`,
        _links: {
            self: mockLink(`/api/items/${i + 1}`),
        },
    }));

    return {
        _links: {
            self: mockLink('/api/items'),
            next: mockLink('/api/items?page=1'),
        },
        _embedded: {
            items,
        },
        page: {
            totalElements: itemCount,
            totalPages: 1,
            size: itemCount,
            number: 0,
        },
        ...overrides,
    };
};

export const mockHalResponseWithForms = (
    overrides?: Partial<HalResponse>,
): HalResponse => ({
    ...mockHalResponse(),
    _templates: {
        create: mockHalFormsTemplate({title: 'Create'}),
        update: mockHalFormsTemplate({method: 'PUT', title: 'Update'}),
    },
    ...overrides,
});

export const mockHalCollectionResponseWithForms = (
    itemCount: number = 2,
    overrides?: Partial<HalCollectionResponse>,
): HalCollectionResponse => ({
    ...mockHalCollectionResponse(itemCount),
    _templates: {
        create: mockHalFormsTemplate({title: 'Create Item'}),
    },
    ...overrides,
});

export const mockEmptyHalCollectionResponse = (): HalCollectionResponse => ({
    _links: {
        self: mockLink('/api/items'),
    },
    _embedded: {
        items: [],
    },
    page: {
        totalElements: 0,
        totalPages: 0,
        size: 10,
        number: 0,
    },
});
