import '@testing-library/jest-dom';
import {render, screen, fireEvent} from '@testing-library/react';
import {MemoryRouter} from 'react-router-dom';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {useHalPageData} from '../../hooks/useHalPageData';
import {useAuthorizedQuery} from '../../hooks/useAuthorizedFetch';
import {mockHalFormsTemplate} from '../../__mocks__/halData';
import {CategoryPresetsPage} from './CategoryPresetsPage';
import {vi} from 'vitest';
import type {HalResponse} from '../../api';

vi.mock('../../hooks/useHalPageData', () => ({
    useHalPageData: vi.fn(),
}));

vi.mock('../../contexts/HalFormContext.tsx', () => ({
    useHalForm: vi.fn().mockReturnValue({
        displayHalForm: vi.fn(),
        currentFormRequest: null,
        closeForm: vi.fn(),
    }),
    HalFormProvider: ({children}: {children: React.ReactNode}) => children,
}));

vi.mock('../../components/HalNavigator2/HalFormDisplay.tsx', () => ({
    HalFormDisplay: () => <div data-testid="hal-form-display"/>,
}));

vi.mock('../../components/UI', async (importOriginal) => {
    const actual = await importOriginal<typeof import('../../components/UI')>();
    return {
        ...actual,
        Modal: ({isOpen, children, title}: {isOpen: boolean; children: React.ReactNode; title: string}) =>
            isOpen ? <div data-testid="modal-overlay" data-title={title}>{children}</div> : null,
    };
});

vi.mock('../../hooks/useAuthorizedFetch', () => ({
    useAuthorizedQuery: vi.fn().mockReturnValue({data: null, error: null}),
    useAuthorizedMutation: vi.fn().mockReturnValue({
        mutate: vi.fn(),
        isPending: false,
        error: null,
    }),
}));

vi.mock('../../api/authorizedFetch', () => ({
    authorizedFetch: vi.fn(),
    FetchError: class FetchError extends Error {
        public responseStatus: number;
        constructor(message: string, status: number) {
            super(message);
            this.responseStatus = status;
        }
    },
}));

