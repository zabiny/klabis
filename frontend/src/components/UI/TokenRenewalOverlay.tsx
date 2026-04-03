import {useEffect, useState} from 'react';
import {Loader2} from 'lucide-react';
import {RENEWAL_HIDE_EVENT, RENEWAL_SHOW_EVENT} from '../../api/tokenRenewalState';

export const TokenRenewalOverlay = () => {
    const [visible, setVisible] = useState(false);

    useEffect(() => {
        const handleShow = () => setVisible(true);
        const handleHide = () => setVisible(false);

        window.addEventListener(RENEWAL_SHOW_EVENT, handleShow);
        window.addEventListener(RENEWAL_HIDE_EVENT, handleHide);

        return () => {
            window.removeEventListener(RENEWAL_SHOW_EVENT, handleShow);
            window.removeEventListener(RENEWAL_HIDE_EVENT, handleHide);
        };
    }, []);

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
