import {type ReactNode} from 'react';

interface MemberDetailsCardProps {
    title: string;
    children: ReactNode;
}

/**
 * Card component for grouping member details into sections
 */
export const MemberDetailsCard = ({title, children}: MemberDetailsCardProps) => {
    return (
        <div className="border rounded-lg bg-white dark:bg-gray-800 shadow-sm">
            <div className="border-b border-gray-200 dark:border-gray-700 px-6 py-4">
                <h3 className="text-lg font-semibold text-gray-900 dark:text-white">
                    {title}
                </h3>
            </div>
            <div className="px-6 py-4">
                {children}
            </div>
        </div>
    );
};
