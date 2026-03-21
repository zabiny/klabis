import '@testing-library/jest-dom';
import {type ReactElement} from 'react';
import {render, screen} from '@testing-library/react';
import {MemoryRouter} from 'react-router-dom';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {vi} from 'vitest';
import type {HalFormsTemplate, HalResponse} from '../../api';
import {MemberRegistrationPage} from './MemberRegistrationPage';

const mockNavigate = vi.fn();
vi.mock('react-router-dom', async () => {
    const actual = await vi.importActual('react-router-dom');
    return {
        ...actual,
        useNavigate: () => mockNavigate,
    };
});

vi.mock('../../hooks/useAuthorizedFetch', () => ({
    useAuthorizedQuery: vi.fn(),
}));

vi.mock('../../api/klabisUserManager', () => ({
    klabisAuthUserManager: {
        getUser: vi.fn().mockResolvedValue({
            access_token: 'test-token',
            token_type: 'Bearer',
        }),
    },
}));

let capturedOnSubmitSuccess: ((data: unknown) => void) | undefined;
let capturedCustomLayout: unknown;

vi.mock('../../components/HalNavigator2/HalFormDisplay', () => ({
    HalFormDisplay: vi.fn(({onSubmitSuccess, customLayout}: any) => {
        capturedOnSubmitSuccess = onSubmitSuccess;
        capturedCustomLayout = customLayout;
        return <div data-testid="hal-form-display">HalFormDisplay</div>;
    }),
}));

import {useAuthorizedQuery} from '../../hooks/useAuthorizedFetch';
import {HalFormDisplay} from '../../components/HalNavigator2/HalFormDisplay';

const memberCreationTemplate: HalFormsTemplate = {
    method: 'POST',
    target: '/api/members',
    properties: [
        {name: 'firstName', type: 'text', prompt: 'Jméno', required: true},
        {name: 'lastName', type: 'text', prompt: 'Příjmení', required: true},
        {name: 'dateOfBirth', type: 'date', prompt: 'Datum narození'},
        {name: 'gender', type: 'text', prompt: 'Pohlaví', options: {inline: [{value: 'MALE', prompt: 'Muž'}, {value: 'FEMALE', prompt: 'Žena'}]}},
        {name: 'nationality', type: 'text', prompt: 'Státní příslušnost'},
        {name: 'email', type: 'email', prompt: 'E-mail'},
        {name: 'phone', type: 'tel', prompt: 'Telefon'},
        {name: 'address', type: 'AddressRequest', prompt: 'Adresa'},
    ],
};

const collectionWithTemplate: HalResponse = {
    _links: {self: {href: '/api/members'}},
    _templates: {registerMember: memberCreationTemplate},
};

const collectionWithoutTemplate: HalResponse = {
    _links: {self: {href: '/api/members'}},
};

const mockUseAuthorizedQuery = (data: HalResponse | null, isLoading = false, error: Error | null = null) => {
    vi.mocked(useAuthorizedQuery).mockReturnValue({
        data,
        isLoading,
        error,
        isError: !!error,
        isSuccess: !isLoading && !error && !!data,
    } as any);
};

const renderPage = () => {
    const queryClient = new QueryClient({defaultOptions: {queries: {retry: false, gcTime: 0}}});
    return render(
        <QueryClientProvider client={queryClient}>
            <MemoryRouter initialEntries={['/members/new']}>
                <MemberRegistrationPage/>
            </MemoryRouter>
        </QueryClientProvider>
    );
};

