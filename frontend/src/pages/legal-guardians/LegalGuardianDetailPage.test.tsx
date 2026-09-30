import '@testing-library/jest-dom';
import {render, screen} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {MemoryRouter} from 'react-router-dom';
import {vi} from 'vitest';
import {useHalPageData} from '../../hooks/useHalPageData';
import {mockHalFormsTemplate} from '../../__mocks__/halData';
import {LegalGuardianDetailPage} from './LegalGuardianDetailPage';

vi.mock('../../hooks/useHalPageData', () => ({useHalPageData: vi.fn()}));

const modalSpy = vi.fn();
vi.mock('../../components/HalNavigator2/HalFormModal.tsx', () => ({
    HalFormModal: (props: Record<string, unknown>) => {
        modalSpy(props);
        return <div data-testid="form-modal"/>;
    },
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

    it('hides the edit button without the template', () => {
        renderPage(buildGuardian());
        expect(screen.queryByRole('button', {name: 'Upravit profil'})).not.toBeInTheDocument();
    });

    it('opens the edit form prefilled with current values', async () => {
        renderPage(buildGuardian({
            _templates: {
                updateLegalGuardian: mockHalFormsTemplate({method: 'PATCH', target: '/api/legal-guardians/u-1'}),
            },
        }));
        await userEvent.click(screen.getByRole('button', {name: 'Upravit profil'}));
        expect(screen.getByTestId('form-modal')).toBeInTheDocument();
        expect(modalSpy).toHaveBeenCalledWith(expect.objectContaining({
            templateName: 'updateLegalGuardian',
            resourceData: expect.objectContaining({firstName: 'Jana', email: 'jana@example.com'}),
        }));
    });

    it('shows an error alert on failure', () => {
        renderPage(null, {error: new Error('Boom')});
        expect(screen.getByText('Boom')).toBeInTheDocument();
    });
});
