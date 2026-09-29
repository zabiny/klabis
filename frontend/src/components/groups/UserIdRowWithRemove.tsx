import type {ReactElement} from 'react';
import {Button, DetailRow} from '../UI';
import {UserMinus} from 'lucide-react';

interface UserIdRowWithRemoveProps {
    userId: string;
    removeAriaLabel?: string;
    onRemove?: () => void;
}

/**
 * Renders a group owner identified only by user id. Used for family group parents, who are users
 * of the system and need not have a member profile: the API exposes no link to resolve a name for
 * them, so the raw id is the only thing there is to show.
 */
export const UserIdRowWithRemove = ({userId, removeAriaLabel, onRemove}: UserIdRowWithRemoveProps): ReactElement => (
    <DetailRow label="">
        <div className="flex items-center justify-between w-full gap-3">
            <span className="font-mono text-text-primary break-all">{userId}</span>
            {onRemove && (
                <Button
                    variant="ghost"
                    size="sm"
                    className="text-red-600"
                    aria-label={removeAriaLabel}
                    onClick={onRemove}
                >
                    <UserMinus className="w-4 h-4"/>
                </Button>
            )}
        </div>
    </DetailRow>
);
