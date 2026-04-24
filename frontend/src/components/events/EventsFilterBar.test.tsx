import '@testing-library/jest-dom';
import { render, screen, act, fireEvent } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { vi, describe, it, expect, beforeEach } from 'vitest';
import { EventsFilterBar, type EventsFilterBarProps, type EventsFilterValue } from './EventsFilterBar';
import { labels } from '../../localization';

const defaultValue: EventsFilterValue = {
    q: '',
    timeWindow: 'budouci',
    registeredByMe: false,
};

const defaultProps: EventsFilterBarProps = {
    value: defaultValue,
    onChange: vi.fn(),
    showRegisteredByMeToggle: true,
};

const renderFilterBar = (props: Partial<EventsFilterBarProps> = {}) =>
    render(
        <MemoryRouter initialEntries={['/']}>
            <EventsFilterBar {...defaultProps} {...props} />
        </MemoryRouter>,
    );

describe('EventsFilterBar', () => {
    beforeEach(() => {
        vi.clearAllMocks();
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

        it('shows value from value.q prop', () => {
            renderFilterBar({ value: { ...defaultValue, q: 'jihlava' } });
            expect(screen.getByDisplayValue('jihlava')).toBeInTheDocument();
        });

        it('calls onChange with updated q when user types 2+ chars', () => {
            vi.useFakeTimers();
            const onChange = vi.fn();
            renderFilterBar({ value: { ...defaultValue, q: '' }, onChange });
            const input = screen.getByPlaceholderText(labels.eventsFilter.searchPlaceholder);
            fireEvent.change(input, { target: { value: 'ab' } });
            act(() => { vi.advanceTimersByTime(300); });
            expect(onChange).toHaveBeenCalledWith({ ...defaultValue, q: 'ab' });
            vi.useRealTimers();
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
            renderFilterBar({ value: { ...defaultValue, timeWindow: 'budouci' } });
            const activeBtn = screen.getByRole('button', { name: labels.eventsFilter.budouci });
            expect(activeBtn).toHaveAttribute('aria-pressed', 'true');
        });

        it('highlights the active time window (Proběhlé)', () => {
            renderFilterBar({ value: { ...defaultValue, timeWindow: 'probehle' } });
            const activeBtn = screen.getByRole('button', { name: labels.eventsFilter.probehle });
            expect(activeBtn).toHaveAttribute('aria-pressed', 'true');
        });

        it('calls onChange with updated timeWindow when Proběhlé is clicked', async () => {
            const onChange = vi.fn();
            renderFilterBar({ onChange });
            await userEvent.click(screen.getByRole('button', { name: labels.eventsFilter.probehle }));
            expect(onChange).toHaveBeenCalledWith({ ...defaultValue, timeWindow: 'probehle' });
        });

        it('calls onChange with updated timeWindow when Vše is clicked', async () => {
            const onChange = vi.fn();
            renderFilterBar({ onChange });
            await userEvent.click(screen.getByRole('button', { name: labels.eventsFilter.vse }));
            expect(onChange).toHaveBeenCalledWith({ ...defaultValue, timeWindow: 'vse' });
        });
    });

    describe('"Moje přihlášky" checkbox', () => {
        it('shows checkbox when showRegisteredByMeToggle is true', () => {
            renderFilterBar({ showRegisteredByMeToggle: true });
            expect(
                screen.getByRole('checkbox', { name: labels.eventsFilter.mojePřihlaskyLabel }),
            ).toBeInTheDocument();
        });

        it('hides checkbox when showRegisteredByMeToggle is false', () => {
            renderFilterBar({ showRegisteredByMeToggle: false });
            expect(
                screen.queryByRole('checkbox', { name: labels.eventsFilter.mojePřihlaskyLabel }),
            ).not.toBeInTheDocument();
        });

        it('reflects checked state from value.registeredByMe', () => {
            renderFilterBar({ value: { ...defaultValue, registeredByMe: true } });
            const checkbox = screen.getByRole('checkbox', {
                name: labels.eventsFilter.mojePřihlaskyLabel,
            });
            expect(checkbox).toBeChecked();
        });

        it('calls onChange with updated registeredByMe when checkbox is toggled', async () => {
            const onChange = vi.fn();
            renderFilterBar({ onChange });
            const checkbox = screen.getByRole('checkbox', {
                name: labels.eventsFilter.mojePřihlaskyLabel,
            });
            await act(async () => {
                await userEvent.click(checkbox);
            });
            expect(onChange).toHaveBeenCalledWith({ ...defaultValue, registeredByMe: true });
        });
    });
});
