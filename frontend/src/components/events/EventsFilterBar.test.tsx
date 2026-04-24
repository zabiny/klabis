import '@testing-library/jest-dom';
import { render, screen, act } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { vi, describe, it, expect, beforeEach } from 'vitest';
import { EventsFilterBar, type EventsFilterBarProps } from './EventsFilterBar';
import { labels } from '../../localization';
import { useAuth } from '../../contexts/AuthContext2';

vi.mock('../../contexts/AuthContext2', () => ({
    useAuth: vi.fn().mockReturnValue({
        getUser: () => ({
            memberId: 'M001',
            firstName: 'Jana',
            lastName: 'Novak',
            id: 1,
            userName: 'ZBM9500',
        }),
    }),
}));

const defaultProps: EventsFilterBarProps = {
    timeWindow: 'budouci',
    onTimeWindowChange: vi.fn(),
    registeredByMe: false,
    onRegisteredByMeChange: vi.fn(),
    searchQuery: '',
    onSearchChange: vi.fn(),
};

const renderFilterBar = (props = defaultProps) =>
    render(
        <MemoryRouter initialEntries={['/']}>
            <EventsFilterBar {...props} />
        </MemoryRouter>,
    );

const mockUseAuthWithMember = () =>
    vi.mocked(useAuth).mockReturnValue({
        getUser: () => ({
            memberId: 'M001',
            firstName: 'Jana',
            lastName: 'Novak',
            id: 1,
            userName: 'ZBM9500',
        }),
        isAuthenticated: true,
        login: vi.fn(),
        logout: vi.fn(),
        isLoading: false,
    });

describe('EventsFilterBar', () => {
    beforeEach(() => {
        vi.clearAllMocks();
        mockUseAuthWithMember();
    });

    describe('search input', () => {
        it('renders search input with correct placeholder', () => {
            renderFilterBar();
            expect(
                screen.getByPlaceholderText(labels.eventsFilter.searchPlaceholder),
            ).toBeInTheDocument();
        });

        it('renders search input with correct aria-label', () => {
            renderFilterBar();
            expect(
                screen.getByRole('textbox', { name: labels.eventsFilter.search }),
            ).toBeInTheDocument();
        });

        it('shows value from searchQuery prop', () => {
            renderFilterBar({ ...defaultProps, searchQuery: 'jihlava' });
            expect(screen.getByDisplayValue('jihlava')).toBeInTheDocument();
        });

        it('accepts user input', async () => {
            const user = userEvent.setup();
            renderFilterBar();
            const input = screen.getByPlaceholderText(labels.eventsFilter.searchPlaceholder);
            await user.type(input, 'test');
            expect(input).toHaveValue('test');
        });
    });

    describe('time window pill group', () => {
        it('renders three time window options', () => {
            renderFilterBar();
            expect(
                screen.getByRole('button', { name: labels.eventsFilter.budouci }),
            ).toBeInTheDocument();
            expect(
                screen.getByRole('button', { name: labels.eventsFilter.probehle }),
            ).toBeInTheDocument();
            expect(
                screen.getByRole('button', { name: labels.eventsFilter.vse }),
            ).toBeInTheDocument();
        });

        it('highlights the active time window (Budoucí)', () => {
            renderFilterBar({ ...defaultProps, timeWindow: 'budouci' });
            const activeBtn = screen.getByRole('button', { name: labels.eventsFilter.budouci });
            expect(activeBtn).toHaveAttribute('aria-pressed', 'true');
        });

        it('highlights the active time window (Proběhlé)', () => {
            renderFilterBar({ ...defaultProps, timeWindow: 'probehle' });
            const activeBtn = screen.getByRole('button', { name: labels.eventsFilter.probehle });
            expect(activeBtn).toHaveAttribute('aria-pressed', 'true');
        });

        it('calls onTimeWindowChange when Proběhlé is clicked', async () => {
            const onTimeWindowChange = vi.fn();
            renderFilterBar({ ...defaultProps, onTimeWindowChange });
            await userEvent.click(screen.getByRole('button', { name: labels.eventsFilter.probehle }));
            expect(onTimeWindowChange).toHaveBeenCalledWith('probehle');
        });

        it('calls onTimeWindowChange when Vše is clicked', async () => {
            const onTimeWindowChange = vi.fn();
            renderFilterBar({ ...defaultProps, onTimeWindowChange });
            await userEvent.click(screen.getByRole('button', { name: labels.eventsFilter.vse }));
            expect(onTimeWindowChange).toHaveBeenCalledWith('vse');
        });
    });

    describe('"Moje přihlášky" checkbox', () => {
        it('shows checkbox when user has a member profile', () => {
            renderFilterBar();
            expect(
                screen.getByRole('checkbox', { name: labels.eventsFilter.mojePřihlaskyLabel }),
            ).toBeInTheDocument();
        });

        it('hides checkbox when user has no member profile', () => {
            vi.mocked(useAuth).mockReturnValue({
                getUser: () => ({
                    memberId: null,
                    firstName: 'Admin',
                    lastName: 'User',
                    id: 2,
                    userName: 'ZBM9000',
                }),
                isAuthenticated: true,
                login: vi.fn(),
                logout: vi.fn(),
                isLoading: false,
            });
            renderFilterBar();
            expect(
                screen.queryByRole('checkbox', { name: labels.eventsFilter.mojePřihlaskyLabel }),
            ).not.toBeInTheDocument();
        });

        it('reflects checked state from prop', () => {
            renderFilterBar({ ...defaultProps, registeredByMe: true });
            const checkbox = screen.getByRole('checkbox', {
                name: labels.eventsFilter.mojePřihlaskyLabel,
            });
            expect(checkbox).toBeChecked();
        });

        it('calls onRegisteredByMeChange when checkbox is toggled', async () => {
            const onRegisteredByMeChange = vi.fn();
            renderFilterBar({ ...defaultProps, onRegisteredByMeChange });
            const checkbox = screen.getByRole('checkbox', {
                name: labels.eventsFilter.mojePřihlaskyLabel,
            });
            await act(async () => {
                await userEvent.click(checkbox);
            });
            expect(onRegisteredByMeChange).toHaveBeenCalledWith(true);
        });
    });
});
