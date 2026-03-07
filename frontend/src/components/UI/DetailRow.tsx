import type {ReactNode} from 'react'

interface DetailRowProps {
    label: string;
    children: ReactNode;
}

export const DetailRow = ({label, children}: DetailRowProps) => (
    <div className="flex flex-col sm:flex-row sm:gap-4 py-2 border-b border-border last:border-b-0">
        <dt className="text-sm text-text-secondary sm:w-48 shrink-0">{label}</dt>
        <dd className="text-sm text-text-primary">{children}</dd>
    </div>
);
