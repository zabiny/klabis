import {labels} from "../../localization";
import {formatDate} from "../../utils/dateUtils";

export {formatDate};

export function formatCurrency(amount: number | undefined, currency: string | undefined, options?: {absolute?: boolean}): string {
    if (amount === undefined || amount === null) return `- ${labels.finance.currency}`;
    const value = options?.absolute ? Math.abs(amount) : amount;
    const formatted = new Intl.NumberFormat('cs-CZ', {minimumFractionDigits: 2, maximumFractionDigits: 2}).format(value);
    return `${formatted} ${currency ?? labels.finance.currency}`;
}
