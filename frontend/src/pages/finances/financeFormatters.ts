import {labels} from "../../localization";

export function formatCurrency(amount: number | undefined, currency: string | undefined): string {
    if (amount === undefined || amount === null) return `- ${labels.finance.currency}`;
    const formatted = new Intl.NumberFormat('cs-CZ', {minimumFractionDigits: 2, maximumFractionDigits: 2}).format(amount);
    return `${formatted} ${currency ?? labels.finance.currency}`;
}

export function formatDate(dateStr: string | undefined): string {
    if (!dateStr) return '-';
    const date = new Date(dateStr);
    return new Intl.DateTimeFormat('cs-CZ').format(date);
}
