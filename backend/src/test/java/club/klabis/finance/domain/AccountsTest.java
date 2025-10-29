package club.klabis.finance.domain;

import club.klabis.finance.domain.events.AccountCreatedEvent;
import club.klabis.finance.domain.events.DepositedAmountEvent;
import club.klabis.finance.domain.events.TransferedAmountEvent;
import club.klabis.finance.domain.events.WithdrawnAmountEvent;
import club.klabis.members.MemberId;
import com.dpolach.eventsourcing.BaseEvent;
import com.dpolach.eventsourcing.EventsSource;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AccountsTest {
    final MemberId david = new MemberId(1);
    final MemberId john = new MemberId(2);

    private static Optional<Account> rebuild(MemberId accountOwner, BaseEvent... events) {
        return rebuild(events).getAccount(accountOwner);
    }

    private static Accounts rebuild(BaseEvent... events) {
        Accounts result = new Accounts();
        Stream.of(events).forEach(result::apply);
        result.clearPendingEvents();
        return result;
    }

    static void assertNoEvents(EventsSource eventsSource) {
        assertThat(eventsSource.getPendingEvents()).isEmpty();
    }

    @DisplayName("read model tests")
    @Nested
    class ReadTests {
        @DisplayName("it should rebuild CreateAccount event")
        @Test
        void itShouldRebuildCreateAccountEvent() {
            Accounts actual = rebuild(new AccountCreatedEvent(david, MoneyAmount.of(100)),
                    new AccountCreatedEvent(john, MoneyAmount.ZERO));

            assertThat(actual.getAccount(david)).isPresent().get().extracting("balance").isEqualTo(MoneyAmount.of(100));
            assertThat(actual.getAccount(john)).isPresent().get().extracting("balance").isEqualTo(MoneyAmount.ZERO);
            assertNoEvents(actual);
        }

        @DisplayName("it should rebuild transfer money event")
        @Test
        void itShouldRebuildTransferMoneyState() {
            Accounts actual = rebuild(
                    new AccountCreatedEvent(john, MoneyAmount.of(100)),
                    new AccountCreatedEvent(david, MoneyAmount.ZERO),
                    new TransferedAmountEvent(john, david, MoneyAmount.of(33)));

            assertThat(actual.getAccount(david)).isPresent().get().extracting("balance").isEqualTo(MoneyAmount.of(33));
            assertThat(actual.getAccount(john)).isPresent().get().extracting("balance").isEqualTo(MoneyAmount.of(67));
            assertNoEvents(actual);
        }

        @DisplayName("it should rebuild deposit money event")
        @Test
        void itShouldRebuildDepositMoneyState() {
            Accounts actual = rebuild(
                    new AccountCreatedEvent(david, MoneyAmount.ZERO),
                    new DepositedAmountEvent(david, MoneyAmount.of(33)));

            assertThat(actual.getAccount(david)).isPresent().get().extracting("balance").isEqualTo(MoneyAmount.of(33));
            assertNoEvents(actual);
        }

        @DisplayName("it should rebuild withdraw money event")
        @Test
        void itShouldRebuildWithdrawMoneyState() {
            Accounts actual = rebuild(
                    new AccountCreatedEvent(david, MoneyAmount.of(100)),
                    new WithdrawnAmountEvent(david, MoneyAmount.of(33)));

            assertThat(actual.getAccount(david)).isPresent().get().extracting("balance").isEqualTo(MoneyAmount.of(67));
            assertNoEvents(actual);
        }

    }

    @DisplayName("Deposit money - write model tests")
    @Nested
    class DepositMoney {
        @DisplayName("it should create Deposit event")
        @Test
        void itShouldCreateDepositMoneyEvent() {
            Account actual = rebuild(david, new AccountCreatedEvent(david, MoneyAmount.of(10))).orElseThrow();

            actual.deposit(MoneyAmount.of(30));

            Assertions.assertThat(actual.getBalance()).isEqualTo(MoneyAmount.of(40));
        }
    }

    @DisplayName("Withdraw money - write model tests")
    @Nested
    class WithdrawMoney {

        @DisplayName("it should throw exception when account balance is not sufficient")
        @Test
        void itShouldThrowExceptionWhenInsufficientFunds() {
            Account account = rebuild(david, new AccountCreatedEvent(david, MoneyAmount.of(10))).orElseThrow();

            assertThatThrownBy(() -> account.withdraw(MoneyAmount.of(30))).isInstanceOf(IllegalStateException.class)
                    .hasMessage("Insufficient money to withdraw");
        }

        @DisplayName("it should create Withdraw event")
        @Test
        void itShouldCreateDepositMoneyEvent() {
            Account actual = rebuild(david, new AccountCreatedEvent(david, MoneyAmount.of(10))).orElseThrow();

            actual.withdraw(MoneyAmount.of(3));

            Assertions.assertThat(actual.getBalance()).isEqualTo(MoneyAmount.of(7));
        }
    }

    @DisplayName("Transfer money - write model tests")
    @Nested
    class TransferTests {
        @DisplayName("it should throw exception when target account doesn't exist")
        @Test
        void itShouldThrowExceptionWhenTargetAccountDoesntExist() {
            Accounts accounts = rebuild(new AccountCreatedEvent(john, MoneyAmount.of(100)));

            assertThatThrownBy(() -> accounts.transferMoney(john, david, MoneyAmount.of(100)))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("Target account not found");
        }

        @DisplayName("it should throw exception when source account doesn't exist")
        @Test
        void itShouldThrowExceptionWhenSourceAccountDoesntExist() {
            Accounts accounts = rebuild(new AccountCreatedEvent(david, MoneyAmount.of(200)));

            assertThatThrownBy(() -> accounts.transferMoney(john, david, MoneyAmount.of(100)))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("Source account not found");
        }


        @DisplayName("it should throw exception when source account doesn't have sufficient funds for transfer")
        @Test
        void itShouldThrowExceptionWhenSourceAccountDoesntHaveSufficientFunds() {
            Accounts accounts = rebuild(new AccountCreatedEvent(john, MoneyAmount.of(10)),
                    new AccountCreatedEvent(david, MoneyAmount.ZERO));

            assertThatThrownBy(() -> accounts.transferMoney(john, david, MoneyAmount.of(100)))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("Insufficient funds in source account");
        }


        @DisplayName("it should throw exception when transfering ZERO amount")
        @Test
        void itShouldThrowExceptionWhenTransferingZeroAmount() {
            Accounts accounts = rebuild(new AccountCreatedEvent(john, MoneyAmount.of(200)),
                    new AccountCreatedEvent(david, MoneyAmount.ZERO));

            assertThatThrownBy(() -> accounts.transferMoney(john, david, MoneyAmount.ZERO))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("Cannot transfer zero money amount");
        }

        @DisplayName("it should create TransferMoneyEvent")
        @Test
        void itShouldAddMoneyToTargetAccount() {
            Accounts accounts = rebuild(new AccountCreatedEvent(john, MoneyAmount.of(200)),
                    new AccountCreatedEvent(david, MoneyAmount.ZERO));

            accounts.transferMoney(john, david, MoneyAmount.of(75));

            // TODO: do we want just single transfer event? Or transfer event + withdraw event + deposit event? Or just withdraw + deposit events?
            assertThat(accounts.getPendingEvents()).usingRecursiveComparison()
                    .ignoringFields("createdAt", "sequenceId")
                    .isEqualTo(List.of(new TransferedAmountEvent(john, david, MoneyAmount.of(75))));
        }

    }

}