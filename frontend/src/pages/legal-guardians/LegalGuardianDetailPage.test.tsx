import '@testing-library/jest-dom';
import {render, screen} from '@testing-library/react';
import {MemoryRouter} from 'react-router-dom';
import {vi} from 'vitest';
import {useHalPageData} from '../../hooks/useHalPageData';
import {LegalGuardianDetailPage} from './LegalGuardianDetailPage';

vi.mock('../../hooks/useHalPageData', () => ({useHalPageData: vi.fn()}));

vi.mock('../../components/HalNavigator2/HalFormButton.tsx', () => ({
    HalFormButton: ({name}: {name: string}) => <button data-testid="hal-form-button">{name}</button>,
}));

const renderPage = (resourceData: Record<string, unknown> | null, extra?: {isLoading?: boolean; error?: Error}) => {
    vi.mocked(useHalPageData).mockReturnValue({
        resourceData,
        isLoading: extra?.isLoading ?? false,
        error: extra?.error ?? null,
        route: {pathname: '/legal-guardians/u-1', refetch: async () => {}},
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
        renderPage(null, {error: new Error('Boom')});
        expect(screen.getByText('Boom')).toBeInTheDocument();
    });
});
