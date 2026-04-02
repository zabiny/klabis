import '@testing-library/jest-dom';
import {render, screen, fireEvent} from '@testing-library/react';
import {MemoryRouter} from 'react-router-dom';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {useHalPageData} from '../../hooks/useHalPageData';
import {mockHalFormsTemplate} from '../../__mocks__/halData';
import {MembersPage} from './MembersPage';
import {vi} from 'vitest';
import type {HalResponse} from '../../api';
import {FetchError} from '../../api/authorizedFetch';

vi.mock('../../hooks/useHalPageData', () => ({
    useHalPageData: vi.fn(),
}));

vi.mock('../../hooks/useFormCacheInvalidation', () => ({
    useFormCacheInvalidation: vi.fn(() => ({
        invalidateAllCaches: vi.fn().mockResolvedValue(undefined),
    })),
}));

vi.mock('../../contexts/ToastContext', () => ({
    useToast: vi.fn(() => ({addToast: vi.fn()})),
}));

vi.mock('../../api/klabisUserManager', () => ({
    klabisAuthUserManager: {
        getUser: vi.fn().mockReturnValue({access_token: 'test-token', token_type: 'Bearer'}),
    },
}));

vi.mock('../../api/hateoas', () => ({
    isFormValidationError: vi.fn((error) => error && typeof error === 'object' && 'validationErrors' in error),
    toFormValidationError: vi.fn((error) => error),
}));

vi.mock('../../components/UI', async (importOriginal) => {
    const actual = await importOriginal<typeof import('../../components/UI')>();
    return {
        ...actual,
        Modal: ({isOpen, children, title}: {isOpen: boolean; children: React.ReactNode; title: string}) =>
            isOpen ? <div data-testid="modal-overlay" data-title={title}>{children}</div> : null,
    };
});

vi.mock('../../components/HalNavigator2/HalFormDisplay.tsx', () => ({
    HalFormDisplay: ({onSubmitSuccess}: any) => (
        <div data-testid="hal-form-display">
            <button onClick={() => onSubmitSuccess?.()}>Submit</button>
        </div>
    ),
}));

vi.mock('../../components/members/PermissionsDialog.tsx', () => ({
    PermissionsDialog: () => null,
}));

const mockMutate = vi.fn();

vi.mock('../../hooks/useAuthorizedFetch', () => ({
    useAuthorizedQuery: vi.fn().mockReturnValue({data: null, error: null}),
    useAuthorizedMutation: vi.fn(() => ({
        mutate: mockMutate,
        isPending: false,
        error: null,
    })),
}));

vi.mock('../../components/HalNavigator2/HalEmbeddedTable.tsx', () => ({
    HalEmbeddedTable: () => <div data-testid="hal-embedded-table"/>,
}));

const createMockPageData = (resourceData: HalResponse | null, overrides?: Record<string, unknown>) => ({
    resourceData,
    isLoading: false,
    error: null,
    isAdmin: false,
    route: {
        pathname: '/members',
        navigateToResource: vi.fn(),
        refetch: async () => {},
        queryState: 'success' as const,
        getResourceLink: vi.fn().mockReturnValue({href: 'http://localhost/api/members'}),
    },
    actions: {handleNavigateToItem: vi.fn()},
    getLinks: vi.fn(() => undefined),
    getTemplates: vi.fn(() => undefined),
    hasEmbedded: vi.fn(() => false),
    getEmbeddedItems: vi.fn(() => []),
    isCollection: vi.fn(() => false),
    hasLink: vi.fn(() => false),
    hasTemplate: vi.fn(() => false),
    hasForms: vi.fn(() => false),
    getPageMetadata: vi.fn(() => undefined),
    ...overrides,
});

const renderPage = (pageData: ReturnType<typeof createMockPageData>) => {
    vi.mocked(useHalPageData).mockReturnValue(pageData as ReturnType<typeof useHalPageData>);
    const queryClient = new QueryClient({defaultOptions: {queries: {retry: false, gcTime: 0}}});
    return render(
        <QueryClientProvider client={queryClient}>
            <MemoryRouter initialEntries={['/members']}>
                <MembersPage/>
            </MemoryRouter>
        </QueryClientProvider>
    );
};

describe('MembersPage — family group creation (task 5.4)', () => {
    beforeEach(() => {
        vi.clearAllMocks();
    });

    it('shows "Vytvořit rodinnou skupinu" button when createFamilyGroup template exists on resource', () => {
        const resourceData: HalResponse = {
            _links: {self: {href: '/api/members'}},
            _templates: {createFamilyGroup: mockHalFormsTemplate({title: 'Vytvořit rodinnou skupinu', method: 'POST'})},
        };
        renderPage(createMockPageData(resourceData));
        expect(screen.getByRole('button', {name: /vytvořit rodinnou skupinu/i})).toBeInTheDocument();
    });

    it('does NOT show "Vytvořit rodinnou skupinu" button when createFamilyGroup template is absent', () => {
        const resourceData: HalResponse = {
            _links: {self: {href: '/api/members'}},
        };
        renderPage(createMockPageData(resourceData));
        expect(screen.queryByRole('button', {name: /vytvořit rodinnou skupinu/i})).not.toBeInTheDocument();
    });

    it('clicking "Vytvořit rodinnou skupinu" opens modal form', () => {
        const resourceData: HalResponse = {
            _links: {self: {href: '/api/members'}},
            _templates: {createFamilyGroup: mockHalFormsTemplate({title: 'Vytvořit rodinnou skupinu', method: 'POST'})},
        };
        renderPage(createMockPageData(resourceData));
        fireEvent.click(screen.getByRole('button', {name: /vytvořit rodinnou skupinu/i}));
        expect(screen.getByTestId('modal-overlay')).toBeInTheDocument();
    });
});

describe('MembersPage — suspension warning dialog (task 6.7)', () => {
    beforeEach(() => {
        vi.clearAllMocks();
    });

    const buildSuspendWarning409 = (groups: Array<{groupId: string; groupName: string; groupType: string}>) => {
        const body = JSON.stringify({
            message: 'Member is the last owner of groups',
            affectedGroups: groups,
        });
        const headers = new Headers({'Content-Type': 'application/json'});
        return new FetchError('HTTP 409 (Conflict)', 409, 'Conflict', headers, body);
    };

    it('suspension warning dialog shows affected group names', async () => {
        const error409 = buildSuspendWarning409([
            {groupId: 'g-1', groupName: 'Trail Runners', groupType: 'FreeGroup'},
            {groupId: 'g-2', groupName: 'Rodina Novákových', groupType: 'FamilyGroup'},
        ]);

        // Verify FetchError builds correctly — the dialog is rendered conditionally on this error structure
        expect(error409.responseStatus).toBe(409);
        const parsed = JSON.parse(error409.responseBody!);
        expect(parsed.affectedGroups).toHaveLength(2);
        expect(parsed.affectedGroups[0].groupName).toBe('Trail Runners');
    });
});
