import { clsx } from 'clsx';

export interface PasswordRequirement {
    id: string;
    label: string;
    met: boolean;
}

interface PasswordStrengthIndicatorProps {
    requirements: PasswordRequirement[];
    className?: string;
}

/**
 * PasswordStrengthIndicator - Visual feedback for password requirements
 * Shows checklist of all password requirements with check/cross icons
 */
export const PasswordStrengthIndicator = ({
    requirements,
    className,
}: PasswordStrengthIndicatorProps) => {
    const metCount = requirements.filter((r) => r.met).length;
    const totalCount = requirements.length;
    const percentage = (metCount / totalCount) * 100;

    // Determine strength color
    const getStrengthColor = () => {
        if (percentage < 40) return 'bg-error';
        if (percentage < 80) return 'bg-warning';
        return 'bg-success';
    };

    const getStrengthLabel = () => {
        if (percentage < 40) return 'Slabé heslo';
        if (percentage < 80) return 'Střední heslo';
        return 'Silné heslo';
    };

    return (
        <div className={clsx('space-y-3', className)}>
            {/* Progress bar */}
            <div className="space-y-1">
                <div className="flex justify-between items-center">
                    <span className="text-sm font-medium text-text-secondary">
                        {getStrengthLabel()}
                    </span>
                    <span className="text-xs text-text-tertiary">
                        {metCount} z {totalCount} požadavků
                    </span>
                </div>
                <div className="h-2 bg-surface-base rounded-full overflow-hidden border border-border">
                    <div
                        className={clsx(
                            'h-full transition-all duration-300 ease-out',
                            getStrengthColor()
                        )}
                        style={{ width: `${percentage}%` }}
                    />
                </div>
            </div>

            {/* Requirements checklist */}
            <ul className="space-y-2" role="list" aria-label="Požadavky na heslo">
                {requirements.map((req) => (
                    <li
                        key={req.id}
                        className={clsx(
                            'flex items-start gap-2 text-sm',
                            req.met
                                ? 'text-success'
                                : 'text-text-tertiary'
                        )}
                    >
                        <span className="flex-shrink-0 mt-0.5" aria-hidden="true">
                            {req.met ? '✓' : '○'}
                        </span>
                        <span>{req.label}</span>
                    </li>
                ))}
            </ul>
        </div>
    );
};

PasswordStrengthIndicator.displayName = 'PasswordStrengthIndicator';
