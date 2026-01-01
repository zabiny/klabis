package club.klabis.finance.infrastructure.listeners;

import club.klabis.finance.domain.MoneyAmount;
import club.klabis.finance.domain.events.AccountCreatedEvent;
import club.klabis.finance.domain.events.DepositedAmountEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class AccountsCountProjector {

    private MoneyAmount overalClubBalance = MoneyAmount.ZERO;
    private int count = 0;

    private static final Logger LOG = LoggerFactory.getLogger(AccountsCountProjector.class);

    @EventListener
    public void onAccountCreate(AccountCreatedEvent ev) {
        overalClubBalance = overalClubBalance.add(ev.getInitialBalance());
        count++;
    }

    @EventListener
    public void onDeposit(DepositedAmountEvent ev) {
        overalClubBalance = overalClubBalance.add(ev.getAmount());
    }

    public AccountsStats getStats() {
        return new AccountsStats(count, overalClubBalance);
    }

    record AccountsStats(int accountsCount, MoneyAmount overallBalance) {

    }

    @Scheduled(fixedDelayString = "PT30M", initialDelayString = "PT30S")
    void printStats() {
        LOG.debug("Account stats: {}", getStats());
    }
}
