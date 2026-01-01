import {render, screen} from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import {KlabisTable} from './KlabisTable'
import {TableCell} from './TableCell'
import {vi} from 'vitest';

describe('KlabisTable - Pure UI Component', () => {
    const mockStaticData: { id: number; name: string; email: string }[] = [
        {id: 1, name: 'Alice', email: 'alice@example.com'},
        {id: 2, name: 'Bob', email: 'bob@example.com'},
    ]

    const mockPageData = {
        size: 10,
        totalElements: 2,
        totalPages: 1,
        number: 0,
    }

    describe('Data Rendering', () => {
        it('renders table with provided data', () => {
            render(
                <KlabisTable data={mockStaticData} page={mockPageData}>
                    <TableCell column="name">Name</TableCell>
                    <TableCell column="email">Email</TableCell>
                </KlabisTable>
            )

            expect(screen.getByText('Name')).toBeInTheDocument()
            expect(screen.getByText('Email')).toBeInTheDocument()
            expect(screen.getByText('Alice')).toBeInTheDocument()
            expect(screen.getByText('Bob')).toBeInTheDocument()
        })

        it('displays empty message when data is empty', () => {
            render(
                <KlabisTable data={[]} page={mockPageData}>
                    <TableCell column="name">Name</TableCell>
                </KlabisTable>
            )

            expect(screen.getByText('Žádná data')).toBeInTheDocument()
        })

        it('displays custom empty message', () => {
            render(
                <KlabisTable data={[]} page={mockPageData} emptyMessage="No results">
                    <TableCell column="name">Name</TableCell>
                </KlabisTable>
            )

            expect(screen.getByText('No results')).toBeInTheDocument()
        })

        it('renders pagination when page data is provided', () => {
            render(
                <KlabisTable data={mockStaticData} page={mockPageData}>
                    <TableCell column="name">Name</TableCell>
                </KlabisTable>
            )

            expect(screen.getByText(/1-2 z 2/)).toBeInTheDocument()
        })

        it('does not render pagination when page data is not provided', () => {
            render(
                <KlabisTable data={mockStaticData}>
                    <TableCell column="name">Name</TableCell>
                </KlabisTable>
            )

            expect(screen.queryByText(/\d+-\d+ z \d+/)).not.toBeInTheDocument()
        })
    })

    describe('Error States', () => {
        it('displays error message when error prop is provided', () => {
            const error = new Error('Network error')
            render(
                <KlabisTable data={[]} error={error}>
                    <TableCell column="name">Name</TableCell>
                </KlabisTable>
            )

            expect(screen.getByText(/network error/i)).toBeInTheDocument()
            expect(screen.getByText(/failed to load data/i)).toBeInTheDocument()
        })

        it('prioritizes error display over empty state', () => {
            const error = new Error('Network error')
            render(
                <KlabisTable data={[]} error={error} emptyMessage="No data">
                    <TableCell column="name">Name</TableCell>
                </KlabisTable>
            )

            expect(screen.getByText(/network error/i)).toBeInTheDocument()
            expect(screen.queryByText('No data')).not.toBeInTheDocument()
        })
    })

    describe('User Interactions - Row Clicks', () => {
        it('calls onRowClick when row is clicked', async () => {
            const onRowClick = vi.fn()
            const user = userEvent.setup()

            render(
                <KlabisTable data={mockStaticData} page={mockPageData} onRowClick={onRowClick}>
                    <TableCell column="name">Name</TableCell>
                </KlabisTable>
            )

            const firstRow = screen.getByText('Alice').closest('tr')
            if (firstRow) {
                await user.click(firstRow)
                expect(onRowClick).toHaveBeenCalledWith(mockStaticData[0])
            }
        })

        it('does not show row click styling when onRowClick is not provided', () => {
            const {container} = render(
                <KlabisTable data={mockStaticData} page={mockPageData}>
                    <TableCell column="name">Name</TableCell>
                </KlabisTable>
            )

            const rows = container.querySelectorAll('tbody tr')
            rows.forEach((row) => {
                expect(row.className).not.toMatch(/cursor-pointer/)
            })
        })
    })

    describe('User Interactions - Sorting', () => {
        it('calls onSortChange when sortable column header is clicked', async () => {
            const onSortChange = vi.fn()
            const user = userEvent.setup()

            render(
                <KlabisTable
                    data={mockStaticData}
                    page={mockPageData}
                    onSortChange={onSortChange}
                >
                    <TableCell column="name" sortable>
                        Name
                    </TableCell>
                </KlabisTable>
            )

            const nameHeader = screen.getByText('Name')
            await user.click(nameHeader)

            expect(onSortChange).toHaveBeenCalledWith('name', 'asc')
        })

        it('toggles sort direction on subsequent clicks', async () => {
            const onSortChange = vi.fn()
            const user = userEvent.setup()

            const {rerender} = render(
                <KlabisTable
                    data={mockStaticData}
                    page={mockPageData}
                    onSortChange={onSortChange}
                    currentSort={{by: 'name', direction: 'asc'}}
                >
                    <TableCell column="name" sortable>
                        Name
                    </TableCell>
                </KlabisTable>
            )

            const nameHeader = screen.getByText('Name')
            await user.click(nameHeader)

            expect(onSortChange).toHaveBeenCalledWith('name', 'desc')

            // Rerender with new sort state
            rerender(
                <KlabisTable
                    data={mockStaticData}
                    page={mockPageData}
                    onSortChange={onSortChange}
                    currentSort={{by: 'name', direction: 'desc'}}
                >
                    <TableCell column="name" sortable>
                        Name
                    </TableCell>
                </KlabisTable>
            )

            await user.click(nameHeader)

            expect(onSortChange).toHaveBeenCalledWith('name', 'asc')
        })

        it('displays sort indicator for current sort column', () => {
            render(
                <KlabisTable
                    data={mockStaticData}
                    page={mockPageData}
                    currentSort={{by: 'name', direction: 'asc'}}
                >
                    <TableCell column="name" sortable>
                        Name
                    </TableCell>
                </KlabisTable>
            )

            const header = screen.getByText('Name')
            expect(header.textContent).toMatch(/↑/)
        })

        it('displays descending indicator for desc sort', () => {
            render(
                <KlabisTable
                    data={mockStaticData}
                    page={mockPageData}
                    currentSort={{by: 'name', direction: 'desc'}}
                >
                    <TableCell column="name" sortable>
                        Name
                    </TableCell>
                </KlabisTable>
            )

            const header = screen.getByText('Name')
            expect(header.textContent).toMatch(/↓/)
        })

        it('does not make non-sortable columns clickable', async () => {
            const onSortChange = vi.fn()
            const user = userEvent.setup()

            render(
                <KlabisTable
                    data={mockStaticData}
                    page={mockPageData}
                    onSortChange={onSortChange}
                >
                    <TableCell column="name">Name</TableCell>
                </KlabisTable>
            )

            const nameHeader = screen.getByText('Name')
            await user.click(nameHeader)

            expect(onSortChange).not.toHaveBeenCalled()
        })
    })

    describe('User Interactions - Pagination', () => {
        it('calls onPageChange when page changes', async () => {
            const onPageChange = vi.fn()
            const multiPageData = {
                size: 10,
                totalElements: 50,
                totalPages: 5,
                number: 0,
            }
            const user = userEvent.setup()

            render(
                <KlabisTable
                    data={mockStaticData}
                    page={multiPageData}
                    onPageChange={onPageChange}
                    currentPage={0}
                >
                    <TableCell column="name">Name</TableCell>
                </KlabisTable>
            )

            const nextButton = screen.getByRole('button', {name: /Next/i})
            await user.click(nextButton)

            expect(onPageChange).toHaveBeenCalledWith(1)
        })

        it('calls onRowsPerPageChange when rows per page changes', async () => {
            const onRowsPerPageChange = vi.fn()
            const user = userEvent.setup()

            render(
                <KlabisTable
                    data={mockStaticData}
                    page={mockPageData}
                    onRowsPerPageChange={onRowsPerPageChange}
                    rowsPerPage={10}
                >
                    <TableCell column="name">Name</TableCell>
                </KlabisTable>
            )

            const select = screen.getByDisplayValue('10')
            await user.selectOptions(select, '25')

            expect(onRowsPerPageChange).toHaveBeenCalledWith(25)
        })
    })

    describe('Column Visibility', () => {
        it('hides hidden columns', () => {
            render(
                <KlabisTable data={mockStaticData} page={mockPageData}>
                    <TableCell column="name">Name</TableCell>
                    <TableCell column="email" hidden>
                        Email
                    </TableCell>
                </KlabisTable>
            )

            expect(screen.getByText('Name')).toBeInTheDocument()
            expect(screen.queryByText('Email')).not.toBeInTheDocument()
            expect(screen.queryByText('alice@example.com')).not.toBeInTheDocument()
        })
    })

    describe('Custom Cell Rendering', () => {
        it('uses custom dataRender for cells', () => {
            render(
                <KlabisTable data={mockStaticData} page={mockPageData}>
                    <TableCell column="name">Name</TableCell>
                    <TableCell
                        column="email"
                        dataRender={({value}) => <strong>{String(value)}</strong>}
                    >
                        Email
                    </TableCell>
                </KlabisTable>
            )

            const emailElements = screen.getAllByText(/example.com/)
            expect(emailElements.some((el) => el.tagName === 'STRONG')).toBe(true)
        })

        it('handles errors in custom dataRender gracefully', () => {
            const consoleError = vi.spyOn(console, 'error').mockImplementation(() => {
            })

            render(
                <KlabisTable data={mockStaticData} page={mockPageData}>
                    <TableCell column="name">Name</TableCell>
                    <TableCell
                        column="email"
                        dataRender={() => {
                            throw new Error('Render failed')
                        }}
                    >
                        Email
                    </TableCell>
                </KlabisTable>
            )

            // Should render error state instead of crashing
            const errorElements = screen.getAllByText('Error')
            expect(errorElements.length).toBeGreaterThanOrEqual(2) // one per row

            // Verify error was logged
            expect(consoleError).toHaveBeenCalledWith(
                expect.stringContaining('Error rendering cell for column "email"'),
                expect.any(Error)
            )

            // Verify table is still rendered (not crashed)
            expect(screen.getByText('Alice')).toBeInTheDocument()
            expect(screen.getByText('Bob')).toBeInTheDocument()

            consoleError.mockRestore()
        })
    })

    describe('Pagination Options', () => {
        it('adds current page size if not in options list', () => {
            render(
                <KlabisTable
                    data={Array.from({length: 20}, (_, i) => ({
                        id: i,
                        name: `Item ${i}`,
                        email: `item${i}@example.com`,
                    }))}
                    page={{
                        size: 20,
                        totalElements: 100,
                        totalPages: 5,
                        number: 0,
                    }}
                    rowsPerPageOptions={[5, 10, 25, 50]}
                    rowsPerPage={20}
                >
                    <TableCell column="name">Name</TableCell>
                </KlabisTable>
            )

            const select = screen.getByDisplayValue('20')
            const options = Array.from((select as HTMLSelectElement).options).map(
                (opt) => opt.value
            )
            expect(options).toEqual(['5', '10', '20', '25', '50'])
        })
    })
})
