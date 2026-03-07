import '@testing-library/jest-dom';
import {render, screen, waitFor} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
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

const mockMutate = vi.fn();
vi.mock('../../hooks/useAuthorizedFetch', () => ({
    useAuthorizedQuery: vi.fn(),
    useAuthorizedMutation: vi.fn(() => ({
        mutate: mockMutate,
        isPending: false,
        error: null,
    })),
}));

vi.mock('../../hooks/useFormCacheInvalidation', () => ({
    useFormCacheInvalidation: vi.fn(() => ({
        invalidateAllCaches: vi.fn().mockResolvedValue(undefined),
    })),
}));

vi.mock('../../api/klabisUserManager', () => ({
    klabisAuthUserManager: {
        getUser: vi.fn().mockResolvedValue({
            access_token: 'test-token',
            token_type: 'Bearer',
        }),
    },
}));

vi.mock('../../api/hateoas', async () => {
    const actual = await vi.importActual('../../api/hateoas');
    return {
        ...actual,
        submitHalFormsData: vi.fn(),
        isFormValidationError: vi.fn((error) => {
            return error && typeof error === 'object' && 'validationErrors' in error;
        }),
    };
});

import {useAuthorizedQuery, useAuthorizedMutation} from '../../hooks/useAuthorizedFetch';

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
    _templates: {default: memberCreationTemplate},
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

        it('shows error when collection has no creation template', () => {
            mockUseAuthorizedQuery(collectionWithoutTemplate);
            renderPage();
            expect(screen.getByText(/registrace.*není.*k dispozici/i)).toBeInTheDocument();
        });
    });

    describe('page layout', () => {
        beforeEach(() => {
            mockUseAuthorizedQuery(collectionWithTemplate);
        });

        it('renders back link to members list', () => {
            renderPage();
            const backLink = screen.getByText(/zpět na seznam/i);
            expect(backLink).toBeInTheDocument();
            expect(backLink.closest('a')).toHaveAttribute('href', '/members');
        });

        it('renders page title "Registrace nového člena"', () => {
            renderPage();
            expect(screen.getByRole('heading', {level: 1, name: /registrace nového člena/i})).toBeInTheDocument();
        });

        it('renders "Registrovat" submit button', () => {
            renderPage();
            expect(screen.getByRole('button', {name: /registrovat/i})).toBeInTheDocument();
        });

        it('renders "Zrušit" cancel link to /members', () => {
            renderPage();
            const cancelLink = screen.getByRole('link', {name: /zrušit/i});
            expect(cancelLink).toHaveAttribute('href', '/members');
        });
    });

    describe('form sections', () => {
        beforeEach(() => {
            mockUseAuthorizedQuery(collectionWithTemplate);
        });

        it('renders OSOBNÍ ÚDAJE section with fields from template', () => {
            renderPage();
            expect(screen.getByText(/osobní údaje/i)).toBeInTheDocument();
        });

        it('renders KONTAKT section when email or phone in template', () => {
            renderPage();
            expect(screen.getByText(/kontakt/i)).toBeInTheDocument();
        });

        it('renders ADRESA section when address property in template', () => {
            renderPage();
            expect(screen.getByRole('heading', {level: 3, name: /adresa/i})).toBeInTheDocument();
        });

        it('does not render section when its fields are absent from template', () => {
            const minimalTemplate: HalFormsTemplate = {
                method: 'POST',
                target: '/api/members',
                properties: [
                    {name: 'firstName', type: 'text', prompt: 'Jméno'},
                ],
            };
            const minimalCollection: HalResponse = {
                _links: {self: {href: '/api/members'}},
                _templates: {default: minimalTemplate},
            };
            mockUseAuthorizedQuery(minimalCollection);
            renderPage();
            expect(screen.queryByText(/kontakt/i)).not.toBeInTheDocument();
            expect(screen.queryByText(/adresa/i)).not.toBeInTheDocument();
        });
    });

    describe('form fields are editable', () => {
        beforeEach(() => {
            mockUseAuthorizedQuery(collectionWithTemplate);
        });

        it('renders editable input fields from template', () => {
            renderPage();
            const inputs = screen.getAllByRole('textbox');
            expect(inputs.length).toBeGreaterThan(0);
        });

        it('allows typing into firstName field', async () => {
            const user = userEvent.setup();
            renderPage();

            const inputs = screen.getAllByRole('textbox');
            const firstNameInput = inputs[0];
            await user.type(firstNameInput, 'Karel');
            expect(firstNameInput).toHaveValue('Karel');
        });

        it('renders gender field from template', () => {
            renderPage();
            expect(screen.getByText('Pohlaví')).toBeInTheDocument();
        });
    });

    describe('form submission', () => {
        beforeEach(() => {
            mockUseAuthorizedQuery(collectionWithTemplate);
        });

        it('calls mutate with POST to /api/members on submit', async () => {
            const simpleTemplate: HalFormsTemplate = {
                method: 'POST',
                target: '/api/members',
                properties: [
                    {name: 'firstName', type: 'text', prompt: 'Jméno'},
                    {name: 'lastName', type: 'text', prompt: 'Příjmení'},
                ],
            };
            const simpleCollection: HalResponse = {
                _links: {self: {href: '/api/members'}},
                _templates: {default: simpleTemplate},
            };
            mockUseAuthorizedQuery(simpleCollection);

            const user = userEvent.setup();
            renderPage();

            const inputs = screen.getAllByRole('textbox');
            await user.type(inputs[0], 'Karel');
            await user.type(inputs[1], 'Novak');

            await user.click(screen.getByRole('button', {name: /registrovat/i}));

            await waitFor(() => {
                expect(mockMutate).toHaveBeenCalled();
            });

            expect(mockMutate).toHaveBeenCalledWith(
                expect.objectContaining({
                    url: '/api/members',
                    data: expect.objectContaining({
                        firstName: 'Karel',
                        lastName: 'Novak',
                    }),
                })
            );
        });

        it('passes useAuthorizedMutation with method POST', () => {
            renderPage();
            expect(useAuthorizedMutation).toHaveBeenCalledWith(
                expect.objectContaining({method: 'POST'})
            );
        });
    });

    describe('navigation after success', () => {
        it('onSuccess callback navigates to created member detail page', () => {
            mockUseAuthorizedQuery(collectionWithTemplate);

            let capturedOnSuccess: (data: any) => void = () => {};
            vi.mocked(useAuthorizedMutation).mockImplementation((opts: any) => {
                capturedOnSuccess = opts.onSuccess;
                return {mutate: mockMutate, isPending: false, error: null} as any;
            });

            renderPage();

            capturedOnSuccess({
                _links: {self: {href: '/api/members/new-member-uuid'}},
            });

            expect(mockNavigate).toHaveBeenCalledWith('/members/new-member-uuid');
        });

        it('onSuccess navigates to /members when no self link in response', () => {
            mockUseAuthorizedQuery(collectionWithTemplate);

            let capturedOnSuccess: (data: any) => void = () => {};
            vi.mocked(useAuthorizedMutation).mockImplementation((opts: any) => {
                capturedOnSuccess = opts.onSuccess;
                return {mutate: mockMutate, isPending: false, error: null} as any;
            });

            renderPage();

            capturedOnSuccess({});

            expect(mockNavigate).toHaveBeenCalledWith('/members');
        });
    });
});
