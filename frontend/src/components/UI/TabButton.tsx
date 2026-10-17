import type {ReactElement, ReactNode} from 'react';

interface TabButtonProps {
    active: boolean;
    label: string;
    onClick: () => void;
    icon?: ReactNode;
    id?: string;
    controls?: string;
}

export const TabButton = ({active, label, onClick, icon, id, controls}: TabButtonProps): ReactElement => (
    <button
        role="tab"
        id={id}
        aria-controls={controls}
        aria-selected={active}
        onClick={onClick}
        className={`flex items-center gap-2 px-4 py-3 text-sm font-medium transition-colors ${
            active
                ? 'text-primary border-b-2 border-primary'
                : 'text-text-secondary hover:text-text-primary'
        }`}
    >
        {icon}
        {label}
    </button>
);
