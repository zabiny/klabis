import '@testing-library/jest-dom';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, it, expect, vi } from 'vitest';
import { PillGroup } from './PillGroup';

type Status = 'ACTIVE' | 'INACTIVE' | 'ALL';

const OPTIONS: { value: Status; label: string }[] = [
    { value: 'ACTIVE', label: 'Aktivní' },
    { value: 'INACTIVE', label: 'Neaktivní' },
    { value: 'ALL', label: 'Vše' },
];

const renderPillGroup = (selectedValue: Status = 'ACTIVE', onChange = vi.fn()) =>
    render(
        <PillGroup<Status>
            options={OPTIONS}
            selectedValue={selectedValue}
            onChange={onChange}
            ariaLabel="Status filter"
        />,
    );

describe('PillGroup', () => {
    it('renders all option buttons', () => {
        renderPillGroup();
        expect(screen.getByRole('button', { name: 'Aktivní' })).toBeInTheDocument();
        expect(screen.getByRole('button', { name: 'Neaktivní' })).toBeInTheDocument();
        expect(screen.getByRole('button', { name: 'Vše' })).toBeInTheDocument();
    });

    it('renders a group with the provided aria-label', () => {
        renderPillGroup();
        expect(screen.getByRole('group', { name: 'Status filter' })).toBeInTheDocument();
    });

    it('marks the selected option with aria-pressed=true', () => {
        renderPillGroup('INACTIVE');
        expect(screen.getByRole('button', { name: 'Neaktivní' })).toHaveAttribute('aria-pressed', 'true');
        expect(screen.getByRole('button', { name: 'Aktivní' })).toHaveAttribute('aria-pressed', 'false');
        expect(screen.getByRole('button', { name: 'Vše' })).toHaveAttribute('aria-pressed', 'false');
    });

    it('calls onChange with the clicked value', async () => {
        const onChange = vi.fn();
        renderPillGroup('ACTIVE', onChange);
        await userEvent.click(screen.getByRole('button', { name: 'Neaktivní' }));
        expect(onChange).toHaveBeenCalledWith('INACTIVE');
    });

    it('calls onChange when already-selected pill is clicked', async () => {
        const onChange = vi.fn();
        renderPillGroup('ACTIVE', onChange);
        await userEvent.click(screen.getByRole('button', { name: 'Aktivní' }));
        expect(onChange).toHaveBeenCalledWith('ACTIVE');
    });

    describe('disabled options', () => {
        it('renders a disabled button with aria-disabled=true when value is in disabledValues', () => {
            render(
                <PillGroup<Status>
                    options={OPTIONS}
                    selectedValue="ALL"
                    onChange={vi.fn()}
                    ariaLabel="Status filter"
                    disabledValues={['ACTIVE', 'INACTIVE']}
                />,
            );
            expect(screen.getByRole('button', { name: 'Aktivní' })).toHaveAttribute('aria-disabled', 'true');
            expect(screen.getByRole('button', { name: 'Neaktivní' })).toHaveAttribute('aria-disabled', 'true');
            expect(screen.getByRole('button', { name: 'Vše' })).not.toHaveAttribute('aria-disabled', 'true');
        });

        it('does not call onChange when a disabled option is clicked', async () => {
            const onChange = vi.fn();
            render(
                <PillGroup<Status>
                    options={OPTIONS}
                    selectedValue="ALL"
                    onChange={onChange}
                    ariaLabel="Status filter"
                    disabledValues={['ACTIVE']}
                />,
            );
            await userEvent.click(screen.getByRole('button', { name: 'Aktivní' }));
            expect(onChange).not.toHaveBeenCalled();
        });

        it('renders a tooltip title on disabled buttons when disabledTooltip is provided', () => {
            render(
                <PillGroup<Status>
                    options={OPTIONS}
                    selectedValue="ALL"
                    onChange={vi.fn()}
                    ariaLabel="Status filter"
                    disabledValues={['ACTIVE']}
                    disabledTooltip="Not available"
                />,
            );
            expect(screen.getByRole('button', { name: 'Aktivní' })).toHaveAttribute('title', 'Not available');
        });

        it('does not render title on enabled buttons even when disabledTooltip is provided', () => {
            render(
                <PillGroup<Status>
                    options={OPTIONS}
                    selectedValue="ALL"
                    onChange={vi.fn()}
                    ariaLabel="Status filter"
                    disabledValues={['ACTIVE']}
                    disabledTooltip="Not available"
                />,
            );
            expect(screen.getByRole('button', { name: 'Vše' })).not.toHaveAttribute('title');
        });
    });
});
