import '@testing-library/jest-dom';
import {render, screen} from '@testing-library/react';
import {vi} from 'vitest';
import {ApiError} from '../../api/apiError';
import {useHalPageData} from '../../hooks/useHalPageData';
import {TrainingGroupsPage} from './TrainingGroupsPage';

vi.mock('../../hooks/useHalPageData', () => ({useHalPageData: vi.fn()}));

describe('TrainingGroupsPage', () => {
    it('shows the Czech text of a load error instead of the HTTP status', () => {
        vi.mocked(useHalPageData).mockReturnValue({
            isLoading: false,
            error: new ApiError(403, {type: '/errors/forbidden', detail: 'Access denied'}),
            route: {navigateToResource: vi.fn()},
        } as unknown as ReturnType<typeof useHalPageData>);

        render(<TrainingGroupsPage/>);

        expect(screen.getByText('K této akci nemáte oprávnění.')).toBeInTheDocument();
        expect(screen.queryByText(/HTTP 403/)).not.toBeInTheDocument();
    });
});
