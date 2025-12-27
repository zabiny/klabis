import {render, screen, waitFor} from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import {KlabisTable} from './KlabisTable'
import {TableCell} from './TableCell'
import type {FetchTableDataCallback} from './types'

describe('KlabisTable', () => {
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

    describe('Static Data Mode (data + page props)', () => {
        it('renders table with static data', () => {
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

        it('calls onRowClick when row is clicked', async () => {
            const onRowClick = jest.fn()
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

        it('calls onStateChange when sort changes', async () => {
            const onStateChange = jest.fn()
            const user = userEvent.setup()

            render(
                <KlabisTable
                    data={mockStaticData}
                    page={mockPageData}
                    onStateChange={onStateChange}
                >
                    <TableCell column="name" sortable>
                        Name
                    </TableCell>
                </KlabisTable>
            )

            const nameHeader = screen.getByText('Name')
            await user.click(nameHeader)

            await waitFor(() => {
                expect(onStateChange).toHaveBeenCalledWith(
                    expect.objectContaining({
                        sort: {by: 'name', direction: 'asc'},
                    })
                )
            })
        })

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

    describe('Auto-Fetch Mode (fetchData prop)', () => {
        it('fetches data when component mounts', async () => {
            const mockFetchData: FetchTableDataCallback<typeof mockStaticData[0]> = jest.fn(
                async () => ({
                    data: mockStaticData,
                    page: mockPageData,
                })
            )

            render(
                <KlabisTable fetchData={mockFetchData}>
                    <TableCell column="name">Name</TableCell>
                </KlabisTable>
            )

            await waitFor(() => {
                expect(mockFetchData).toHaveBeenCalled()
                expect(screen.getByText('Alice')).toBeInTheDocument()
            })
        })

        it('calls fetchData with correct API params', async () => {
            const mockFetchData: FetchTableDataCallback<typeof mockStaticData[0]> = jest.fn(
                async () => ({
                    data: mockStaticData,
                    page: mockPageData,
                })
            )

            render(
                <KlabisTable fetchData={mockFetchData} defaultRowsPerPage={20}>
                    <TableCell column="name">Name</TableCell>
                </KlabisTable>
            )

            await waitFor(() => {
                expect(mockFetchData).toHaveBeenCalledWith(
                    expect.objectContaining({
                        page: 0,
                        size: 20,
                        sort: [],
                    })
                )
            })
        })

        it('refetches data when pagination changes', async () => {
            const multiPageData = {
                size: 10,
                totalElements: 50,
                totalPages: 5,
                number: 0,
            }

            const mockFetchData: FetchTableDataCallback<typeof mockStaticData[0]> = jest.fn(
                async (params) => ({
                    data: mockStaticData,
                    page: {...multiPageData, number: params.page},
                })
            )

            const user = userEvent.setup()

            render(
                <KlabisTable fetchData={mockFetchData} page={multiPageData}>
                    <TableCell column="name">Name</TableCell>
                </KlabisTable>
            )

            await waitFor(() => {
                expect(mockFetchData).toHaveBeenCalled()
            })

            // Click next page
            const nextButton = screen.getByRole('button', {name: /Next/i})
            await user.click(nextButton)

            await waitFor(() => {
                expect(mockFetchData).toHaveBeenCalledTimes(2)
            })
        })

        it('handles fetch errors gracefully', async () => {
            const mockFetchData: FetchTableDataCallback<typeof mockStaticData[0]> = jest.fn(
                async () => {
                    throw new Error('Fetch failed')
                }
            )

            const consoleError = jest.spyOn(console, 'error').mockImplementation()

            render(
                <KlabisTable fetchData={mockFetchData}>
                    <TableCell column="name">Name</TableCell>
                </KlabisTable>
            )

            await waitFor(() => {
                expect(screen.getByText('Žádná data')).toBeInTheDocument()
            })

            consoleError.mockRestore()
        })
    })

    describe('Pagination Options', () => {
        it('uses default rows per page options', () => {
            const largePage = {
                size: 10,
                totalElements: 100,
                totalPages: 10,
                number: 0,
            }

            render(
                <KlabisTable
                    data={Array.from({length: 10}, (_, i) => ({
                        id: i,
                        name: `Item ${i}`,
                        email: `item${i}@example.com`,
                    }))}
                    page={largePage}
                >
                    <TableCell column="name">Name</TableCell>
                </KlabisTable>
            )

            const select = screen.getByDisplayValue('10')
            const options = Array.from((select as HTMLSelectElement).options).map(
                (opt) => opt.value
            )
            expect(options).toEqual(['5', '10', '25', '50'])
        })

        it('adds current page size if not in options list', () => {
            const largePage = {
                size: 20,
                totalElements: 100,
                totalPages: 5,
                number: 0,
            }

            render(
                <KlabisTable
                    data={Array.from({length: 20}, (_, i) => ({
                        id: i,
                        name: `Item ${i}`,
                        email: `item${i}@example.com`,
                    }))}
                    page={largePage}
                    rowsPerPageOptions={[5, 10, 25, 50]}
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
    })
})
