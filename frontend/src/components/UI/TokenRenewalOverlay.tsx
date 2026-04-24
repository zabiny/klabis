import {Loader2} from 'lucide-react';
import {useTokenRenewalVisibility} from '../../hooks/useTokenRenewalVisibility';

export const TokenRenewalOverlay = () => {
    const visible = useTokenRenewalVisibility();

    if (!visible) return null;

    return (
        <div
            className="fixed inset-0 z-50 flex items-center justify-center bg-black/50"
            role="status"
            aria-label="Obnovuji přihlášení..."
            aria-live="assertive"
        >
            <div className="flex flex-col items-center gap-3 text-white">
                <Loader2 className="w-10 h-10 animate-spin"/>
                <span className="text-lg font-medium">Obnovuji přihlášení...</span>
            </div>
        </div>
    );
};
