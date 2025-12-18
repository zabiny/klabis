package club.klabis.finance.infrastructure.listeners;

import club.klabis.finance.domain.MoneyAmount;
import club.klabis.finance.domain.events.AccountCreatedEvent;
import club.klabis.finance.domain.events.DepositedAmountEvent;
import club.klabis.finance.domain.events.WithdrawnAmountEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class AccountsCountProjector {

    private MoneyAmount overalClubBalance = MoneyAmount.ZERO;
    private int count = 0;

    @EventListener
    public void onAccountCreate(AccountCreatedEvent ev) {
        overalClubBalance.add(ev.getInitialBalance());
        count++;
    }

    @EventListener
    public void onDeposit(DepositedAmountEvent ev) {
        overalClubBalance = overalClubBalance.add(ev.getAmount());
    }

    @EventListener
    public void onWithdraw(WithdrawnAmountEvent ev) {
        overalClubBalance = overalClubBalance.subtract(ev.getAmount());
    }

    public AccountsStats getStats() {
        return new AccountsStats(count, overalClubBalance);
    }

    record AccountsStats(int accountsCount, MoneyAmount overallBalance) {

    }

    @Scheduled(fixedDelayString = "PT30S", initialDelayString = "PT30S")
    void printStats() {
        System.out.println("Account stats: " + getStats());
    }
}
