import type {Meta, StoryObj} from '@storybook/react-vite'
import {useState} from 'react'
import {Pagination} from './Pagination'

const meta = {
    title: 'Components/Table/Pagination',
    component: Pagination,
    tags: ['autodocs'],
    args: {
        count: 48,
        page: 0,
        rowsPerPage: 10,
        onPageChange: () => {},
        onRowsPerPageChange: () => {},
    },
} satisfies Meta<typeof Pagination>

export default meta
type Story = StoryObj<typeof meta>

export const Default: Story = {}

export const Controlled: Story = {
    render: () => {
        const [page, setPage] = useState(0)
        const [rowsPerPage, setRowsPerPage] = useState(10)
        return (
            <Pagination
                count={48}
                page={page}
                rowsPerPage={rowsPerPage}
                onPageChange={setPage}
                onRowsPerPageChange={(newSize) => {
                    setRowsPerPage(newSize)
                    setPage(0)
                }}
            />
        )
    },
}

export const FirstPage: Story = {
    args: {page: 0},
}

export const LastPage: Story = {
    args: {count: 48, rowsPerPage: 10, page: 4},
}

export const SinglePage: Story = {
    args: {count: 5, rowsPerPage: 10, page: 0},
}
