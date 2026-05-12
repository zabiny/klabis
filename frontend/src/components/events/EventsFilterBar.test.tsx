import '@testing-library/jest-dom';
import { render, screen, act, fireEvent } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { vi, describe, it, expect, beforeEach } from 'vitest';
import { EventsFilterBar, type EventsFilterBarProps, type EventsFilterValue } from './EventsFilterBar';
import { labels } from '../../localization';
import type { EventTypeCatalogItem } from '../../hooks/useEventTypes';

const mockEventTypes: EventTypeCatalogItem[] = [
    { id: 'et-1', name: 'Klub', sortOrder: 1 },
    { id: 'et-2', name: 'Oblastní', sortOrder: 2, color: '#ff0000' },
];

const defaultValue: EventsFilterValue = {
    q: '',
    timeWindow: 'budouci',
    registeredByMe: false,
    eventTypeIds: [],
};

const defaultProps: EventsFilterBarProps = {
    value: defaultValue,
    onChange: vi.fn(),
    showRegisteredByMeToggle: true,
    eventTypes: [],
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

    describe('"Typ akce" multi-select', () => {
        it('renders event type filter with label', () => {
            renderFilterBar({ eventTypes: mockEventTypes });
            expect(
                screen.getByLabelText(labels.eventsFilter.eventTypeFilter),
            ).toBeInTheDocument();
        });

        it('renders options for each event type plus placeholder', () => {
            renderFilterBar({ eventTypes: mockEventTypes });
            const select = screen.getByLabelText(labels.eventsFilter.eventTypeFilter);
            const options = Array.from((select as HTMLSelectElement).options).map(o => o.text);
            expect(options).toContain(labels.eventsFilter.eventTypeSelectPlaceholder);
            expect(options).toContain('Klub');
            expect(options).toContain('Oblastní');
        });

        it('does not render event type filter when eventTypes is empty', () => {
            renderFilterBar({ eventTypes: [] });
            expect(
                screen.queryByLabelText(labels.eventsFilter.eventTypeFilter),
            ).not.toBeInTheDocument();
        });

        it('does not render event type filter when eventTypes is undefined', () => {
            renderFilterBar({ eventTypes: undefined });
            expect(
                screen.queryByLabelText(labels.eventsFilter.eventTypeFilter),
            ).not.toBeInTheDocument();
        });

        it('reflects selected eventTypeIds in the select value', () => {
            renderFilterBar({
                eventTypes: mockEventTypes,
                value: { ...defaultValue, eventTypeIds: ['et-1'] },
            });
            const select = screen.getByLabelText(labels.eventsFilter.eventTypeFilter) as HTMLSelectElement;
            const selectedValues = Array.from(select.selectedOptions).map(o => o.value);
            expect(selectedValues).toContain('et-1');
        });

        it('calls onChange with updated eventTypeIds when an option is selected', async () => {
            const onChange = vi.fn();
            renderFilterBar({ eventTypes: mockEventTypes, onChange });
            const select = screen.getByLabelText(labels.eventsFilter.eventTypeFilter) as HTMLSelectElement;
            await act(async () => {
                Array.from(select.options).forEach((opt) => {
                    opt.selected = opt.value === 'et-1';
                });
                fireEvent.change(select);
            });
            expect(onChange).toHaveBeenCalledWith(
                expect.objectContaining({ eventTypeIds: ['et-1'] }),
            );
        });
    });
});
