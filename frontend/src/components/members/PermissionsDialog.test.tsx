import {render, screen} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {vi} from 'vitest';
import {PermissionsDialog, type PermissionsDialogProps} from './PermissionsDialog';
import {resolvePermissionErrorMessage} from '../../hooks/usePermissionsEditor';

const renderDialog = (props?: Partial<PermissionsDialogProps>) => {
    const defaultProps: PermissionsDialogProps = {
        isOpen: true,
        onClose: vi.fn(),
        memberName: 'Jan Novák',
        memberRegistrationNumber: 'ZBM102',
        permissions: ['MEMBERS:MANAGE'],
        assignableAuthorities: [
            'MEMBERS:MANAGE',
            'MEMBERS:PERMISSIONS',
            'EVENTS:MANAGE',
            'CALENDAR:MANAGE',
            'GROUPS:TRAINING',
            'EVENTS:REGISTER_MEMBER',
            'FINANCE:MANAGE',
        ],
        isLoading: false,
        isSaving: false,
        error: null,
        onSave: vi.fn(),
    };
    const mergedProps = {...defaultProps, ...props};

    return render(<PermissionsDialog {...mergedProps} />);
};

describe('PermissionsDialog', () => {
    describe('Header', () => {
        it('shows member name and registration number in dialog title', () => {
            renderDialog();

            expect(screen.getByText('Jan Novák – ZBM102')).toBeInTheDocument();
        });

        it('shows only member name when registration number is not provided', () => {
            renderDialog({memberRegistrationNumber: undefined});

            expect(screen.getByText('Jan Novák')).toBeInTheDocument();
        });
    });

    describe('Loading state', () => {
        it('shows spinner while loading permissions', () => {
            renderDialog({isLoading: true, permissions: undefined});

            expect(screen.queryByRole('switch')).not.toBeInTheDocument();
        });
    });

    describe('Permission toggles', () => {
        it('shows all available permissions as toggle switches', () => {
            renderDialog();

            const switches = screen.getAllByRole('switch');
            expect(switches.length).toBeGreaterThan(0);
        });

        it('does not offer MEMBERS:EDIT_PROFILE because the backend options never include relationship-granted authorities', () => {
            renderDialog();

            expect(screen.queryByRole('switch', {name: /Úprava údajů člena/i})).not.toBeInTheDocument();
        });

        it.each([
            ['MEMBERS:MANAGE', 'Správa členů', 'Registrace, úprava a mazání členů'],
            ['MEMBERS:PERMISSIONS', 'Správa oprávnění', 'Přidělování a odebírání oprávnění uživatelům'],
            ['EVENTS:MANAGE', 'Správa akcí', 'Vytváření a úprava akcí'],
            ['CALENDAR:MANAGE', 'Správa kalendáře', 'Vytváření a úprava kalendářních událostí'],
            ['GROUPS:TRAINING', 'Správa tréninkových skupin', 'Umožňuje vytvářet a spravovat tréninkové skupiny a jejich členy.'],
            ['EVENTS:REGISTER_MEMBER', 'Správa přihlášek člena', 'Přihlašování, úprava a odhlašování člena z akcí'],
            ['FINANCE:MANAGE', 'Správa financí', 'Vklady, strhávání a storno transakcí na účtech členů'],
            ['SYNC:MANAGE', 'Správa synchronizace', 'Řešení konfliktů a řízení synchronizace s externími systémy'],
        ])('shows %s as "%s" with its description and reflects assigned and unassigned state', (authority, label, description) => {
            const {unmount} = renderDialog({assignableAuthorities: [authority], permissions: [authority]});

            expect(screen.getByRole('switch', {name: label})).toHaveAttribute('aria-checked', 'true');
            expect(screen.getByText(description)).toBeInTheDocument();
            unmount();

            renderDialog({assignableAuthorities: [authority], permissions: []});

            expect(screen.getByRole('switch', {name: label})).toHaveAttribute('aria-checked', 'false');
        });

        it('renders authorities in the order provided by the backend, including unknown ones', () => {
            renderDialog({assignableAuthorities: ['MEMBERS:MANAGE', 'SYNC:MANAGE', 'SOME:UNKNOWN']});

            const switches = screen.getAllByRole('switch');
            expect(switches.map(s => s.getAttribute('aria-label'))).toEqual([
                'Správa členů',
                'Správa synchronizace',
                'SOME:UNKNOWN',
            ]);
        });

        it('does not render a toggle for an authority missing from the backend catalogue', () => {
            renderDialog({assignableAuthorities: ['MEMBERS:MANAGE']});

            expect(screen.queryByRole('switch', {name: /Správa oprávnění/i})).not.toBeInTheDocument();
        });

        it('toggles permission on click', async () => {
            const user = userEvent.setup();
            renderDialog();

            const membersPermissionsToggle = screen.getByRole('switch', {name: /Správa oprávnění/i});
            expect(membersPermissionsToggle).toHaveAttribute('aria-checked', 'false');

            await user.click(membersPermissionsToggle);

            expect(membersPermissionsToggle).toHaveAttribute('aria-checked', 'true');
        });
    });

    describe('Save action', () => {
        it('calls onSave once with the selected authorities', async () => {
            const user = userEvent.setup();
            const onSave = vi.fn();
            renderDialog({onSave});

            await user.click(screen.getByText('Uložit oprávnění'));

            expect(onSave).toHaveBeenCalledTimes(1);
            expect(onSave).toHaveBeenCalledWith(['MEMBERS:MANAGE']);
        });

        it('while saving disables the toggles and the save button, shows a spinner and does not call onSave', async () => {
            const user = userEvent.setup();
            const onSave = vi.fn();
            renderDialog({isSaving: true, onSave});

            screen.getAllByRole('switch').forEach(toggle => expect(toggle).toBeDisabled());
            const saveButton = screen.getByRole('button', {name: /Uložit oprávnění/i});
            expect(saveButton).toBeDisabled();
            expect(saveButton.querySelector('.animate-spin')).toBeInTheDocument();
            await user.click(saveButton);

            expect(onSave).not.toHaveBeenCalled();
        });
    });

    describe('Cancel action', () => {
        it('calls onClose when Zrušit is clicked', async () => {
            const user = userEvent.setup();
            const onClose = vi.fn();
            renderDialog({onClose});

            await user.click(screen.getByText('Zrušit'));

            expect(onClose).toHaveBeenCalledTimes(1);
        });
    });

    describe('Error handling', () => {
        it('shows the resolved error message and keeps the dialog open', () => {
            const error = new Error('HTTP 500 (Internal Server Error)');
            renderDialog({error});

            expect(screen.getByText(resolvePermissionErrorMessage(error))).toBeInTheDocument();
            expect(screen.getByRole('dialog')).toBeInTheDocument();
        });
    });

    describe('Closed state', () => {
        it('does not render when isOpen is false', () => {
            renderDialog({isOpen: false});

            expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
        });
    });
});
