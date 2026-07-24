import type {ReactNode} from 'react'
import type {ColumnDef, TableCellRenderProps} from './types'

interface CardViewProps<T extends Record<string, unknown>> {
    data: T[]
    columns: ColumnDef[]
    onRowClick?: (item: T) => void
    emptyMessage: string
}

const defaultRenderCell = (props: TableCellRenderProps): ReactNode => (
    <span>{String(props.value ?? '')}</span>
)

export const CardView = <T extends Record<string, unknown>>({
    data,
    columns,
    onRowClick,
    emptyMessage,
}: CardViewProps<T>) => {
    // Filter out action columns (prefixed with _) on mobile
    const dataColumns = columns.filter(col => !col.name.startsWith('_'))

    if (data.length === 0) {
        return (
            <div className="px-4 py-8 text-center text-sm text-text-secondary">
                {emptyMessage}
            </div>
        )
    }

    return (
        <div className="flex flex-col gap-3 p-3">
            {data.map((item, rowIndex) => (
                <div
                    key={item.id ? String(item.id) : `card-${rowIndex}`}
                    className={`border border-border bg-surface-raised rounded-lg p-4 transition-colors ${
                        onRowClick ? 'cursor-pointer hover:bg-surface-base active:bg-surface-base' : ''
                    }`}
                    onClick={() => onRowClick?.(item)}
                    onKeyDown={(e) => {
                        if ((e.key === 'Enter' || e.key === ' ') && onRowClick) {
                            e.preventDefault()
                            onRowClick(item)
                        }
                    }}
                    tabIndex={onRowClick ? 0 : -1}
                    role={onRowClick ? 'button' : undefined}
                >
                    {dataColumns.map((col) => {
                        const value = item[col.name]
                        const renderFn = col.dataRender || defaultRenderCell
                        let cellContent: ReactNode
                        try {
                            cellContent = renderFn({item, column: col.name, value})
                        } catch {
                            cellContent = <span className="text-red-500 text-xs">Error</span>
                        }

                        return (
                            <div key={col.name} className="flex items-baseline justify-between py-1">
                                <span className="text-xs text-text-secondary shrink-0 mr-3">{col.label}</span>
                                <span className="text-sm text-text-primary text-right">{cellContent}</span>
                            </div>
                        )
                    })}
                </div>
            ))}
        </div>
    )
}
