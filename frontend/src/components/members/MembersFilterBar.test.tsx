import '@testing-library/jest-dom';
import { render, screen, act, fireEvent } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { vi, describe, it, expect, beforeEach } from 'vitest';
import { MembersFilterBar, type MembersFilterValue } from './MembersFilterBar';
import { labels } from '../../localization';
import type { MembersFilterBarProps } from './MembersFilterBar';

const defaultValue: MembersFilterValue = {
    q: '',
    status: 'ACTIVE',
    incomplete: false,
};

const defaultProps: MembersFilterBarProps = {
    value: defaultValue,
    onChange: vi.fn(),
    hasManageAuthority: true,
};

const renderFilterBar = (props: Partial<MembersFilterBarProps> = {}) =>
    render(
        <MemoryRouter initialEntries={['/']}>
            <MembersFilterBar {...defaultProps} {...props} />
        </MemoryRouter>,
    );

describe('MembersFilterBar', () => {
    beforeEach(() => {
        vi.clearAllMocks();
    });

    describe('search input', () => {
        it('renders search input with correct placeholder', () => {
            renderFilterBar();
            expect(
                screen.getByPlaceholderText(labels.membersFilter.searchPlaceholder),
            ).toBeInTheDocument();
        });

        it('shows value from value.q prop', () => {
            renderFilterBar({ value: { ...defaultValue, q: 'jan' } });
            expect(screen.getByDisplayValue('jan')).toBeInTheDocument();
        });

        it('calls onChange with updated q when user types 2+ chars', () => {
            vi.useFakeTimers();
            const onChange = vi.fn();
            renderFilterBar({ value: { ...defaultValue, q: '' }, onChange });
            const input = screen.getByPlaceholderText(labels.membersFilter.searchPlaceholder);
            fireEvent.change(input, { target: { value: 'ab' } });
            act(() => { vi.advanceTimersByTime(300); });
            expect(onChange).toHaveBeenCalledWith({ ...defaultValue, q: 'ab' });
            vi.useRealTimers();
        });
    });

    describe('status pill group', () => {
        const pills = [
            {label: labels.membersFilter.statusActive, status: 'ACTIVE'},
            {label: labels.membersFilter.statusInactive, status: 'INACTIVE'},
            {label: labels.membersFilter.statusAll, status: 'ALL'},
        ] as const;

        it.each(pills)('shows the "$label" pill iff hasManageAuthority', ({label}) => {
            const {unmount} = renderFilterBar({ hasManageAuthority: true });
            expect(screen.getByRole('button', { name: label })).toBeInTheDocument();
            unmount();

            renderFilterBar({ hasManageAuthority: false });
            expect(screen.queryByRole('button', { name: label })).not.toBeInTheDocument();
        });

        it.each(pills)('only the pill for value.status $status is aria-pressed', ({status}) => {
            renderFilterBar({ value: { ...defaultValue, status } });
            pills.forEach(pill => {
                expect(screen.getByRole('button', { name: pill.label }))
                    .toHaveAttribute('aria-pressed', String(pill.status === status));
            });
        });

        it.each(pills.filter(pill => pill.status !== 'ACTIVE'))(
            'calls onChange with status $status when "$label" is clicked',
            async ({label, status}) => {
                const onChange = vi.fn();
                const user = userEvent.setup();
                renderFilterBar({ onChange });
                await user.click(screen.getByRole('button', { name: label }));
                expect(onChange).toHaveBeenCalledWith({ ...defaultValue, status });
            },
        );
    });

    describe('incomplete-only toggle', () => {
        it.each([[true], [false]])('toggle is shown iff hasManageAuthority (%s)', (hasManageAuthority) => {
            renderFilterBar({ hasManageAuthority });
            expect(screen.queryByRole('checkbox', { name: labels.membersFilter.incompleteOnly }) !== null)
                .toBe(hasManageAuthority);
        });

        it('is checked when value.incomplete is true', () => {
            renderFilterBar({ value: { ...defaultValue, incomplete: true } });
            expect(
                screen.getByRole('checkbox', { name: labels.membersFilter.incompleteOnly }),
            ).toBeChecked();
        });

        it('calls onChange with incomplete=true when checked', async () => {
            const onChange = vi.fn();
            const user = userEvent.setup();
            renderFilterBar({ onChange });
            await user.click(screen.getByRole('checkbox', { name: labels.membersFilter.incompleteOnly }));
            expect(onChange).toHaveBeenCalledWith({ ...defaultValue, incomplete: true });
        });
    });
});
