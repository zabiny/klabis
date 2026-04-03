export const RENEWAL_SHOW_EVENT = 'klabis:renewal:show';
export const RENEWAL_HIDE_EVENT = 'klabis:renewal:hide';

let renewalVisible = false;

export const showRenewalOverlay = (): void => {
    if (renewalVisible) return;
    renewalVisible = true;
    window.dispatchEvent(new CustomEvent(RENEWAL_SHOW_EVENT));
};

export const hideRenewalOverlay = (): void => {
    if (!renewalVisible) return;
    renewalVisible = false;
    window.dispatchEvent(new CustomEvent(RENEWAL_HIDE_EVENT));
};
