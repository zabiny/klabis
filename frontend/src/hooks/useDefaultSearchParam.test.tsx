import '@testing-library/jest-dom';
import { render, screen } from '@testing-library/react';
import { MemoryRouter, useLocation } from 'react-router-dom';
import { describe, it, expect } from 'vitest';
import { useDefaultSearchParam } from './useDefaultSearchParam';

const SearchReader = ({ paramName }: { paramName: string }) => {
    const location = useLocation();
    const params = new URLSearchParams(location.search);
    return <span data-testid="param-value">{params.get(paramName) ?? '(none)'}</span>;
};

const TestComponent = ({
    paramName,
    defaultValue,
    shouldApply,
}: {
    paramName: string;
    defaultValue: string;
    shouldApply?: () => boolean;
}) => {
    useDefaultSearchParam(paramName, defaultValue, shouldApply);
    return <SearchReader paramName={paramName} />;
};

const renderHook = (
    initialUrl: string,
    paramName: string,
    defaultValue: string,
    shouldApply?: () => boolean,
) =>
    render(
        <MemoryRouter initialEntries={[initialUrl]}>
            <TestComponent paramName={paramName} defaultValue={defaultValue} shouldApply={shouldApply} />
        </MemoryRouter>,
    );

describe('useDefaultSearchParam', () => {
    it('injects default when param is absent from URL', async () => {
        renderHook('/', 'status', 'ACTIVE');
        expect(await screen.findByTestId('param-value')).toHaveTextContent('ACTIVE');
    });

    it('does NOT overwrite an existing param', () => {
        renderHook('/?status=INACTIVE', 'status', 'ACTIVE');
        expect(screen.getByTestId('param-value')).toHaveTextContent('INACTIVE');
    });

    it('does NOT inject when shouldApply returns false', () => {
        renderHook('/', 'status', 'ACTIVE', () => false);
        expect(screen.getByTestId('param-value')).toHaveTextContent('(none)');
    });

    it('injects when shouldApply returns true and param is absent', async () => {
        renderHook('/', 'status', 'ACTIVE', () => true);
        expect(await screen.findByTestId('param-value')).toHaveTextContent('ACTIVE');
    });
});
