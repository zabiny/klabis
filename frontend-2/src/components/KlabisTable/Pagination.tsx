import type {ReactNode} from 'react'
import {Button} from '../UI'

interface PaginationProps {
    count: number
    page: number
    rowsPerPage: number
    onPageChange: (page: number) => void
    onRowsPerPageChange: (rowsPerPage: number) => void
    rowsPerPageOptions?: number[]
    labelRowsPerPage?: string
    labelDisplayedRows?: (paginationInfo: { from: number; to: number; count: number }) => ReactNode
    className?: string
}

/**
 * Pagination - Custom pagination control
 * Replaces MUI TablePagination with Tailwind styling
 */
export const Pagination = ({
                               count,
                               page,
                               rowsPerPage,
                               onPageChange,
                               onRowsPerPageChange,
                               rowsPerPageOptions = [5, 10, 25, 50],
                               labelRowsPerPage = 'Rows per page:',
                               labelDisplayedRows,
                               className = ''
                           }: PaginationProps) => {
    const from = page * rowsPerPage + 1
    const to = Math.min((page + 1) * rowsPerPage, count)
    const totalPages = Math.ceil(count / rowsPerPage)
    const canPreviousPage = page > 0
    const canNextPage = page < totalPages - 1

    const defaultDisplayedRows = `${from}-${to} of ${count}`
    const displayedRows = labelDisplayedRows ? labelDisplayedRows({from, to, count}) : defaultDisplayedRows

    const handleRowsPerPageChange = (e: React.ChangeEvent<HTMLSelectElement>) => {
        onRowsPerPageChange(parseInt(e.target.value, 10))
    }

    return (
        <div
            className={`flex items-center justify-between gap-4 px-6 py-4 border-t border-border bg-surface-base ${className}`}>
            {/* Rows per page selector */}
            <div className="flex items-center gap-2">
                <label htmlFor="rows-per-page" className="text-sm text-text-secondary">
                    {labelRowsPerPage}
                </label>
                <select
                    id="rows-per-page"
                    value={rowsPerPage}
                    onChange={handleRowsPerPageChange}
                    className="form-input py-1 px-2 text-sm h-9 bg-surface-base border border-border rounded-md text-text-primary"
                >
                    {rowsPerPageOptions.map((option) => (
                        <option key={option} value={option}>
                            {option}
                        </option>
                    ))}
                </select>
            </div>

            {/* Displayed rows info */}
            <div className="text-sm text-text-secondary">
                {displayedRows}
            </div>

            {/* Navigation buttons */}
            <div className="flex items-center gap-2">
                <Button
                    variant="ghost"
                    size="sm"
                    onClick={() => onPageChange(page - 1)}
                    disabled={!canPreviousPage}
                    className="disabled:opacity-50 disabled:cursor-not-allowed"
                >
                    ← Previous
                </Button>
                <span className="text-sm text-text-secondary px-2">
          Page {page + 1} of {totalPages}
        </span>
                <Button
                    variant="ghost"
                    size="sm"
                    onClick={() => onPageChange(page + 1)}
                    disabled={!canNextPage}
                    className="disabled:opacity-50 disabled:cursor-not-allowed"
                >
                    Next →
                </Button>
            </div>
        </div>
    )
}

Pagination.displayName = 'Pagination'
