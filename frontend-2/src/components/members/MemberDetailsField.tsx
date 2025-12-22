import {type ReactNode} from 'react';

interface MemberDetailsFieldProps {
    label: string;
    value: any;
    render?: (value: any) => ReactNode;
}

/**
 * Displays a single field in member details
 * Renders label and value in a row format
 */
export const MemberDetailsField = ({
                                       label,
                                       value,
                                       render,
                                   }: MemberDetailsFieldProps) => {
    // Don't render if value is null/undefined
    if (value === null || value === undefined) {
        return null;
    }

    let displayValue: ReactNode;

    if (render) {
        displayValue = render(value);
    } else if (typeof value === 'boolean') {
        displayValue = value ? 'Ano' : 'Ne';
    } else if (Array.isArray(value)) {
        displayValue = value.length === 0 ? '—' : value.join(', ');
    } else if (typeof value === 'object') {
        displayValue = JSON.stringify(value);
    } else {
        displayValue = String(value);
    }

    return (
        <div
            className="grid grid-cols-3 gap-4 items-start py-3 border-b border-gray-200 dark:border-gray-700 last:border-0">
            <dt className="font-medium text-gray-700 dark:text-gray-300">{label}</dt>
            <dd className="col-span-2 text-gray-900 dark:text-gray-100">{displayValue || '—'}</dd>
        </div>
    );
};
