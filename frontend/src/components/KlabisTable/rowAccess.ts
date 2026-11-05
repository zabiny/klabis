const asRecord = (row: object): Record<string, unknown> => row as Record<string, unknown>

export const cellValue = (row: object, column: string): unknown => asRecord(row)[column]

export const rowKey = (row: object, index: number, prefix: string): string => {
    const id = asRecord(row).id
    return id ? String(id) : `${prefix}-${index}`
}
