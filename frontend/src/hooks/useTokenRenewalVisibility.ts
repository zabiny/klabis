import {useEffect, useState} from 'react';
import {RENEWAL_HIDE_EVENT, RENEWAL_SHOW_EVENT} from '../api/tokenRenewalState';

export function useTokenRenewalVisibility(): boolean {
    const [visible, setVisible] = useState(false);

    useEffect(() => {
        const show = () => setVisible(true);
        const hide = () => setVisible(false);
        window.addEventListener(RENEWAL_SHOW_EVENT, show);
        window.addEventListener(RENEWAL_HIDE_EVENT, hide);
        return () => {
            window.removeEventListener(RENEWAL_SHOW_EVENT, show);
            window.removeEventListener(RENEWAL_HIDE_EVENT, hide);
        };
    }, []);

    return visible;
}
