import '@testing-library/jest-dom';
import {render, screen} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {vi} from 'vitest';
import {useHalPageData} from '../../hooks/useHalPageData';
import {mockHalFormsTemplate} from '../../__mocks__/halData';
import {useLegalGuardians} from '../../hooks/useLegalGuardians.ts';
import {LegalGuardiansSection} from './LegalGuardiansSection';

vi.mock('../../hooks/useHalPageData', () => ({useHalPageData: vi.fn()}));

const modalSpy = vi.fn();
vi.mock('../HalNavigator2/HalFormModal.tsx', () => ({
    HalFormModal: (props: Record<string, unknown>) => {
        modalSpy(props);
        return <div data-testid="form-modal"/>;
    },
}));

const loadedGuardians = [{userId: 'u-1', firstName: 'Jana', lastName: 'Nováková', _links: {member: {href: '/api/members/u-1'}}}];
vi.mock('../../hooks/useLegalGuardians.ts', () => ({useLegalGuardians: vi.fn()}));

const navigateToResource = vi.fn();
const refetch = vi.fn();

beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(useLegalGuardians).mockReturnValue({guardians: loadedGuardians, isLoading: false});
    vi.mocked(useHalPageData).mockReturnValue({
        route: {pathname: '/members/m-1', navigateToResource, refetch},
    } as unknown as ReturnType<typeof useHalPageData>);
});

describe('LegalGuardiansSection', () => {
    it('offers no edit action without the template', () => {
        render(<LegalGuardiansSection guardiansLink={{href: '/api/x/guardians'}}/>);
        expect(screen.queryByRole('button', {name: 'Upravit zástupce'})).not.toBeInTheDocument();
    });

    it('opens the edit form prefilled with the current guardians and refetches the page on close', async () => {
        const onEditStarted = vi.fn();
        render(<LegalGuardiansSection
            guardiansLink={{href: '/api/x/guardians'}}
            editTemplate={mockHalFormsTemplate({method: 'PUT', target: '/api/x/legal-guardians'})}
            editTemplateName="setMemberLegalGuardians"
            onEditStarted={onEditStarted}
        />);

        await userEvent.click(screen.getByRole('button', {name: 'Upravit zástupce'}));

        expect(onEditStarted).toHaveBeenCalled();
        expect(screen.getByTestId('form-modal')).toBeInTheDocument();
        const props = modalSpy.mock.calls[0][0] as {resourceData: unknown; pathname: string; onClose: () => void};
        expect(props.resourceData).toEqual({legalGuardians: [{userId: 'u-1'}]});
        expect(props.pathname).toBe('/members/m-1');
        props.onClose();
        expect(refetch).toHaveBeenCalled();
    });

    it('disables the edit action until the guardians are loaded', () => {
        vi.mocked(useLegalGuardians).mockReturnValue({guardians: [], isLoading: true});
        render(<LegalGuardiansSection
            guardiansLink={{href: '/api/x/guardians'}}
            editTemplate={mockHalFormsTemplate({method: 'PUT', target: '/api/x/legal-guardians'})}
            editTemplateName="setMemberLegalGuardians"
        />);
        expect(screen.getByRole('button', {name: 'Upravit zástupce'})).toBeDisabled();
    });

    it('navigates to the guardian resource on click', async () => {
        render(<LegalGuardiansSection guardiansLink={{href: '/api/x/guardians'}}/>);
        await userEvent.click(screen.getByRole('button', {name: 'Jana Nováková'}));
        expect(navigateToResource).toHaveBeenCalledWith({href: '/api/members/u-1'});
    });
});
