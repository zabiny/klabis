import type {ReactNode} from 'react'
import {KlabisTableProvider, useKlabisTableContext} from './KlabisTableContext'
import type {KlabisTableProps} from './types'
import {TableContainer} from './TableContainer'
import {TableHead} from './TableHead'
import {TableBody} from './TableBody'
import {TableRow} from './TableRow'
import {TableDataCell} from './TableDataCell'
import {Pagination} from './Pagination'

const KlabisTablePagination = (): ReactNode => {
  const {paging, handlePagingChange: onPagingChange} = useKlabisTableContext()

  if (!paging) return null

  return (
      <Pagination
          count={paging.totalElements}
          page={paging.number}
          rowsPerPage={paging.size}
          onPageChange={(pageNumber: number) => onPagingChange(pageNumber, paging.size)}
          onRowsPerPageChange={(rowsPerPage: number) => onPagingChange(paging.number, rowsPerPage)}
          rowsPerPageOptions={[5, 10, 25, 50]}
          labelRowsPerPage="Řádků na stránku:"
          labelDisplayedRows={({from, to, count}) => `${from}-${to} z ${count}`}
      />
  )
}

const KlabisTableBody = <T extends Record<string, unknown>>({onRowClick, emptyMessage = 'Žádná data'}: {
  onRowClick?: (item: T) => void
  emptyMessage?: string
}): ReactNode => {
  const {tableModel, rows} = useKlabisTableContext<T>()

  const handleRowKeyDown = (event: React.KeyboardEvent, item: T) => {
    if ((event.key === 'Enter' || event.key === ' ') && onRowClick) {
      event.preventDefault()
      onRowClick(item)
    }
  }

  const renderRows = (rows?: T[]): ReactNode => {
    if (!rows || rows.length === 0) {
      return (
          <TableRow key={0}>
            <TableDataCell align="center" colSpan={tableModel.columns.length}>
              {emptyMessage}
            </TableDataCell>
          </TableRow>
      )
    }

    return rows.map((item: T, index: number) => (
        <TableRow
            key={item?.id ? String(item.id) : `row-${index}`}
            hover
            onClick={() => onRowClick && onRowClick(item)}
            onKeyDown={(e) => handleRowKeyDown(e, item)}
            tabIndex={onRowClick ? 0 : -1}
            role={onRowClick ? 'button' : undefined}
            aria-label={`Řádek ${index + 1}`}
        >
          {tableModel.renderCellsForRow(item)}
        </TableRow>
    ))
  }

  return <TableBody>{renderRows(rows)}</TableBody>
}

const KlabisTableHeaders = (): ReactNode => {
  const {tableModel} = useKlabisTableContext()

  return (
      <TableHead>
        <TableRow>{tableModel.renderHeaders()}</TableRow>
      </TableHead>
  )
}

export const KlabisTable = <T extends Record<string, unknown>>(props: KlabisTableProps<T>) => {
  const {
    children,
    defaultOrderBy,
    defaultOrderDirection = 'asc',
    defaultRowsPerPage = 10,
    fetchData,
    onRowClick,
    emptyMessage
  } = props

  return (
      <KlabisTableProvider
          colDefs={children}
          fetchData={fetchData}
          defaultSort={defaultOrderBy !== undefined ? {
            by: defaultOrderBy,
            direction: defaultOrderDirection
          } : undefined}
          defaultRowsPerPage={defaultRowsPerPage}
    >
          <div className="shadow-md rounded-md overflow-hidden border border-border bg-surface-raised">
          <TableContainer>
            <table className="w-full" aria-label="Tabulka dat">
              <KlabisTableHeaders/>
              <KlabisTableBody onRowClick={onRowClick} emptyMessage={emptyMessage}/>
            </table>
          </TableContainer>
          <KlabisTablePagination/>
        </div>
    </KlabisTableProvider>
  )
}