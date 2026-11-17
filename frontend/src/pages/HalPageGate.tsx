import type {ReactNode} from 'react';
import type {HalResponse} from '../api';
import {Skeleton} from '../components/UI';
import {useHalPageData} from '../hooks/useHalPageData';
import {ErrorPage} from './ErrorPage';

interface HalPageGateProps<T> {
    loading?: ReactNode;
    children: (resource: T) => ReactNode;
}

export function HalPageGate<T = HalResponse>({loading, children}: HalPageGateProps<T>): ReactNode {
    const {resourceData, isLoading, error, queryState} = useHalPageData<T>();

    if (queryState === 'idle') {
        return null;
    }

    if (isLoading) {
        return loading ?? <Skeleton/>;
    }

    if (error) {
        return <ErrorPage error={error}/>;
    }

    if (!resourceData) {
        return loading ?? <Skeleton/>;
    }

    return children(resourceData);
}
