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
        it('renders three status pill buttons when hasManageAuthority is true', () => {
            renderFilterBar({ hasManageAuthority: true });
            expect(
                screen.getByRole('button', { name: labels.membersFilter.statusActive }),
            ).toBeInTheDocument();
            expect(
                screen.getByRole('button', { name: labels.membersFilter.statusInactive }),
            ).toBeInTheDocument();
            expect(
                screen.getByRole('button', { name: labels.membersFilter.statusAll }),
            ).toBeInTheDocument();
        });

        it('hides the pill group entirely when hasManageAuthority is false', () => {
            renderFilterBar({ hasManageAuthority: false });
            expect(
                screen.queryByRole('button', { name: labels.membersFilter.statusActive }),
            ).not.toBeInTheDocument();
            expect(
                screen.queryByRole('button', { name: labels.membersFilter.statusInactive }),
            ).not.toBeInTheDocument();
            expect(
                screen.queryByRole('button', { name: labels.membersFilter.statusAll }),
            ).not.toBeInTheDocument();
        });

        it('Aktivní pill is active (aria-pressed=true) when value.status is ACTIVE', () => {
            renderFilterBar({ value: { ...defaultValue, status: 'ACTIVE' } });
            expect(
                screen.getByRole('button', { name: labels.membersFilter.statusActive }),
            ).toHaveAttribute('aria-pressed', 'true');
        });

        it('Neaktivní pill is active (aria-pressed=true) when value.status is INACTIVE', () => {
            renderFilterBar({ value: { ...defaultValue, status: 'INACTIVE' } });
            expect(
                screen.getByRole('button', { name: labels.membersFilter.statusInactive }),
            ).toHaveAttribute('aria-pressed', 'true');
        });

        it('Neaktivní pill is not active when value.status is ACTIVE', () => {
            renderFilterBar({ value: { ...defaultValue, status: 'ACTIVE' } });
            expect(
                screen.getByRole('button', { name: labels.membersFilter.statusInactive }),
            ).toHaveAttribute('aria-pressed', 'false');
        });

        it('calls onChange with updated status when Neaktivní is clicked', async () => {
            const onChange = vi.fn();
            const user = userEvent.setup();
            renderFilterBar({ onChange });
            await user.click(screen.getByRole('button', { name: labels.membersFilter.statusInactive }));
            expect(onChange).toHaveBeenCalledWith({ ...defaultValue, status: 'INACTIVE' });
        });

        it('calls onChange with updated status when Vše is clicked', async () => {
            const onChange = vi.fn();
            const user = userEvent.setup();
            renderFilterBar({ onChange });
            await user.click(screen.getByRole('button', { name: labels.membersFilter.statusAll }));
            expect(onChange).toHaveBeenCalledWith({ ...defaultValue, status: 'ALL' });
        });
    });
});
