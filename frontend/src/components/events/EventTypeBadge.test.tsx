import '@testing-library/jest-dom';
import {render, screen} from '@testing-library/react';
import {describe, it, expect} from 'vitest';
import {EventTypeBadge} from './EventTypeBadge.tsx';

describe('EventTypeBadge', () => {
    it('renders event type name', () => {
        render(<EventTypeBadge eventType={{id: '1', name: 'Trénink', sortOrder: 1}}/>);
        expect(screen.getByText('Trénink')).toBeInTheDocument();
    });

    it('renders color dot when color is provided', () => {
        render(<EventTypeBadge eventType={{id: '1', name: 'Trénink', color: '#FF5733', sortOrder: 1}}/>);
        const dot = document.querySelector('[aria-hidden="true"]') as HTMLElement;
        expect(dot).toBeInTheDocument();
        expect(dot.style.backgroundColor).toBe('rgb(255, 87, 51)');
    });

    it('does not render color dot when color is absent', () => {
        render(<EventTypeBadge eventType={{id: '1', name: 'Trénink', sortOrder: 1}}/>);
        expect(document.querySelector('[aria-hidden="true"]')).not.toBeInTheDocument();
    });
});
