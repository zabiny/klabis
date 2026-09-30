import '@testing-library/jest-dom';
import React from 'react';
import {render, screen} from '@testing-library/react';
import {vi} from 'vitest';
import {useHalPageData} from '../../hooks/useHalPageData';
import {LegalGuardianGroupsPage} from './LegalGuardianGroupsPage';

vi.mock('../../hooks/useHalPageData', () => ({useHalPageData: vi.fn()}));

vi.mock('../../components/HalNavigator2/HalEmbeddedTable.tsx', () => ({
    HalEmbeddedTable: ({collectionName, children}: {collectionName: string; children: React.ReactNode}) => (
        <div data-testid={`table-${collectionName}`}>
            {React.Children.map(children, (child) => {
                const {column, dataRender, children: header} = (child as React.ReactElement<{column: string; dataRender: (arg: {value: unknown}) => React.ReactNode; children: React.ReactNode}>).props;
                return column === 'guardians'
                    ? <div><span>{header}</span><span data-testid="guardians-cell">{dataRender({value: [
                        {firstName: 'Ondřej', lastName: 'Kratochvíl'}, {firstName: 'Lenka', lastName: 'Kratochvílová'}]})}</span></div>
                    : null;
            })}
        </div>
    ),
}));

describe('LegalGuardianGroupsPage', () => {
    it('renders heading and the group table without a create button', () => {
        vi.mocked(useHalPageData).mockReturnValue({
            isLoading: false,
            error: null,
            route: {navigateToResource: vi.fn()},
        } as unknown as ReturnType<typeof useHalPageData>);
        render(<LegalGuardianGroupsPage/>);
        expect(screen.getByRole('heading', {name: 'Zákonní zástupci'})).toBeInTheDocument();
        expect(screen.getByTestId('table-legalGuardianGroupSummaryResponseList')).toBeInTheDocument();
        expect(screen.queryByRole('button')).not.toBeInTheDocument();
    });

    it('shows guardian names column between name and minor count', () => {
        vi.mocked(useHalPageData).mockReturnValue({
            isLoading: false,
            error: null,
            route: {navigateToResource: vi.fn()},
        } as unknown as ReturnType<typeof useHalPageData>);
        render(<LegalGuardianGroupsPage/>);
        expect(screen.getByText('Zástupci')).toBeInTheDocument();
        expect(screen.getByTestId('guardians-cell')).toHaveTextContent('Ondřej Kratochvíl, Lenka Kratochvílová');
    });
});
