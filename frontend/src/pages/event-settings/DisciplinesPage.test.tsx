import '@testing-library/jest-dom';
import {render, screen} from '@testing-library/react';
import {MemoryRouter} from 'react-router-dom';
import {describe, expect, it, vi} from 'vitest';
import {DisciplinesPage} from './DisciplinesPage';
import {useHalPageData} from '../../hooks/useHalPageData';
import {makePageData} from '../../__mocks__/pageData';
import type {HalResponse} from '../../api';

vi.mock('../../hooks/useHalPageData', () => ({useHalPageData: vi.fn()}));
vi.mock('../../hooks/useIsAdmin', () => ({useIsAdmin: () => ({isAdmin: false})}));

const renderPage = () => render(<MemoryRouter><DisciplinesPage/></MemoryRouter>);

describe('DisciplinesPage', () => {
    it('lists disciplines from the embedded collection', () => {
        vi.mocked(useHalPageData).mockReturnValue(makePageData({
            resourceData: {
                _embedded: {disciplineList: [{name: 'Orienteering', _links: {self: {href: '/api/disciplines/1'}}}]},
                _links: {self: {href: '/api/disciplines'}},
            } as HalResponse,
            queryState: 'success',
        }));

        renderPage();

        expect(screen.getByText(/Orienteering/)).toBeInTheDocument();
    });

    it('shows a spinner instead of the table while loading', () => {
        vi.mocked(useHalPageData).mockReturnValue(makePageData({isLoading: true, queryState: 'pending'}));

        renderPage();

        expect(screen.queryByRole('table')).not.toBeInTheDocument();
        expect(screen.getByRole('status')).toBeInTheDocument();
    });
});
