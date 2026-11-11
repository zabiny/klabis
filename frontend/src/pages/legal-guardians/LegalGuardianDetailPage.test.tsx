import '@testing-library/jest-dom';
import {render, screen} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {MemoryRouter} from 'react-router-dom';
import {vi} from 'vitest';
import {ApiError} from '../../api/apiError';
import {useHalPageData} from '../../hooks/useHalPageData';
import {LegalGuardianDetailPage} from './LegalGuardianDetailPage';

vi.mock('../../hooks/useHalPageData', () => ({useHalPageData: vi.fn()}));

vi.mock('../../components/HalNavigator2/HalFormButton.tsx', () => ({
    HalFormButton: ({name}: {name: string}) => <button data-testid="hal-form-button">{name}</button>,
}));

const navigateToResource = vi.fn();

const renderPage = (resourceData: Record<string, unknown> | null, extra?: {isLoading?: boolean; error?: ApiError}) => {
    vi.mocked(useHalPageData).mockReturnValue({
        resourceData,
        isLoading: extra?.isLoading ?? false,
        error: extra?.error ?? null,
        refetch: async () => {},
        navigateToResource,
    } as unknown as ReturnType<typeof useHalPageData>);
    return render(
        <MemoryRouter>
            <LegalGuardianDetailPage/>
        </MemoryRouter>
    );
};

const buildGuardian = (overrides?: Record<string, unknown>) => ({
    userId: 'u-1',
    loginName: 'EXT0001',
    firstName: 'Jana',
    lastName: 'Nováková',
    email: 'jana@example.com',
    phone: '+420777111222',
    _links: {self: {href: '/api/legal-guardians/u-1'}},
    ...overrides,
});

describe('LegalGuardianDetailPage', () => {
    beforeEach(() => {
        vi.clearAllMocks();
    });

    it('shows name, login number, e-mail and phone', () => {
        renderPage(buildGuardian());
        expect(screen.getByRole('heading', {name: 'Jana Nováková'})).toBeInTheDocument();
        expect(screen.getByText('EXT0001')).toBeInTheDocument();
        expect(screen.getByText('jana@example.com')).toBeInTheDocument();
        expect(screen.getByText('+420777111222')).toBeInTheDocument();
    });

    it('offers the updateLegalGuardian form action', () => {
        renderPage(buildGuardian());
        expect(screen.getByTestId('hal-form-button')).toHaveTextContent('updateLegalGuardian');
    });

    it('shows an error alert on failure', () => {
        renderPage(null, {error: new ApiError(404, {type: '/errors/legal-guardian-not-found', detail: 'Boom'})});
        expect(screen.getByText('Zákonný zástupce nebyl nalezen.')).toBeInTheDocument();
        expect(screen.queryByText('Boom')).not.toBeInTheDocument();
    });

    it('hides the register-as-member button without the registerAsMember link', () => {
        renderPage(buildGuardian());
        expect(screen.queryByRole('button', {name: 'Registrovat jako člena'})).not.toBeInTheDocument();
    });

    it('opens member registration with the guardian taken over and the profile as the way back', async () => {
        renderPage(buildGuardian({
            _links: {
                self: {href: '/api/legal-guardians/u-1'},
                registerAsMember: {href: '/api/members'},
            },
        }));

        await userEvent.click(screen.getByRole('button', {name: 'Registrovat jako člena'}));

        expect(navigateToResource).toHaveBeenCalledWith({href: '/api/members'}, {
            state: {takeOverLegalGuardianId: 'u-1', returnTo: '/legal-guardians/u-1'},
        });
    });
});
