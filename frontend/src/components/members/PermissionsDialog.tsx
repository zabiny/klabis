import {useEffect, useState} from 'react';
import {Modal, Spinner} from '../UI';
import {useToast} from '../../contexts/ToastContext';
import {useAuthorizedMutation, useAuthorizedQuery} from '../../hooks/useAuthorizedFetch';

interface PermissionsDialogProps {
    isOpen: boolean;
    onClose: () => void;
    permissionsUrl: string;
    memberName: string;
}

const PERMISSION_LABELS: Record<string, { label: string; description: string; color: string }> = {
    'MEMBERS:READ':        {label: 'Zobrazení členů',   description: 'Přístup k seznamu a detailům členů',             color: 'bg-blue-100 text-blue-600'},
    'MEMBERS:CREATE':      {label: 'Vytváření členů',   description: 'Registrace nových členů v klubu',                color: 'bg-green-100 text-green-600'},
    'MEMBERS:UPDATE':      {label: 'Úprava členů',      description: 'Změna osobních a kontaktních údajů členů',       color: 'bg-purple-100 text-purple-600'},
    'MEMBERS:TERMINATE':   {label: 'Ukončení členství', description: 'Deaktivace členů a ukončení jejich členství',    color: 'bg-orange-100 text-orange-600'},
    'MEMBERS:PERMISSIONS': {label: 'Správa oprávnění',  description: 'Přidělování a odebírání oprávnění uživatelům',   color: 'bg-red-100 text-red-600'},
    'EVENTS:READ':         {label: 'Zobrazení akcí',    description: 'Přístup k seznamu a detailům akcí',              color: 'bg-teal-100 text-teal-600'},
    'EVENTS:MANAGE':       {label: 'Správa akcí',       description: 'Vytváření a úprava akcí',                        color: 'bg-indigo-100 text-indigo-600'},
};

interface PermissionsResponse {
    authorities: string[];
    _links?: {
        self?: { href: string };
    };
}

const PermissionIcon = ({color}: { color: string }) => (
    <div className={`flex-shrink-0 w-9 h-9 rounded-full flex items-center justify-center ${color}`}>
        <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
            <path strokeLinecap="round" strokeLinejoin="round" d="M9 12l2 2 4-4m5.618-4.016A11.955 11.955 0 0112 2.944a11.955 11.955 0 01-8.618 3.04A12.02 12.02 0 003 9c0 5.591 3.824 10.29 9 11.622 5.176-1.332 9-6.03 9-11.622 0-1.042-.133-2.052-.382-3.016z"/>
        </svg>
    </div>
);

const Toggle = ({checked, onChange, disabled}: { checked: boolean; onChange: () => void; disabled: boolean }) => (
    <button
        type="button"
        role="switch"
        aria-checked={checked}
        onClick={onChange}
        disabled={disabled}
        className={`relative inline-flex h-6 w-11 flex-shrink-0 rounded-full border-2 border-transparent transition-colors duration-200 focus:outline-none focus:ring-2 focus:ring-primary focus:ring-offset-2 disabled:opacity-50 disabled:cursor-not-allowed ${
            checked ? 'bg-primary' : 'bg-border'
        }`}
    >
        <span
            className={`pointer-events-none inline-block h-5 w-5 transform rounded-full bg-white shadow ring-0 transition duration-200 ease-in-out ${
                checked ? 'translate-x-5' : 'translate-x-0'
            }`}
        />
    </button>
);

export const PermissionsDialog = ({isOpen, onClose, permissionsUrl, memberName}: PermissionsDialogProps) => {
    const {addToast} = useToast();
    const [selectedAuthorities, setSelectedAuthorities] = useState<Set<string>>(new Set());

    const {data, isLoading} = useAuthorizedQuery<PermissionsResponse>(permissionsUrl, {enabled: isOpen && !!permissionsUrl});

    useEffect(() => {
        if (data?.authorities) {
            setSelectedAuthorities(new Set(data.authorities));
        }
    }, [data]);

    const putUrl = data?._links?.self?.href ?? permissionsUrl;

    const {mutate, isPending, error} = useAuthorizedMutation({
        method: 'PUT',
        onSuccess: () => {
            addToast('Oprávnění uložena', 'success');
            onClose();
        },
    });

    const toggleAuthority = (authority: string) => {
        setSelectedAuthorities(prev => {
            const next = new Set(prev);
            if (next.has(authority)) {
                next.delete(authority);
            } else {
                next.add(authority);
            }
            return next;
        });
    };

    const handleSave = () => {
        mutate({url: putUrl, data: {authorities: Array.from(selectedAuthorities)}});
    };

    const footer = (
        <>
            <button
                type="button"
                onClick={onClose}
                className="inline-flex items-center px-4 py-2 text-sm font-medium rounded-md border border-border text-text-primary hover:bg-surface-raised"
            >
                Zrušit
            </button>
            <button
                type="button"
                onClick={handleSave}
                disabled={isLoading || isPending}
                className="inline-flex items-center px-4 py-2 text-sm font-medium rounded-md bg-primary text-white hover:bg-primary-light disabled:opacity-50 disabled:cursor-not-allowed"
            >
                {isPending ? <Spinner/> : 'Uložit oprávnění'}
            </button>
        </>
    );

    return (
        <Modal
            isOpen={isOpen}
            onClose={onClose}
            title="Oprávnění uživatele"
            size="lg"
            footer={footer}
        >
            <p className="text-xs text-text-secondary mb-1 -mt-1">{memberName}</p>
            <p className="text-sm text-text-secondary mb-4">
                Oprávnění určují, ke kterým funkcím aplikace má uživatel přístup. Změny se projeví okamžitě.
            </p>

            {isLoading ? (
                <div className="flex justify-center py-8">
                    <Spinner/>
                </div>
            ) : (
                <div>
                    {error && (
                        <div className="mb-4 p-3 rounded-md bg-feedback-error/10 text-feedback-error text-sm">
                            {error.message || 'Nepodařilo se uložit oprávnění. Zkuste to prosím znovu.'}
                        </div>
                    )}
                    <div className="divide-y divide-border">
                        {Object.entries(PERMISSION_LABELS).map(([authority, info]) => (
                            <div key={authority} className="flex items-center gap-4 py-3">
                                <PermissionIcon color={info.color}/>
                                <div className="flex-1 min-w-0">
                                    <p className="text-sm font-medium text-text-primary">{info.label}</p>
                                    <p className="text-xs text-text-secondary">{info.description}</p>
                                </div>
                                <Toggle
                                    checked={selectedAuthorities.has(authority)}
                                    onChange={() => toggleAuthority(authority)}
                                    disabled={isLoading || isPending}
                                />
                            </div>
                        ))}
                    </div>
                </div>
            )}
        </Modal>
    );
};

PermissionsDialog.displayName = 'PermissionsDialog';
