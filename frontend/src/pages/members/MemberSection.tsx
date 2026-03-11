import type {ReactNode} from 'react';

interface SectionProps {
    title: string;
    children: ReactNode;
}

export const Section = ({title, children}: SectionProps) => (
    <div className="bg-surface-raised rounded-md border border-border p-6">
        <h3 className="text-xs uppercase font-semibold text-text-secondary mb-4">{title}</h3>
        <dl>{children}</dl>
    </div>
);
