import type {ReactElement} from "react";
import {Card} from "../UI";
import {labels} from "../../localization";
import {formatCurrency} from "./financeFormatters.ts";

export const BalanceCard = ({balance, currency}: {balance: number | undefined; currency: string | undefined}): ReactElement => {
    const isNegative = typeof balance === 'number' && balance < 0;

    return (
        <Card className="p-6">
            <div className="flex flex-col gap-2">
                <p className="text-xs uppercase font-semibold text-text-secondary tracking-wide">
                    {labels.finance.balance}
                </p>
                <p className={`text-4xl font-bold ${isNegative ? 'text-red-600 dark:text-red-400' : 'text-green-700 dark:text-green-400'}`}>
                    {formatCurrency(balance, currency)}
                </p>
            </div>
        </Card>
    );
};
