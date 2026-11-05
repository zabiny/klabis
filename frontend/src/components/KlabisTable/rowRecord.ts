export const rowRecord = (row: object): Record<string, unknown> => row as Record<string, unknown>

export const rowKey = (record: Record<string, unknown>, index: number, prefix: string): string =>
    record.id ? String(record.id) : `${prefix}-${index}`
