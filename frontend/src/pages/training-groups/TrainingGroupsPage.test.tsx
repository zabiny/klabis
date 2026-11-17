import '@testing-library/jest-dom';
import {render, screen} from '@testing-library/react';
import {MemoryRouter} from 'react-router-dom';
import {vi} from 'vitest';
import {ApiError} from '../../api/apiError';
import {useHalPageData} from '../../hooks/useHalPageData';
import {TrainingGroupsPage} from './TrainingGroupsPage';

vi.mock('../../hooks/useHalPageData', () => ({useHalPageData: vi.fn()}));

describe('TrainingGroupsPage', () => {
    it('shows the page-level error for a 403', () => {
        vi.mocked(useHalPageData).mockReturnValue({
            resourceData: null,
            isLoading: false,
            error: new ApiError(403, {type: '/errors/forbidden', detail: 'Access denied'}),
            queryState: 'error',
            navigateToResource: vi.fn(),
        } as unknown as ReturnType<typeof useHalPageData>);

        render(
            <MemoryRouter>
                <TrainingGroupsPage/>
            </MemoryRouter>
        );

        expect(screen.getByText('Přístup odepřen')).toBeInTheDocument();
        expect(screen.queryByText(/HTTP 403/)).not.toBeInTheDocument();
    });
});
