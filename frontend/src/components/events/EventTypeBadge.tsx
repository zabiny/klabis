import type {ReactElement} from 'react';
import type {EventTypeCatalogItem} from '../../hooks/useEventTypes.ts';

interface EventTypeBadgeProps {
    eventType: EventTypeCatalogItem;
}

/**
 * Displays an event type as a colored pill badge (color dot + name).
 * Used in the events table column and event detail basic info section.
 */
export function EventTypeBadge({eventType}: EventTypeBadgeProps): ReactElement {
    return (
        <span className="inline-flex items-center gap-1.5">
            {eventType.color && (
                <span
                    className="inline-block w-3 h-3 rounded-full flex-shrink-0"
                    style={{backgroundColor: eventType.color}}
                    aria-hidden="true"
                />
            )}
            <span className="text-sm text-text-primary">{eventType.name}</span>
        </span>
    );
}