const createMockPageData = (resourceData: HalResponse | null, overrides?: any) => ({
    resourceData,
    isLoading: false,
    error: null,
    isAdmin: false,
    route: {
        pathname: '/category-presets',
        navigateToResource: vi.fn(),
        refetch: async () => {},
        queryState: 'success' as const,
        getResourceLink: vi.fn().mockReturnValue({href: 'http://localhost/api/category-presets'}),
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

const renderPage = (pageData: any) => {
    vi.mocked(useHalPageData).mockReturnValue(pageData);
    const queryClient = new QueryClient({defaultOptions: {queries: {retry: false, gcTime: 0}}});
    return render(
        <QueryClientProvider client={queryClient}>
            <MemoryRouter initialEntries={['/category-presets']}>
                <CategoryPresetsPage />
            </MemoryRouter>
        </QueryClientProvider>,
    );
};

const buildPresetRow = (overrides?: Record<string, unknown>) => ({
    id: 'preset-1',
    name: 'Základní šablona',
    categories: ['A', 'B'],
    _links: {self: {href: '/api/category-presets/preset-1'}},
    ...overrides,
});

const renderPageWithPresets = (presets: unknown[]) => {
    const resourceData: HalResponse = {
        _links: {self: {href: 'http://localhost/api/category-presets'}},
        _embedded: {categoryPresetDtoList: presets},
        page: {size: 10, totalElements: presets.length, totalPages: 1, number: 0},
    };
    vi.mocked(useAuthorizedQuery).mockReturnValue({data: resourceData, error: null} as any);
    const pageData = createMockPageData(resourceData);
    return renderPage(pageData);
};

describe('CategoryPresetsPage', () => {
    beforeEach(() => {
        vi.clearAllMocks();
    });

    it('renders page title "Šablony kategorií"', () => {
        renderPage(createMockPageData(null));
        expect(screen.getByRole('heading', {name: 'Šablony kategorií'})).toBeInTheDocument();
    });

    describe('"Přidat šablonu" button', () => {
        it('shows button when createCategoryPreset template exists', () => {
            const resourceData: HalResponse = {
                _links: {self: {href: '/api/category-presets'}},
                _templates: {
                    createCategoryPreset: mockHalFormsTemplate({
                        method: 'POST',
                        target: '/api/category-presets',
                        title: 'Přidat šablonu',
                    }),
                },
            };
            renderPage(createMockPageData(resourceData));
            expect(screen.getByRole('button', {name: /přidat šablonu/i})).toBeInTheDocument();
        });

        it('does NOT show button when createCategoryPreset template is absent', () => {
            const resourceData: HalResponse = {
                _links: {self: {href: '/api/category-presets'}},
            };
            renderPage(createMockPageData(resourceData));
            expect(screen.queryByRole('button', {name: /přidat šablonu/i})).not.toBeInTheDocument();
        });
    });

    describe('row click behaviour', () => {
        it('does not call navigateToResource when a row is clicked', () => {
            const navigateToResource = vi.fn();
            const resourceData: HalResponse = {
                _links: {self: {href: 'http://localhost/api/category-presets'}},
                _embedded: {categoryPresetDtoList: [buildPresetRow()]},
                page: {size: 10, totalElements: 1, totalPages: 1, number: 0},
            };
            vi.mocked(useAuthorizedQuery).mockReturnValue({data: resourceData, error: null} as any);
            renderPage(createMockPageData(resourceData, {route: {pathname: '/category-presets', navigateToResource, refetch: async () => {}, queryState: 'success' as const, getResourceLink: vi.fn().mockReturnValue({href: 'http://localhost/api/category-presets'})}}));

            fireEvent.click(screen.getByText('Základní šablona'));

            expect(navigateToResource).not.toHaveBeenCalled();
        });
    });

    describe('Akce column — action buttons on preset rows', () => {
        it('renders Akce column header', () => {
            renderPage(createMockPageData({_links: {self: {href: '/api/category-presets'}}}));
            expect(screen.getByText('Akce')).toBeInTheDocument();
        });

        it('shows edit button when _templates.updateCategoryPreset is present on a row', () => {
            const preset = buildPresetRow({
                _templates: {updateCategoryPreset: mockHalFormsTemplate({method: 'PUT', title: 'Upravit šablonu'})},
            });
            renderPageWithPresets([preset]);
            expect(screen.getByRole('button', {name: 'Upravit'})).toBeInTheDocument();
        });

        it('does NOT show edit button when _templates.updateCategoryPreset is absent', () => {
            renderPageWithPresets([buildPresetRow()]);
            expect(screen.queryByRole('button', {name: 'Upravit'})).not.toBeInTheDocument();
        });

        it('shows delete button when _templates.deleteCategoryPreset is present on a row', () => {
            const preset = buildPresetRow({
                _templates: {deleteCategoryPreset: mockHalFormsTemplate({method: 'DELETE', title: 'Smazat šablonu'})},
            });
            renderPageWithPresets([preset]);
            expect(screen.getByRole('button', {name: 'Smazat šablonu'})).toBeInTheDocument();
        });

        it('does NOT show delete button when _templates.deleteCategoryPreset is absent', () => {
            renderPageWithPresets([buildPresetRow()]);
            expect(screen.queryByRole('button', {name: 'Smazat šablonu'})).not.toBeInTheDocument();
        });

        it('shows empty action cell when row has neither updateCategoryPreset nor deleteCategoryPreset', () => {
            renderPageWithPresets([buildPresetRow()]);
            expect(screen.queryByRole('button', {name: 'Upravit'})).not.toBeInTheDocument();
            expect(screen.queryByRole('button', {name: 'Smazat šablonu'})).not.toBeInTheDocument();
        });

        it('opens modal with HalFormDisplay when edit button is clicked', () => {
            const preset = buildPresetRow({
                _templates: {updateCategoryPreset: mockHalFormsTemplate({method: 'PUT', title: 'Upravit šablonu'})},
            });
            renderPageWithPresets([preset]);

            fireEvent.click(screen.getByRole('button', {name: 'Upravit'}));

            expect(screen.getByTestId('modal-overlay')).toBeInTheDocument();
            expect(screen.getByTestId('hal-form-display')).toBeInTheDocument();
        });

        it('opens modal with HalFormDisplay when delete button is clicked', () => {
            const preset = buildPresetRow({
                _templates: {deleteCategoryPreset: mockHalFormsTemplate({method: 'DELETE', title: 'Smazat šablonu'})},
            });
            renderPageWithPresets([preset]);

            fireEvent.click(screen.getByRole('button', {name: 'Smazat šablonu'}));

            expect(screen.getByTestId('modal-overlay')).toBeInTheDocument();
            expect(screen.getByTestId('hal-form-display')).toBeInTheDocument();
        });
    });
});
