import '@testing-library/jest-dom';
import {render, screen} from '@testing-library/react';
import {MemoryRouter} from 'react-router-dom';
import {FeeGroupsTable} from './FeeGroupsTable';

const mockGroups = [
    {
        id: 'group-1',
        name: 'Základní členství',
        memberCount: 10,
        status: 'EDITABLE' as const,
        _links: {self: {href: '/api/membership-fee-groups/group-1'}},
    },
    {
        id: 'group-2',
        name: 'Aktivní závodník',
        memberCount: 5,
        status: 'FROZEN' as const,
        _links: {self: {href: '/api/membership-fee-groups/group-2'}},
    },
];

const renderTable = (groups = mockGroups) =>
    render(
        <MemoryRouter>
            <FeeGroupsTable groups={groups}/>
        </MemoryRouter>
    );

describe('FeeGroupsTable', () => {
    describe('empty state', () => {
        it('renders empty state message when no groups are provided', () => {
            renderTable([]);
            expect(screen.getByText(/žádné skupiny/i)).toBeInTheDocument();
        });

        it('does not render table when no groups are provided', () => {
            renderTable([]);
            expect(screen.queryByRole('table')).not.toBeInTheDocument();
        });
    });

    describe('group rows', () => {
        it('renders group name', () => {
            renderTable();
            expect(screen.getByText('Základní členství')).toBeInTheDocument();
            expect(screen.getByText('Aktivní závodník')).toBeInTheDocument();
        });

        it('renders member count', () => {
            renderTable();
            expect(screen.getByText('10')).toBeInTheDocument();
            expect(screen.getByText('5')).toBeInTheDocument();
        });

        it('renders EDITABLE status badge as "Editovatelná"', () => {
            renderTable();
            expect(screen.getByText('Editovatelná')).toBeInTheDocument();
        });

        it('renders FROZEN status badge as "Zmrazená"', () => {
            renderTable();
            expect(screen.getByText('Zmrazená')).toBeInTheDocument();
        });
    });

    describe('navigation', () => {
        it('renders name link and arrow link to group detail for each row', () => {
            renderTable();
            const links = screen.getAllByRole('link');
            const groupLinks = links.filter(l => l.getAttribute('href')?.includes('membership-fee-groups'));
            expect(groupLinks).toHaveLength(mockGroups.length * 2);
        });

        it('does not navigate when row background is clicked', async () => {
            renderTable();
            const row = screen.getByText('Základní členství').closest('tr')!;
            expect(row).not.toHaveAttribute('onClick');
        });
    });
});
