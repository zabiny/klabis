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

/**
 * Finance-specific mock data factories
 */

export const mockFinanceResource = (overrides?: Partial<HalResponse>): HalResponse => ({
    balance: 1500,
    _links: {
        self: mockLink('/api/finances/123'),
        owner: mockLink('/api/members/456'),
        transactions: mockLink('/api/finances/123/transactions'),
        deposit: mockLink('/api/finances/123/deposit'),
        withdraw: mockLink('/api/finances/123/withdraw'),
    },
    ...overrides,
});

export const mockMemberResource = (overrides?: Partial<HalResponse>): HalResponse => ({
    firstName: 'Jan',
    lastName: 'Novák',
    email: 'jan.novak@example.com',
    _links: {
        self: mockLink('/api/members/456'),
        finances: mockLink('/api/members/456/finances'),
    },
    ...overrides,
});

export const mockTransactionCollection = (
    count: number = 3,
    overrides?: Partial<HalCollectionResponse>,
): HalCollectionResponse => {
    const transactions = Array.from({length: count}, (_, i) => ({
        id: i + 1,
        date: `2025-01-${String(i + 1).padStart(2, '0')}`,
        amount: (i + 1) * 100,
        note: `Transaction ${i + 1}`,
        type: i % 2 === 0 ? 'deposit' : 'withdraw',
        _links: {
            self: mockLink(`/api/transactions/${i + 1}`),
        },
    }));

    return {
        _links: {
            self: mockLink('/api/finances/123/transactions'),
        },
        _embedded: {
            transactionItemResponseList: transactions,
        },
        page: {
            totalElements: count,
            totalPages: 1,
            size: count,
            number: 0,
        },
        ...overrides,
    };
};

export const mockTransactionResponse = (overrides?: Partial<HalResponse>): HalResponse => ({
    id: 1,
    date: '2025-01-15',
    amount: 500,
    note: 'Monthly deposit',
    type: 'deposit',
    _links: {
        self: mockLink('/api/transactions/1'),
        finance: mockLink('/api/finances/123'),
    },
    ...overrides,
});
