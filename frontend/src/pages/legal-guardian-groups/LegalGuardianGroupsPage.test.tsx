import '@testing-library/jest-dom';
import {render, screen} from '@testing-library/react';
import {vi} from 'vitest';
import {useHalPageData} from '../../hooks/useHalPageData';
import {LegalGuardianGroupsPage} from './LegalGuardianGroupsPage';

vi.mock('../../hooks/useHalPageData', () => ({useHalPageData: vi.fn()}));

vi.mock('../../components/HalNavigator2/HalEmbeddedTable.tsx', () => ({
    HalEmbeddedTable: ({collectionName}: {collectionName: string}) => <div data-testid={`table-${collectionName}`}/>,
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
});
