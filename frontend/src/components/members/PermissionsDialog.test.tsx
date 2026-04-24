import {render, screen} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {vi} from 'vitest';
import {PermissionsDialog, type PermissionsDialogProps} from './PermissionsDialog';
import {FetchError} from '../../api/authorizedFetch';

const renderDialog = (props?: Partial<PermissionsDialogProps>) => {
    const defaultProps: PermissionsDialogProps = {
        isOpen: true,
        onClose: vi.fn(),
        memberName: 'Jan Novák',
        memberRegistrationNumber: 'ZBM102',
        permissions: ['MEMBERS:MANAGE'],
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

        it('pre-selects permissions currently assigned to the user', () => {
            renderDialog();

            const membersManageToggle = screen.getByRole('switch', {name: /Správa členů/i});
            expect(membersManageToggle).toHaveAttribute('aria-checked', 'true');
        });

        it('shows unassigned permissions as toggled off', () => {
            renderDialog();

            const membersPermissionsToggle = screen.getByRole('switch', {name: /Správa oprávnění/i});
            expect(membersPermissionsToggle).toHaveAttribute('aria-checked', 'false');
        });

        it('renders a toggle for GROUPS:TRAINING permission', () => {
            renderDialog();

            const toggle = screen.getByRole('switch', {name: /Správa tréninkových skupin/i});
            expect(toggle).toBeInTheDocument();
        });

        it('reflects current state for GROUPS:TRAINING when assigned', () => {
            renderDialog({permissions: ['GROUPS:TRAINING']});

            const toggle = screen.getByRole('switch', {name: /Správa tréninkových skupin/i});
            expect(toggle).toHaveAttribute('aria-checked', 'true');
        });

        it('reflects current state for GROUPS:TRAINING when not assigned', () => {
            renderDialog();

            const toggle = screen.getByRole('switch', {name: /Správa tréninkových skupin/i});
            expect(toggle).toHaveAttribute('aria-checked', 'false');
        });

        it('renders a toggle for EVENTS:REGISTRATIONS permission with label "Správa přihlášek"', () => {
            renderDialog();

            const toggle = screen.getByRole('switch', {name: /Správa přihlášek/i});
            expect(toggle).toBeInTheDocument();
        });

        it('shows description "Editace přihlášek ostatních členů na akce" for EVENTS:REGISTRATIONS', () => {
            renderDialog();

            expect(screen.getByText('Editace přihlášek ostatních členů na akce')).toBeInTheDocument();
        });

        it('reflects current state for EVENTS:REGISTRATIONS when assigned', () => {
            renderDialog({permissions: ['EVENTS:REGISTRATIONS']});

            const toggle = screen.getByRole('switch', {name: /Správa přihlášek/i});
            expect(toggle).toHaveAttribute('aria-checked', 'true');
        });

        it('reflects current state for EVENTS:REGISTRATIONS when not assigned', () => {
            renderDialog();

            const toggle = screen.getByRole('switch', {name: /Správa přihlášek/i});
            expect(toggle).toHaveAttribute('aria-checked', 'false');
        });

        it('toggles permission on click', async () => {
            const user = userEvent.setup();
            renderDialog();

            const membersPermissionsToggle = screen.getByRole('switch', {name: /Správa oprávnění/i});
            expect(membersPermissionsToggle).toHaveAttribute('aria-checked', 'false');

            await user.click(membersPermissionsToggle);

            expect(membersPermissionsToggle).toHaveAttribute('aria-checked', 'true');
        });

        it('disables toggles while saving', () => {
            renderDialog({isSaving: true});

            const switches = screen.getAllByRole('switch');
            switches.forEach(toggle => {
                expect(toggle).toBeDisabled();
            });
        });
    });

    describe('Save action', () => {
        it('calls onSave with selected authorities on save', async () => {
            const user = userEvent.setup();
            const onSave = vi.fn();
            renderDialog({onSave});

            await user.click(screen.getByText('Uložit oprávnění'));

            expect(onSave).toHaveBeenCalledWith(
                expect.arrayContaining(['MEMBERS:MANAGE']),
            );
        });

        it('calls onSave exactly once per save click', async () => {
            const user = userEvent.setup();
            const onSave = vi.fn();
            renderDialog({onSave});

            await user.click(screen.getByText('Uložit oprávnění'));

            expect(onSave).toHaveBeenCalledTimes(1);
        });

        it('does not call onSave when save button is disabled during pending', async () => {
            const user = userEvent.setup();
            const onSave = vi.fn();
            renderDialog({isSaving: true, onSave});

            const saveButton = screen.getByRole('button', {name: /Uložit oprávnění/i});
            expect(saveButton).toBeDisabled();
            await user.click(saveButton);

            expect(onSave).not.toHaveBeenCalled();
        });

        it('shows spinner in save button while pending', () => {
            renderDialog({isSaving: true});

            const saveButton = screen.getByRole('button', {name: /Uložit oprávnění/i});
            expect(saveButton).toBeDisabled();
            expect(saveButton.querySelector('.animate-spin')).toBeInTheDocument();
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
        it('shows generic error message when save fails', () => {
            renderDialog({error: new Error('HTTP 500 (Internal Server Error)')});

            expect(screen.getByText(/Nepodařilo se uložit oprávnění/i)).toBeInTheDocument();
        });

        it('shows user-friendly Czech message when admin lockout is detected (409)', () => {
            const conflictError = new FetchError(
                'HTTP 409 (Conflict)',
                409,
                'Conflict',
                new Headers(),
                undefined,
            );

            renderDialog({error: conflictError});

            expect(screen.getByText(/Nelze odebrat oprávnění/i)).toBeInTheDocument();
        });

        it('keeps dialog open when error occurs', () => {
            renderDialog({error: new Error('Some error')});

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
