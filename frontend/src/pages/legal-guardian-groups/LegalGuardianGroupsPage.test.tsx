import '@testing-library/jest-dom';
import React from 'react';
import {render, screen} from '@testing-library/react';
import {vi} from 'vitest';
import type {HalResponse} from '../../api';
import {useHalPageData} from '../../hooks/useHalPageData';
import {makePageData} from '../../__mocks__/pageData';
import {LegalGuardianGroupsPage} from './LegalGuardianGroupsPage';

vi.mock('../../hooks/useHalPageData', () => ({useHalPageData: vi.fn()}));

vi.mock('../../components/HalNavigator2/HalEmbeddedTable.tsx', () => ({
    HalEmbeddedTable: ({children}: {children: React.ReactNode}) => (
        <div data-testid="groups-table">
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

const resource: HalResponse = {_links: {self: {href: 'http://localhost/api/legal-guardian-groups'}}};

describe('LegalGuardianGroupsPage', () => {
    it('renders heading and the group table without a create button', () => {
        vi.mocked(useHalPageData).mockReturnValue(makePageData({resourceData: resource}));
        render(<LegalGuardianGroupsPage/>);
        expect(screen.getByRole('heading', {name: 'Zákonní zástupci'})).toBeInTheDocument();
        expect(screen.getByTestId('groups-table')).toBeInTheDocument();
        expect(screen.queryByRole('button')).not.toBeInTheDocument();
    });

    it('shows guardian names column between name and minor count', () => {
        vi.mocked(useHalPageData).mockReturnValue(makePageData({resourceData: resource}));
        render(<LegalGuardianGroupsPage/>);
        expect(screen.getByText('Zástupci')).toBeInTheDocument();
        expect(screen.getByTestId('guardians-cell')).toHaveTextContent('Ondřej Kratochvíl, Lenka Kratochvílová');
    });
});
