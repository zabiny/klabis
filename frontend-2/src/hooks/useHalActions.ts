/**
 * Custom hook for HAL resource actions
 * Handles common navigation, form selection, and form submission logic
 */

import {useNavigate} from 'react-router-dom';
import {extractNavigationPath} from '../utils/navigationPath';

interface UseHalActionsReturn {
    handleNavigateToItem: (href: string) => void;
}

/**
 * Hook to manage HAL resource actions (navigation, forms, etc.)
 * Shared between collection and item display components
 */
export function useHalActions(): UseHalActionsReturn {
    const navigate = useNavigate();

    const handleNavigateToItem = (href: string) => {
        const path = extractNavigationPath(href);
        navigate(path);
    };

    return {
        handleNavigateToItem,
    };
}
