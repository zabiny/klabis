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
        <div className="border border-border rounded-md bg-surface-raised shadow-sm">
            <div className="border-b border-border px-6 py-4">
                <h3 className="text-lg font-semibold text-text-primary">
                    {title}
                </h3>
            </div>
            <div className="px-6 py-4">
                {children}
            </div>
        </div>
    );
};