describe('MemberRegistrationPage', () => {
    beforeEach(() => {
        vi.clearAllMocks();
        capturedOnSubmitSuccess = undefined;
        capturedCustomLayout = undefined;
    });

    describe('loading and error states', () => {
        it('shows loading skeleton when fetching collection data', () => {
            mockUseAuthorizedQuery(null, true);
            renderPage();
            expect(screen.getByText(/načítání/i)).toBeInTheDocument();
        });

        it('shows error when collection fetch fails', () => {
            mockUseAuthorizedQuery(null, false, new Error('Network error'));
            renderPage();
            expect(screen.getByText(/Network error/)).toBeInTheDocument();
        });

        it('shows error when collection has no registerMember template', () => {
            mockUseAuthorizedQuery(collectionWithoutTemplate);
            renderPage();
            expect(screen.getByText(/registrace.*není.*k dispozici/i)).toBeInTheDocument();
        });
    });

    describe('HalFormDisplay integration', () => {
        beforeEach(() => {
            mockUseAuthorizedQuery(collectionWithTemplate);
        });

        it('renders HalFormDisplay when registerMember template is available', () => {
            renderPage();
            expect(screen.getByTestId('hal-form-display')).toBeInTheDocument();
        });

        it('passes registerMember template to HalFormDisplay', () => {
            renderPage();
            expect(vi.mocked(HalFormDisplay)).toHaveBeenCalledWith(
                expect.objectContaining({
                    template: memberCreationTemplate,
                    templateName: 'registerMember',
                }),
                undefined
            );
        });

        it('passes customLayout renderForm callback to HalFormDisplay', () => {
            renderPage();
            expect(capturedCustomLayout).toBeTypeOf('function');
        });

        it('does not use default template key', () => {
            const collectionWithDefault: HalResponse = {
                _links: {self: {href: '/api/members'}},
                _templates: {default: memberCreationTemplate},
            };
            mockUseAuthorizedQuery(collectionWithDefault);
            renderPage();
            expect(screen.getByText(/registrace.*není.*k dispozici/i)).toBeInTheDocument();
        });
    });

    describe('navigation after success', () => {
        beforeEach(() => {
            mockUseAuthorizedQuery(collectionWithTemplate);
        });

        it('navigates to new member detail page when onSubmitSuccess receives self link', () => {
            renderPage();

            capturedOnSubmitSuccess?.({
                _links: {self: {href: '/api/members/new-member-uuid'}},
            });

            expect(mockNavigate).toHaveBeenCalledWith('/members/new-member-uuid');
        });

        it('navigates to /members when onSubmitSuccess receives response without self link', () => {
            renderPage();

            capturedOnSubmitSuccess?.({});

            expect(mockNavigate).toHaveBeenCalledWith('/members');
        });

        it('navigates to /members when onSubmitSuccess receives undefined', () => {
            renderPage();

            capturedOnSubmitSuccess?.(undefined);

            expect(mockNavigate).toHaveBeenCalledWith('/members');
        });
    });

    describe('page layout rendering via renderForm callback', () => {
        const renderFormLayout = () => {
            const queryClient = new QueryClient({defaultOptions: {queries: {retry: false, gcTime: 0}}});
            mockUseAuthorizedQuery(collectionWithTemplate);
            render(
                <QueryClientProvider client={queryClient}>
                    <MemoryRouter initialEntries={['/members/new']}>
                        <MemberRegistrationPage/>
                    </MemoryRouter>
                </QueryClientProvider>
            );
            const renderFn = capturedCustomLayout as (helpers: any) => ReactElement;
            const helpers = {
                renderInput: (name: string) => <input key={name} data-testid={`input-${name}`}/>,
                renderField: (name: string) => <div key={name} data-testid={`field-${name}`}/>,
                renderLabel: (name: string) => name,
            };
            return render(
                <MemoryRouter>
                    {renderFn(helpers)}
                </MemoryRouter>
            );
        };

        it('renders back link to members list inside custom layout', () => {
            renderFormLayout();
            const backLinks = screen.getAllByText(/zpět na seznam/i);
            expect(backLinks.length).toBeGreaterThan(0);
            expect(backLinks[0].closest('a')).toHaveAttribute('href', '/members');
        });

        it('renders page title "Registrace nového člena" inside custom layout', () => {
            renderFormLayout();
            expect(screen.getByRole('heading', {level: 1, name: /registrace nového člena/i})).toBeInTheDocument();
        });
    });
});
