import React, {createContext, ReactNode, useContext, useState} from 'react';
import {type ApiParams, type SortDirection} from './types';

interface KlabisTableContextType {
    // Pagination state
    page: number;
    rowsPerPage: number;
    setPage: (page: number) => void;
    setRowsPerPage: (rowsPerPage: number) => void;

    // Sorting state
    orderBy?: string;
    orderDirection: SortDirection;
    setOrderBy: (orderBy: string, direction: SortDirection) => void;

    // Combined API params
    createApiParams: () => ApiParams;

    // Actions
    handleRequestSort: (column: string) => void;
    handleChangePage: (event: unknown, newPage: number) => void;
    handleChangeRowsPerPage: (event: React.ChangeEvent<HTMLInputElement>) => void;
}

const KlabisTableContext = createContext<KlabisTableContextType | undefined>(undefined);

interface KlabisTableProviderProps {
    children: ReactNode;
    defaultOrderBy?: string;
    defaultOrderDirection?: SortDirection;
    defaultRowsPerPage?: number;
    additionalParams?: Record<string, any>;
}

export const KlabisTableProvider: React.FC<KlabisTableProviderProps> = ({
                                                                            children,
                                                                            defaultOrderBy,
                                                                            defaultOrderDirection = 'asc',
                                                                            defaultRowsPerPage = 10,
                                                                            additionalParams = {},
                                                                        }) => {
    const [page, setPage] = useState(0);
    const [rowsPerPage, setRowsPerPage] = useState(defaultRowsPerPage);
    const [orderBy, setOrderBy] = useState<string | undefined>(defaultOrderBy);
    const [orderDirection, setOrderDirection] = useState<SortDirection>(defaultOrderDirection);

    const handleRequestSort = (column: string) => {
        const isAsc = orderBy === column && orderDirection === 'asc';
        setOrderDirection(isAsc ? 'desc' : 'asc');
        setOrderBy(column);
        setPage(0); // Reset to first page when sorting changes
    };

    const handleChangePage = (event: unknown, newPage: number) => {
        setPage(newPage);
    };

    const handleChangeRowsPerPage = (event: React.ChangeEvent<HTMLInputElement>) => {
        setRowsPerPage(parseInt(event.target.value, 10));
        setPage(0);
    };

    // Vytvoření parametrů pro API volání
    function createApiParams(): ApiParams {
        return {
            page: page,
            size: rowsPerPage,
            sort: orderBy ? [`${orderBy},${orderDirection}`] : [],
            ...additionalParams
        } as ApiParams;
    }

    const contextValue: KlabisTableContextType = {
        page,
        rowsPerPage,
        setPage,
        setRowsPerPage,
        orderBy,
        orderDirection,
        setOrderBy,
        createApiParams,
        handleRequestSort,
        handleChangePage,
        handleChangeRowsPerPage,
    };

    return (
        <KlabisTableContext.Provider value={contextValue}>
            {children}
        </KlabisTableContext.Provider>
    );
};

export const useKlabisTableContext = (): KlabisTableContextType => {
    const context = useContext(KlabisTableContext);
    if (context === undefined) {
        throw new Error('useKlabisTableContext must be used within a KlabisTableProvider');
    }
    return context;
};
