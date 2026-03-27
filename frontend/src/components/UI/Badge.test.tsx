import {render, screen} from '@testing-library/react';
import {Badge} from './Badge';

describe('Badge Component', () => {
    describe('Rendering', () => {
        it('should render children', () => {
            render(<Badge>Active</Badge>);
            expect(screen.getByText('Active')).toBeInTheDocument();
        });

        it('should render as inline span element', () => {
            render(<Badge>Label</Badge>);
            expect(screen.getByText('Label').tagName).toBe('SPAN');
        });
    });

    describe('Variants', () => {
        it('should render default variant', () => {
            render(<Badge variant="default">Default</Badge>);
            expect(screen.getByText('Default')).toHaveClass('bg-surface-raised');
        });

        it('should render success variant', () => {
            render(<Badge variant="success">Active</Badge>);
            const badge = screen.getByText('Active');
            expect(badge).toHaveClass('bg-success/20');
            expect(badge).toHaveClass('text-success');
        });

        it('should render warning variant', () => {
            render(<Badge variant="warning">Warning</Badge>);
            expect(screen.getByText('Warning')).toHaveClass('bg-warning/20');
        });

        it('should render error variant', () => {
            render(<Badge variant="error">Error</Badge>);
            expect(screen.getByText('Error')).toHaveClass('bg-error/20');
        });

        it('should render info variant', () => {
            render(<Badge variant="info">Info</Badge>);
            expect(screen.getByText('Info')).toHaveClass('bg-info/20');
        });

        it('should render orange variant with orange-50 background and orange-700 text', () => {
            render(<Badge variant="orange">Trenér III</Badge>);
            const badge = screen.getByText('Trenér III');
            expect(badge).toHaveClass('bg-orange-50');
            expect(badge).toHaveClass('text-orange-700');
        });

        it('should render blue variant with blue-50 background and blue-700 text', () => {
            render(<Badge variant="blue">Rozhodčí II</Badge>);
            const badge = screen.getByText('Rozhodčí II');
            expect(badge).toHaveClass('bg-blue-50');
            expect(badge).toHaveClass('text-blue-700');
        });
    });

    describe('Sizes', () => {
        it('should render md size by default', () => {
            render(<Badge>Label</Badge>);
            expect(screen.getByText('Label')).toHaveClass('text-sm');
        });

        it('should render sm size', () => {
            render(<Badge size="sm">Small</Badge>);
            expect(screen.getByText('Small')).toHaveClass('text-xs');
        });

        it('should render lg size', () => {
            render(<Badge size="lg">Large</Badge>);
            expect(screen.getByText('Large')).toHaveClass('text-base');
        });
    });

    describe('Custom className', () => {
        it('should apply custom className', () => {
            render(<Badge className="my-custom-class">Label</Badge>);
            expect(screen.getByText('Label')).toHaveClass('my-custom-class');
        });
    });
});
