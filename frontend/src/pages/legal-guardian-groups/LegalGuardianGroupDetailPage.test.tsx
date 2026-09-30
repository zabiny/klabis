import '@testing-library/jest-dom';
import {render, screen} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {MemoryRouter} from 'react-router-dom';
import {vi} from 'vitest';
import {useHalPageData} from '../../hooks/useHalPageData';
import {mockHalFormsTemplate} from '../../__mocks__/halData';
import {LegalGuardianGroupDetailPage} from './LegalGuardianGroupDetailPage';

vi.mock('../../hooks/useHalPageData', () => ({useHalPageData: vi.fn()}));

const modalSpy = vi.fn();
vi.mock('../../components/HalNavigator2/HalFormModal.tsx', () => ({
    HalFormModal: (props: Record<string, unknown>) => {
        modalSpy(props);
        return <div data-testid="form-modal"/>;
    },
}));

vi.mock('../../contexts/HalRouteContext.tsx', () => ({
    HalRouteProvider: ({children}: {children: React.ReactNode}) => <>{children}</>,
}));

vi.mock('../../contexts/halRouteContext.ts', () => ({
    useHalRoute: vi.fn(() => ({
        resourceData: {firstName: 'Jana', lastName: 'Nováková', registrationNumber: 'ZBM2000'},
    })),
}));

const navigateToResource = vi.fn();

const renderPage = (resourceData: Record<string, unknown>) => {
    vi.mocked(useHalPageData).mockReturnValue({
        resourceData,
        isLoading: false,
        error: null,
        route: {pathname: '/legal-guardian-groups/g-1', navigateToResource, refetch: async () => {}},
    } as unknown as ReturnType<typeof useHalPageData>);
    return render(
        <MemoryRouter>
            <LegalGuardianGroupDetailPage/>
        </MemoryRouter>
    );
};

const buildGroup = (overrides?: Record<string, unknown>) => ({
    id: 'g-1',
    name: 'Nováková a Svobodová',
    guardians: [{userId: 'u-1', _links: {member: {href: '/api/members/u-1'}}}],
    minors: [{memberId: 'm-1', joinedAt: '2026-01-01T10:00:00Z', _links: {member: {href: '/api/members/m-1'}}}],
    _links: {self: {href: '/api/legal-guardian-groups/g-1'}},
    ...overrides,
});

describe('LegalGuardianGroupDetailPage', () => {
    beforeEach(() => {
        vi.clearAllMocks();
    });

    it('shows group name, guardians and minors', () => {
        renderPage(buildGroup());
        expect(screen.getByRole('heading', {name: 'Nováková a Svobodová'})).toBeInTheDocument();
        expect(screen.getByText('ZÁKONNÍ ZÁSTUPCI')).toBeInTheDocument();
        expect(screen.getByText('NEZLETILÍ')).toBeInTheDocument();
        expect(screen.getAllByText('Jana Nováková (ZBM2000)').length).toBeGreaterThan(0);
    });

    it('navigates to guardian member on click', async () => {
        renderPage(buildGroup());
        await userEvent.click(screen.getByRole('button', {name: /Jana Nováková/}));
        expect(navigateToResource).toHaveBeenCalledWith({href: '/api/members/u-1'});
    });

    it('offers no create, delete or add-member actions', () => {
        renderPage(buildGroup());
        expect(screen.queryByRole('button', {name: /smazat|vytvořit|přidat/i})).not.toBeInTheDocument();
    });

    it('hides "Upravit zástupce" without the template', () => {
        renderPage(buildGroup());
        expect(screen.queryByRole('button', {name: 'Upravit zástupce'})).not.toBeInTheDocument();
    });

    it('opens the guardians form prefilled with current guardians', async () => {
        renderPage(buildGroup({
            _templates: {
                setLegalGuardianGroupGuardians: mockHalFormsTemplate({
                    method: 'PUT',
                    target: '/api/legal-guardian-groups/g-1/guardians',
                }),
            },
        }));
        await userEvent.click(screen.getByRole('button', {name: 'Upravit zástupce'}));
        expect(screen.getByTestId('form-modal')).toBeInTheDocument();
        expect(modalSpy).toHaveBeenCalledWith(expect.objectContaining({
            templateName: 'setLegalGuardianGroupGuardians',
            resourceData: {legalGuardians: [{userId: 'u-1'}]},
        }));
    });
});
