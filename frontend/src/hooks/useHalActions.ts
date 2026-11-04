/**
 * Custom hook for HAL resource actions
 * Handles common navigation, form selection, and form submission logic
 */

import {useNavigate} from 'react-router-dom';
import {toRoutePath} from '../api/apiPath';

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
        const path = toRoutePath(href);
        navigate(path);
    };

    return {
        handleNavigateToItem,
    };
}
