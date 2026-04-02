import '@testing-library/jest-dom';
import {render, screen, fireEvent} from '@testing-library/react';
import {SuspensionWarningDialog} from './SuspensionWarningDialog';
import {vi} from 'vitest';

describe('SuspensionWarningDialog (task 6.7)', () => {
    const baseProps = {
        isOpen: true,
        onClose: vi.fn(),
        affectedGroups: [
            {groupId: 'g-1', groupName: 'Trail Runners', groupType: 'FreeGroup'},
        ],
    };

    beforeEach(() => {
        vi.clearAllMocks();
    });

    it('renders dialog title when open', () => {
        render(<SuspensionWarningDialog {...baseProps}/>);
        expect(screen.getByText('Varování: Správce skupin')).toBeInTheDocument();
    });

    it('renders description text', () => {
        render(<SuspensionWarningDialog {...baseProps}/>);
        expect(screen.getByText(/posledním správcem/i)).toBeInTheDocument();
    });

    it('renders affected group names', () => {
        render(<SuspensionWarningDialog {...baseProps}/>);
        expect(screen.getByText('Trail Runners')).toBeInTheDocument();
    });

    it('shows "Určit nástupce" and "Rozpustit skupinu" for FreeGroup', () => {
        render(<SuspensionWarningDialog {...baseProps}/>);
        expect(screen.getByRole('button', {name: /určit nástupce/i})).toBeInTheDocument();
        expect(screen.getByRole('button', {name: /rozpustit skupinu/i})).toBeInTheDocument();
    });

    it('shows "Určit nástupce" and "Rozpustit skupinu" for FamilyGroup', () => {
        const props = {
            ...baseProps,
            affectedGroups: [{groupId: 'g-2', groupName: 'Rodina Novákových', groupType: 'FamilyGroup'}],
        };
        render(<SuspensionWarningDialog {...props}/>);
        expect(screen.getByRole('button', {name: /určit nástupce/i})).toBeInTheDocument();
        expect(screen.getByRole('button', {name: /rozpustit skupinu/i})).toBeInTheDocument();
    });

    it('shows only "Určit nástupce" for TrainingGroup (no dissolve option)', () => {
        const props = {
            ...baseProps,
            affectedGroups: [{groupId: 'g-3', groupName: 'U10 Závoďáci', groupType: 'TrainingGroup'}],
        };
        render(<SuspensionWarningDialog {...props}/>);
        expect(screen.getByRole('button', {name: /určit nástupce/i})).toBeInTheDocument();
        expect(screen.queryByRole('button', {name: /rozpustit skupinu/i})).not.toBeInTheDocument();
    });

    it('shows human-readable group type label for FreeGroup', () => {
        render(<SuspensionWarningDialog {...baseProps}/>);
        expect(screen.getByText('Volná skupina')).toBeInTheDocument();
    });

    it('shows human-readable group type label for TrainingGroup', () => {
        const props = {
            ...baseProps,
            affectedGroups: [{groupId: 'g-3', groupName: 'U10', groupType: 'TrainingGroup'}],
        };
        render(<SuspensionWarningDialog {...props}/>);
        expect(screen.getByText('Tréninková skupina')).toBeInTheDocument();
    });

    it('shows human-readable group type label for FamilyGroup', () => {
        const props = {
            ...baseProps,
            affectedGroups: [{groupId: 'g-2', groupName: 'Novákovi', groupType: 'FamilyGroup'}],
        };
        render(<SuspensionWarningDialog {...props}/>);
        expect(screen.getByText('Rodinná skupina')).toBeInTheDocument();
    });

    it('renders multiple affected groups', () => {
        const props = {
            ...baseProps,
            affectedGroups: [
                {groupId: 'g-1', groupName: 'Trail Runners', groupType: 'FreeGroup'},
                {groupId: 'g-2', groupName: 'Rodina Novákových', groupType: 'FamilyGroup'},
                {groupId: 'g-3', groupName: 'U10', groupType: 'TrainingGroup'},
            ],
        };
        render(<SuspensionWarningDialog {...props}/>);
        expect(screen.getByText('Trail Runners')).toBeInTheDocument();
        expect(screen.getByText('Rodina Novákových')).toBeInTheDocument();
        expect(screen.getByText('U10')).toBeInTheDocument();
    });

    it('calls onClose when close button is clicked', () => {
        const onClose = vi.fn();
        render(<SuspensionWarningDialog {...baseProps} onClose={onClose}/>);
        fireEvent.click(screen.getByRole('button', {name: /zrušit/i}));
        expect(onClose).toHaveBeenCalledTimes(1);
    });

    it('renders nothing when isOpen is false', () => {
        render(<SuspensionWarningDialog {...baseProps} isOpen={false}/>);
        expect(screen.queryByText('Varování: Správce skupin')).not.toBeInTheDocument();
    });
});
