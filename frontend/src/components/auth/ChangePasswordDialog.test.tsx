import '@testing-library/jest-dom';
import {render, screen, waitFor} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {vi} from 'vitest';
import {ChangePasswordDialog} from './ChangePasswordDialog';
import {useAuthorizedMutation, type MutationResult} from '../../hooks/useAuthorizedFetch';
import {FetchError} from '../../api/authorizedFetch';

vi.mock('../../hooks/useAuthorizedFetch', () => ({
    useAuthorizedMutation: vi.fn(),
}));

const mockMutation = (overrides?: Partial<ReturnType<typeof useAuthorizedMutation>>) => {
    vi.mocked(useAuthorizedMutation).mockReturnValue({
        mutate: vi.fn(),
        mutateAsync: vi.fn().mockResolvedValue(undefined),
        isPending: false,
        isSuccess: false,
        isIdle: true,
        isError: false,
        data: undefined,
        error: null,
        reset: vi.fn(),
        status: 'idle',
        variables: undefined,
        context: undefined,
        failureCount: 0,
        failureReason: null,
        submittedAt: 0,
        ...overrides,
    } as unknown as ReturnType<typeof useAuthorizedMutation>);
};

const renderDialog = (props?: Partial<{isOpen: boolean; onClose: () => void}>) => {
    return render(
        <ChangePasswordDialog
            isOpen={props?.isOpen ?? true}
            onClose={props?.onClose ?? vi.fn()}
        />
    );
};

const getInput = (name: string) =>
    document.querySelector<HTMLInputElement>(`input[name="${name}"]`)!;

