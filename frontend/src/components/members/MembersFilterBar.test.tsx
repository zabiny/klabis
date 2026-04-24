import '@testing-library/jest-dom';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { vi, describe, it, expect, beforeEach } from 'vitest';
import { MembersFilterBar } from './MembersFilterBar';
import { labels } from '../../localization';

const renderFilterBar = (
    hasManageAuthority: boolean,
    initialUrl = '/',
    searchQuery = '',
    onSearchChange = vi.fn(),
) =>
    render(
        <MemoryRouter initialEntries={[initialUrl]}>
            <MembersFilterBar
                hasManageAuthority={hasManageAuthority}
                searchQuery={searchQuery}
                onSearchChange={onSearchChange}
            />
        </MemoryRouter>,
    );

describe('MembersFilterBar', () => {
    beforeEach(() => {
        vi.clearAllMocks();
    });

    describe('search input', () => {
        it('renders search input with correct placeholder', () => {
            renderFilterBar(true);
            expect(
                screen.getByPlaceholderText(labels.membersFilter.searchPlaceholder),
            ).toBeInTheDocument();
        });

        it('shows value from searchQuery prop', () => {
            renderFilterBar(true, '/', 'jan');
            expect(screen.getByDisplayValue('jan')).toBeInTheDocument();
        });
    });

    describe('status pill group', () => {
        it('renders three status pill buttons when hasManageAuthority is true', () => {
            renderFilterBar(true);
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
            renderFilterBar(false);
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

        it('Aktivní pill is active (aria-pressed=true) when URL has no status param (default)', () => {
            renderFilterBar(true, '/');
            expect(
                screen.getByRole('button', { name: labels.membersFilter.statusActive }),
            ).toHaveAttribute('aria-pressed', 'true');
        });

        it('Aktivní pill is active (aria-pressed=true) when URL has ?status=ACTIVE', () => {
            renderFilterBar(true, '/?status=ACTIVE');
            expect(
                screen.getByRole('button', { name: labels.membersFilter.statusActive }),
            ).toHaveAttribute('aria-pressed', 'true');
        });

        it('Neaktivní pill is active (aria-pressed=true) when URL has ?status=INACTIVE', () => {
            renderFilterBar(true, '/?status=INACTIVE');
            expect(
                screen.getByRole('button', { name: labels.membersFilter.statusInactive }),
            ).toHaveAttribute('aria-pressed', 'true');
        });

        it('Neaktivní pill is not active when URL has ?status=ACTIVE', () => {
            renderFilterBar(true, '/?status=ACTIVE');
            expect(
                screen.getByRole('button', { name: labels.membersFilter.statusInactive }),
            ).toHaveAttribute('aria-pressed', 'false');
        });

        it('clicking Neaktivní writes ?status=INACTIVE to the URL', async () => {
            const user = userEvent.setup();
            renderFilterBar(true, '/');
            await user.click(screen.getByRole('button', { name: labels.membersFilter.statusInactive }));
            expect(
                screen.getByRole('button', { name: labels.membersFilter.statusInactive }),
            ).toHaveAttribute('aria-pressed', 'true');
        });

        it('clicking Vše writes ?status=ALL to the URL', async () => {
            const user = userEvent.setup();
            renderFilterBar(true, '/');
            await user.click(screen.getByRole('button', { name: labels.membersFilter.statusAll }));
            expect(
                screen.getByRole('button', { name: labels.membersFilter.statusAll }),
            ).toHaveAttribute('aria-pressed', 'true');
        });
    });
});
