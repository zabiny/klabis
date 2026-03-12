import {render, screen} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {vi} from 'vitest';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {PermissionsDialog, type PermissionsDialogProps} from './PermissionsDialog';
import {ToastProvider} from '../../contexts/ToastContext';
import {FetchError} from '../../api/authorizedFetch';

vi.mock('../../hooks/useAuthorizedFetch', () => ({
    useAuthorizedQuery: vi.fn(),
    useAuthorizedMutation: vi.fn(),
}));

const {useAuthorizedQuery, useAuthorizedMutation} = await import('../../hooks/useAuthorizedFetch');

const mockUseAuthorizedQuery = useAuthorizedQuery as ReturnType<typeof vi.fn>;
const mockUseAuthorizedMutation = useAuthorizedMutation as ReturnType<typeof vi.fn>;

const createQueryClient = () =>
    new QueryClient({defaultOptions: {queries: {retry: false}}});

const renderDialog = (props?: Partial<PermissionsDialogProps>) => {
    const defaultProps = {
        isOpen: true,
        onClose: vi.fn(),
        permissionsUrl: '/api/users/1/permissions',
        memberName: 'Jan Novák',
        memberRegistrationNumber: 'ZBM102',
    };
    const mergedProps = {...defaultProps, ...props};

    return render(
        <QueryClientProvider client={createQueryClient()}>
            <ToastProvider>
                <PermissionsDialog {...mergedProps} />
            </ToastProvider>
        </QueryClientProvider>,
    );
};

describe('PermissionsDialog', () => {
    const defaultMutate = vi.fn();

    beforeEach(() => {
        mockUseAuthorizedQuery.mockReturnValue({
            data: {
                authorities: ['MEMBERS:READ', 'EVENTS:READ'],
                _links: {self: {href: '/api/users/1/permissions'}},
            },
            isLoading: false,
        });

        mockUseAuthorizedMutation.mockReturnValue({
            mutate: defaultMutate,
            isPending: false,
            error: null,
        });
    });

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
            mockUseAuthorizedQuery.mockReturnValue({
                data: undefined,
                isLoading: true,
            });

            renderDialog();

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

            const membersReadToggle = screen.getByRole('switch', {name: /Zobrazení členů/i});
            expect(membersReadToggle).toHaveAttribute('aria-checked', 'true');
        });

        it('shows unassigned permissions as toggled off', () => {
            renderDialog();

            const membersManageToggle = screen.getByRole('switch', {name: /Správa členů/i});
            expect(membersManageToggle).toHaveAttribute('aria-checked', 'false');
        });

        it('toggles permission on click', async () => {
            const user = userEvent.setup();
            renderDialog();

            const membersManageToggle = screen.getByRole('switch', {name: /Správa členů/i});
            expect(membersManageToggle).toHaveAttribute('aria-checked', 'false');

            await user.click(membersManageToggle);

            expect(membersManageToggle).toHaveAttribute('aria-checked', 'true');
        });

        it('disables toggles while saving', () => {
            mockUseAuthorizedMutation.mockReturnValue({
                mutate: defaultMutate,
                isPending: true,
                error: null,
            });

            renderDialog();

            const switches = screen.getAllByRole('switch');
            switches.forEach(toggle => {
                expect(toggle).toBeDisabled();
            });
        });
    });

    describe('Save action', () => {
        it('calls PUT with selected authorities on save', async () => {
            const user = userEvent.setup();
            renderDialog();

            await user.click(screen.getByText('Uložit oprávnění'));

            expect(defaultMutate).toHaveBeenCalledWith(
                {
                    url: '/api/users/1/permissions',
                    data: {authorities: expect.arrayContaining(['MEMBERS:READ', 'EVENTS:READ'])},
                },
                expect.objectContaining({onSuccess: expect.any(Function)}),
            );
        });

        it('calls mutate exactly once per save click', async () => {
            const user = userEvent.setup();
            renderDialog();

            await user.click(screen.getByText('Uložit oprávnění'));

            expect(defaultMutate).toHaveBeenCalledTimes(1);
        });

        it('does not call mutate when save button is disabled during pending', async () => {
            const user = userEvent.setup();
            const guardedMutate = vi.fn();
            mockUseAuthorizedMutation.mockReturnValue({
                mutate: guardedMutate,
                isPending: true,
                error: null,
            });
            renderDialog();

            const saveButton = screen.getByRole('button', {name: /Loading/i});
            expect(saveButton).toBeDisabled();
            await user.click(saveButton);

            expect(guardedMutate).not.toHaveBeenCalled();
        });

        it('invalidates permissions query cache after successful save', async () => {
            const user = userEvent.setup();
            const queryClient = createQueryClient();
            const invalidateQueriesSpy = vi.spyOn(queryClient, 'invalidateQueries');

            mockUseAuthorizedMutation.mockReturnValue({
                mutate: defaultMutate,
                isPending: false,
                error: null,
            });

            render(
                <QueryClientProvider client={queryClient}>
                    <ToastProvider>
                        <PermissionsDialog
                            isOpen={true}
                            onClose={vi.fn()}
                            permissionsUrl="/api/users/1/permissions"
                            memberName="Jan Novák"
                        />
                    </ToastProvider>
                </QueryClientProvider>,
            );

            await user.click(screen.getByText('Uložit oprávnění'));

            const [, callOptions] = defaultMutate.mock.calls[0];
            callOptions!.onSuccess(undefined);

            expect(invalidateQueriesSpy).toHaveBeenCalledWith(
                expect.objectContaining({queryKey: ['authorized']}),
            );
        });

        it('shows spinner in save button while pending', () => {
            mockUseAuthorizedMutation.mockReturnValue({
                mutate: defaultMutate,
                isPending: true,
                error: null,
            });

            renderDialog();

            expect(screen.queryByText('Uložit oprávnění')).not.toBeInTheDocument();
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
            mockUseAuthorizedMutation.mockReturnValue({
                mutate: defaultMutate,
                isPending: false,
                error: new Error('HTTP 500 (Internal Server Error)'),
            });

            renderDialog();

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

            mockUseAuthorizedMutation.mockReturnValue({
                mutate: defaultMutate,
                isPending: false,
                error: conflictError,
            });

            renderDialog();

            expect(screen.getByText(/Nelze odebrat oprávnění/i)).toBeInTheDocument();
        });

        it('keeps dialog open when error occurs', () => {
            mockUseAuthorizedMutation.mockReturnValue({
                mutate: defaultMutate,
                isPending: false,
                error: new Error('Some error'),
            });

            renderDialog();

            expect(screen.getByRole('dialog')).toBeInTheDocument();
        });
    });

    describe('Closed state', () => {
        it('does not render when isOpen is false', () => {
            renderDialog({isOpen: false});

            expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
        });

        it('does not fetch permissions when dialog is closed', () => {
            renderDialog({isOpen: false});

            expect(mockUseAuthorizedQuery).toHaveBeenCalledWith(
                expect.any(String),
                expect.objectContaining({enabled: false}),
            );
        });
    });
});