describe('ChangePasswordDialog', () => {
    beforeEach(() => {
        vi.clearAllMocks();
        mockMutation();
    });

    describe('dialog visibility', () => {
        it('renders nothing when isOpen is false', () => {
            renderDialog({isOpen: false});
            expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
        });

        it('renders dialog when isOpen is true', () => {
            renderDialog({isOpen: true});
            expect(screen.getByRole('dialog')).toBeInTheDocument();
        });

        it('shows dialog title', () => {
            renderDialog();
            expect(screen.getByText('Změna hesla')).toBeInTheDocument();
        });
    });

    describe('form fields', () => {
        it('renders current password field', () => {
            renderDialog();
            expect(getInput('currentPassword')).toBeInTheDocument();
        });

        it('renders new password field', () => {
            renderDialog();
            expect(getInput('newPassword')).toBeInTheDocument();
        });

        it('renders confirm password field', () => {
            renderDialog();
            expect(getInput('confirmPassword')).toBeInTheDocument();
        });

        it('renders submit button', () => {
            renderDialog();
            expect(screen.getByRole('button', {name: /Změnit heslo/i})).toBeInTheDocument();
        });

        it('renders cancel button', () => {
            renderDialog();
            expect(screen.getByRole('button', {name: /Zrušit/i})).toBeInTheDocument();
        });
    });

    describe('client-side validation', () => {
        it('shows password requirements indicator when new password field is not empty', async () => {
            const user = userEvent.setup();
            renderDialog();

            await user.type(getInput('newPassword'), 'abc');

            expect(screen.getByRole('list', {name: /Požadavky na heslo/i})).toBeInTheDocument();
        });

        it('blocks submission when new password does not meet complexity requirements', async () => {
            const user = userEvent.setup();
            const mutateMock = vi.fn();
            mockMutation({mutate: mutateMock});
            renderDialog();

            await user.type(getInput('currentPassword'), 'OldPassword1!');
            await user.type(getInput('newPassword'), 'weak');
            await user.type(getInput('confirmPassword'), 'weak');
            await user.click(screen.getByRole('button', {name: /Změnit heslo/i}));

            expect(mutateMock).not.toHaveBeenCalled();
            expect(screen.getByText(/Heslo nesplňuje všechny požadavky/i)).toBeInTheDocument();
        });

        it('blocks submission when confirmation does not match new password', async () => {
            const user = userEvent.setup();
            const mutateMock = vi.fn();
            mockMutation({mutate: mutateMock});
            renderDialog();

            await user.type(getInput('currentPassword'), 'OldPassword1!');
            await user.type(getInput('newPassword'), 'NewStrongPassword1!');
            await user.type(getInput('confirmPassword'), 'DifferentPassword1!');
            await user.click(screen.getByRole('button', {name: /Změnit heslo/i}));

            expect(mutateMock).not.toHaveBeenCalled();
            expect(screen.getByText(/Hesla se neshodují/i)).toBeInTheDocument();
        });

        it('blocks submission when current password is empty', async () => {
            const user = userEvent.setup();
            const mutateMock = vi.fn();
            mockMutation({mutate: mutateMock});
            renderDialog();

            await user.type(getInput('newPassword'), 'NewStrongPassword1!');
            await user.type(getInput('confirmPassword'), 'NewStrongPassword1!');
            await user.click(screen.getByRole('button', {name: /Změnit heslo/i}));

            expect(mutateMock).not.toHaveBeenCalled();
        });
    });

    describe('successful submission', () => {
        it('calls POST /api/me/password-change with correct payload', async () => {
            const user = userEvent.setup();
            const mutateMock = vi.fn();
            mockMutation({mutate: mutateMock});
            renderDialog();

            await user.type(getInput('currentPassword'), 'OldPassword1!');
            await user.type(getInput('newPassword'), 'NewStrongPassword1!');
            await user.type(getInput('confirmPassword'), 'NewStrongPassword1!');
            await user.click(screen.getByRole('button', {name: /Změnit heslo/i}));

            expect(mutateMock).toHaveBeenCalledWith(
                {
                    url: '/api/me/password-change',
                    data: {
                        currentPassword: 'OldPassword1!',
                        newPassword: 'NewStrongPassword1!',
                    },
                },
                expect.any(Object),
            );
        });

        it('calls onClose after successful password change', async () => {
            const user = userEvent.setup();
            const onCloseMock = vi.fn();
            let onSuccessCallback: ((result: MutationResult) => void) | undefined;

            vi.mocked(useAuthorizedMutation).mockReturnValue({
                mutate: vi.fn().mockImplementation((_vars, opts) => {
                    onSuccessCallback = opts?.onSuccess;
                }),
                mutateAsync: vi.fn(),
                isPending: false,
                isSuccess: false,
                isIdle: true,
                isError: false,
                data: undefined,
                error: null,
                reset: vi.fn(),
                status: 'idle',
                variables: undefined,
                context: undefined,
                failureCount: 0,
                failureReason: null,
                submittedAt: 0,
            } as unknown as ReturnType<typeof useAuthorizedMutation>);

            renderDialog({onClose: onCloseMock});

            await user.type(getInput('currentPassword'), 'OldPassword1!');
            await user.type(getInput('newPassword'), 'NewStrongPassword1!');
            await user.type(getInput('confirmPassword'), 'NewStrongPassword1!');
            await user.click(screen.getByRole('button', {name: /Změnit heslo/i}));

            onSuccessCallback?.({data: null, location: null});

            await waitFor(() => {
                expect(onCloseMock).toHaveBeenCalled();
            });
        });
    });

    describe('server-side error handling', () => {
        it('shows server error message inline when mutation fails', async () => {
            const user = userEvent.setup();
            let onErrorCallback: ((error: Error) => void) | undefined;

            vi.mocked(useAuthorizedMutation).mockReturnValue({
                mutate: vi.fn().mockImplementation((_vars, opts) => {
                    onErrorCallback = opts?.onError;
                }),
                mutateAsync: vi.fn(),
                isPending: false,
                isSuccess: false,
                isIdle: true,
                isError: false,
                data: undefined,
                error: null,
                reset: vi.fn(),
                status: 'idle',
                variables: undefined,
                context: undefined,
                failureCount: 0,
                failureReason: null,
                submittedAt: 0,
            } as unknown as ReturnType<typeof useAuthorizedMutation>);

            renderDialog();

            await user.type(getInput('currentPassword'), 'WrongPassword1!');
            await user.type(getInput('newPassword'), 'NewStrongPassword1!');
            await user.type(getInput('confirmPassword'), 'NewStrongPassword1!');
            await user.click(screen.getByRole('button', {name: /Změnit heslo/i}));

            const serverError = new FetchError('HTTP 400 (Bad Request)', 400, 'Bad Request', new Headers(), JSON.stringify({detail: 'Current password is incorrect'}));
            onErrorCallback?.(serverError);

            await waitFor(() => {
                expect(screen.getByText(/Zadané aktuální heslo je nesprávné/i)).toBeInTheDocument();
            });
        });

        it('shows generic error message for non-password errors', async () => {
            const user = userEvent.setup();
            let onErrorCallback: ((error: Error) => void) | undefined;

            vi.mocked(useAuthorizedMutation).mockReturnValue({
                mutate: vi.fn().mockImplementation((_vars, opts) => {
                    onErrorCallback = opts?.onError;
                }),
                mutateAsync: vi.fn(),
                isPending: false,
                isSuccess: false,
                isIdle: true,
                isError: false,
                data: undefined,
                error: null,
                reset: vi.fn(),
                status: 'idle',
                variables: undefined,
                context: undefined,
                failureCount: 0,
                failureReason: null,
                submittedAt: 0,
            } as unknown as ReturnType<typeof useAuthorizedMutation>);

            renderDialog();

            await user.type(getInput('currentPassword'), 'OldPassword1!');
            await user.type(getInput('newPassword'), 'NewStrongPassword1!');
            await user.type(getInput('confirmPassword'), 'NewStrongPassword1!');
            await user.click(screen.getByRole('button', {name: /Změnit heslo/i}));

            const serverError = new FetchError('HTTP 400 (Bad Request)', 400, 'Bad Request', new Headers(), JSON.stringify({detail: 'Password does not meet complexity requirements'}));
            onErrorCallback?.(serverError);

            await waitFor(() => {
                expect(screen.getByRole('alert')).toBeInTheDocument();
            });
        });

        it('clears server error when user starts typing in current password', async () => {
            const user = userEvent.setup();
            let onErrorCallback: ((error: Error) => void) | undefined;

            vi.mocked(useAuthorizedMutation).mockReturnValue({
                mutate: vi.fn().mockImplementation((_vars, opts) => {
                    onErrorCallback = opts?.onError;
                }),
                mutateAsync: vi.fn(),
                isPending: false,
                isSuccess: false,
                isIdle: true,
                isError: false,
                data: undefined,
                error: null,
                reset: vi.fn(),
                status: 'idle',
                variables: undefined,
                context: undefined,
                failureCount: 0,
                failureReason: null,
                submittedAt: 0,
            } as unknown as ReturnType<typeof useAuthorizedMutation>);

            renderDialog();

            await user.type(getInput('currentPassword'), 'WrongPassword1!');
            await user.type(getInput('newPassword'), 'NewStrongPassword1!');
            await user.type(getInput('confirmPassword'), 'NewStrongPassword1!');
            await user.click(screen.getByRole('button', {name: /Změnit heslo/i}));

            const serverError = new FetchError('HTTP 400 (Bad Request)', 400, 'Bad Request', new Headers(), JSON.stringify({detail: 'Current password is incorrect'}));
            onErrorCallback?.(serverError);

            await waitFor(() => {
                expect(screen.getByText(/Zadané aktuální heslo je nesprávné/i)).toBeInTheDocument();
            });

            await user.type(getInput('currentPassword'), 'X');

            await waitFor(() => {
                expect(screen.queryByText(/Zadané aktuální heslo je nesprávné/i)).not.toBeInTheDocument();
            });
        });
    });

    describe('cancel behavior', () => {
        it('calls onClose when cancel button is clicked', async () => {
            const user = userEvent.setup();
            const onCloseMock = vi.fn();
            renderDialog({onClose: onCloseMock});

            await user.click(screen.getByRole('button', {name: /Zrušit/i}));

            expect(onCloseMock).toHaveBeenCalled();
        });
    });

    describe('submit button state', () => {
        it('shows submit button as disabled when mutation is pending', () => {
            mockMutation({isPending: true});
            renderDialog();

            expect(screen.getByRole('button', {name: /Změnit heslo/i})).toBeDisabled();
        });
    });
});
