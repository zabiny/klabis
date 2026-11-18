import '@testing-library/jest-dom';
import {render, screen, fireEvent} from '@testing-library/react';
import {MemoryRouter} from 'react-router-dom';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {useHalPageData} from '../../hooks/useHalPageData';
import type {UseHalPageDataReturn} from '../../hooks/useHalPageData';
import {useAuthorizedQuery} from '../../hooks/useAuthorizedFetch';
import {mockHalFormsTemplate} from '../../__mocks__/halData';
import {makePageData} from '../../__mocks__/pageData';
import {DisciplinesPage} from './DisciplinesPage';
import {vi} from 'vitest';
import type {HalResponse} from '../../api';
import {ToastProvider} from '../../contexts/ToastContext.tsx';

vi.mock('../../hooks/useHalPageData', () => ({
    useHalPageData: vi.fn(),
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

vi.mock('../../hooks/useHalFormData', () => ({
    useHalFormData: (_template: unknown, resourceData: Record<string, unknown>) => ({
        formData: resourceData ?? {},
        isLoadingTargetData: false,
        targetFetchError: null,
        refetchTargetData: vi.fn(),
    }),
}));

vi.mock('../../api/klabisUserManager', () => ({
    klabisAuthUserManager: {
        getUser: vi.fn().mockReturnValue({access_token: 'test-token', token_type: 'Bearer'}),
    },
}));

vi.mock('../../hooks/useAuthorizedFetch', () => ({
    useAuthorizedQuery: vi.fn().mockReturnValue({data: undefined, error: null}),
    useAuthorizedMutation: vi.fn().mockReturnValue({
        mutate: vi.fn(),
        isPending: false,
        error: null,
    }),
}));

vi.mock('../../api/authorizedFetch', () => ({
    authorizedFetch: vi.fn(),
    ApiError: class ApiError extends Error {
        public status: number;
        public fieldErrors: Record<string, string>;
        constructor(status: number, details: {fieldErrors?: Record<string, string>} = {}) {
            super(`HTTP ${status}`);
            this.status = status;
            this.fieldErrors = details.fieldErrors ?? {};
        }
    },
}));

const createMockPageData = (resourceData: HalResponse | null) =>
    makePageData({
        resourceData,
        getResourceLink: vi.fn().mockReturnValue({href: 'http://localhost/api/disciplines'}),
    });

const renderPage = (pageData: UseHalPageDataReturn) => {
    vi.mocked(useHalPageData).mockReturnValue(pageData);
    const queryClient = new QueryClient({defaultOptions: {queries: {retry: false, gcTime: 0}}});
    return render(
        <QueryClientProvider client={queryClient}>
            <MemoryRouter initialEntries={['/event-settings/disciplines']}>
                <ToastProvider>
                    <DisciplinesPage/>
                </ToastProvider>
            </MemoryRouter>
        </QueryClientProvider>,
    );
};

const buildRow = (overrides?: Record<string, unknown>) => ({
    id: 'disc-1',
    code: 'OB',
    name: 'Orientační běh',
    archived: false,
    _links: {self: {href: '/api/disciplines/disc-1'}},
    ...overrides,
});

const renderPageWithDisciplines = (rows: unknown[]) => {
    const resourceData: HalResponse = {
        _links: {self: {href: 'http://localhost/api/disciplines'}},
        _embedded: {disciplineDtoList: rows},
        page: {size: 10, totalElements: rows.length, totalPages: 1, number: 0},
    };
    vi.mocked(useAuthorizedQuery).mockReturnValue({data: resourceData, error: null} as unknown as ReturnType<typeof useAuthorizedQuery>);
    return renderPage(createMockPageData(resourceData));
};

describe('DisciplinesPage', () => {
    beforeEach(() => {
        vi.clearAllMocks();
    });

    it('renders page title "Disciplíny"', () => {
        renderPage(createMockPageData(null));
        expect(screen.getByRole('heading', {name: 'Disciplíny'})).toBeInTheDocument();
    });

    describe('"Přidat disciplínu" button', () => {
        it('shows button when createDiscipline template exists', () => {
            renderPage(createMockPageData({
                _links: {self: {href: '/api/disciplines'}},
                _templates: {
                    createDiscipline: mockHalFormsTemplate({method: 'POST', target: '/api/disciplines', title: 'Přidat disciplínu'}),
                },
            }));
            expect(screen.getByRole('button', {name: /přidat disciplínu/i})).toBeInTheDocument();
        });

        it('does NOT show button when createDiscipline template is absent', () => {
            renderPage(createMockPageData({_links: {self: {href: '/api/disciplines'}}}));
            expect(screen.queryByRole('button', {name: /přidat disciplínu/i})).not.toBeInTheDocument();
        });
    });

    describe('table', () => {
        it('renders column headers', () => {
            renderPage(createMockPageData({_links: {self: {href: '/api/disciplines'}}}));
            expect(screen.getByText('Kód')).toBeInTheDocument();
            expect(screen.getByText('Název')).toBeInTheDocument();
            expect(screen.getByText('Status')).toBeInTheDocument();
            expect(screen.getByText('Akce')).toBeInTheDocument();
        });

        it('renders code and name of a discipline', () => {
            renderPageWithDisciplines([buildRow()]);
            expect(screen.getByText('OB')).toBeInTheDocument();
            expect(screen.getByText('Orientační běh')).toBeInTheDocument();
        });

        it('marks active discipline as Aktivní', () => {
            renderPageWithDisciplines([buildRow()]);
            expect(screen.getByText('Aktivní')).toBeInTheDocument();
        });

        it('marks archived discipline as Archivovaná', () => {
            renderPageWithDisciplines([buildRow({archived: true})]);
            expect(screen.getByText('Archivovaná')).toBeInTheDocument();
        });
    });

    describe('row actions', () => {
        it.each([
            ['updateDiscipline', 'PUT' as const, 'Upravit disciplínu'],
            ['archiveDiscipline', 'DELETE' as const, 'Archivovat disciplínu'],
            ['restoreDiscipline', 'POST' as const, 'Obnovit disciplínu'],
        ])('shows %s button only when the row offers the template', (name, method, label) => {
            renderPageWithDisciplines([buildRow({_templates: {[name]: mockHalFormsTemplate({method, title: label})}})]);
            expect(screen.getByRole('button', {name: label})).toBeInTheDocument();
        });

        it('shows no action buttons when the row offers no templates', () => {
            renderPageWithDisciplines([buildRow()]);
            expect(screen.queryByRole('button', {name: 'Upravit disciplínu'})).not.toBeInTheDocument();
            expect(screen.queryByRole('button', {name: 'Archivovat disciplínu'})).not.toBeInTheDocument();
            expect(screen.queryByRole('button', {name: 'Obnovit disciplínu'})).not.toBeInTheDocument();
        });

        it('opens a dialog titled by the template when edit is clicked', () => {
            renderPageWithDisciplines([buildRow({
                _templates: {updateDiscipline: mockHalFormsTemplate({method: 'PUT', title: 'Upravit disciplínu'})},
            })]);
            fireEvent.click(screen.getByRole('button', {name: 'Upravit disciplínu'}));
            expect(screen.getByRole('dialog')).toBeInTheDocument();
        });
    });
});
