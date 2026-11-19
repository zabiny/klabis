import '@testing-library/jest-dom';
import {render, screen} from '@testing-library/react';
import {MemoryRouter} from 'react-router-dom';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {useHalPageData} from '../../hooks/useHalPageData';
import {useAuthorizedQuery} from '../../hooks/useAuthorizedFetch';
import {mockHalFormsTemplate} from '../../__mocks__/halData';
import {makePageData} from '../../__mocks__/pageData';
import {TypeCatalogSection} from './TypeCatalogSection';
import {vi} from 'vitest';
import type {HalResponse} from '../../api';

vi.mock('../../hooks/useHalPageData', () => ({
    useHalPageData: vi.fn(),
}));

vi.mock('../../hooks/useAuthorizedFetch', () => ({
    useAuthorizedQuery: vi.fn().mockReturnValue({data: undefined, error: null}),
    useAuthorizedMutation: vi.fn().mockReturnValue({
        mutate: vi.fn(),
        isPending: false,
        error: null,
    }),
}));

vi.mock('../../contexts/HalFormContext.tsx', () => ({
    HalFormProvider: ({children}: {children: React.ReactNode}) => children,
}));

vi.mock('../../contexts/halFormContext.ts', () => ({
    useHalForm: vi.fn().mockReturnValue({
        displayHalForm: vi.fn(),
        currentFormRequest: null,
        closeForm: vi.fn(),
    }),
}));

const createMockPageData = (resourceData: HalResponse | null, overrides: Partial<ReturnType<typeof makePageData>> = {}) =>
    makePageData({
        resourceData,
        getResourceLink: vi.fn().mockReturnValue({href: 'http://localhost/api/membership-fee-types'}),
        ...overrides,
    });

const renderSection = (pageData: ReturnType<typeof createMockPageData>) => {
    vi.mocked(useHalPageData).mockReturnValue(pageData);
    const queryClient = new QueryClient({defaultOptions: {queries: {retry: false, gcTime: 0}}});
    return render(
        <QueryClientProvider client={queryClient}>
            <MemoryRouter initialEntries={['/administration/membership-fees']}>
                <TypeCatalogSection/>
            </MemoryRouter>
        </QueryClientProvider>
    );
};

describe('TypeCatalogSection', () => {
    beforeEach(() => {
        vi.clearAllMocks();
    });

    it('renders section heading "Katalog typů členského příspěvku"', () => {
        renderSection(createMockPageData(null));
        expect(screen.getByRole('heading', {name: /katalog typů členského příspěvku/i})).toBeInTheDocument();
    });

    it('renders table column headers for name and yearly fee amount', () => {
        renderSection(createMockPageData(null));
        expect(screen.getByText('Název')).toBeInTheDocument();
        expect(screen.getByText('Roční poplatek')).toBeInTheDocument();
    });

    it('renders "Přidat typ členského příspěvku" create button when createFeeType template is present', () => {
        const resourceData: HalResponse = {
            _links: {self: {href: '/api/membership-fee-types'}},
            _templates: {
                createFeeType: mockHalFormsTemplate({title: 'Přidat typ členského příspěvku', method: 'POST'}),
            },
        };
        renderSection(createMockPageData(resourceData));
        expect(screen.getByRole('button', {name: /přidat typ členského příspěvku/i})).toBeInTheDocument();
    });

    it('does not render create button when createFeeType template is absent', () => {
        const resourceData: HalResponse = {
            _links: {self: {href: '/api/membership-fee-types'}},
        };
        renderSection(createMockPageData(resourceData));
        expect(screen.queryByRole('button', {name: /přidat typ členského příspěvku/i})).not.toBeInTheDocument();
    });

    it('renders type name when embedded types are available', () => {
        const resourceData: HalResponse = {
            _links: {self: {href: '/api/membership-fee-types'}},
            _embedded: {
                membershipFeeTypeSummaryResponseList: [{
                    id: 'type-1',
                    name: 'Základní členství',
                    yearlyFeeAmount: 500,
                    _links: {self: {href: '/api/membership-fee-types/type-1'}},
                }],
            },
            page: {size: 10, totalElements: 1, totalPages: 1, number: 0},
        };
        vi.mocked(useAuthorizedQuery).mockReturnValue({
            data: resourceData,
            error: null,
        } as unknown as ReturnType<typeof useAuthorizedQuery>);
        renderSection(createMockPageData(resourceData));
        expect(screen.getByText('Základní členství')).toBeInTheDocument();
    });

    it('navigates to type detail when a table row is clicked', () => {
        const navigateToResource = vi.fn();
        const resourceData: HalResponse = {
            _links: {self: {href: '/api/membership-fee-types'}},
            _embedded: {
                membershipFeeTypeSummaryResponseList: [{
                    id: 'type-1',
                    name: 'Základní členství',
                    yearlyFeeAmount: 500,
                    _links: {self: {href: '/api/membership-fee-types/type-1'}},
                }],
            },
            page: {size: 10, totalElements: 1, totalPages: 1, number: 0},
        };
        vi.mocked(useAuthorizedQuery).mockReturnValue({
            data: resourceData,
            error: null,
        } as unknown as ReturnType<typeof useAuthorizedQuery>);
        renderSection(createMockPageData(resourceData, {
            navigateToResource,
            refetch: async () => {},
            getResourceLink: vi.fn().mockReturnValue({href: 'http://localhost/api/membership-fee-types'}),
        }));
        screen.getByText('Základní členství').closest('tr')!.click();
        expect(navigateToResource).toHaveBeenCalled();
    });
});
