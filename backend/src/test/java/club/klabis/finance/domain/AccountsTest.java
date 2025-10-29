package club.klabis.finance.domain;

import club.klabis.finance.application.TransferMoneyUseCase;
import club.klabis.finance.domain.events.AccountCreatedEvent;
import club.klabis.finance.domain.events.DepositedAmountEvent;
import club.klabis.finance.domain.events.TransferedAmountEvent;
import club.klabis.finance.domain.events.WithdrawnAmountEvent;
import club.klabis.members.MemberId;
import com.dpolach.eventsourcing.BaseEvent;
import com.dpolach.eventsourcing.EventsRepository;
import com.dpolach.eventsourcing.EventsSource;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.ListAssert;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AccountsTest {
    final MemberId david = new MemberId(1);
    final MemberId john = new MemberId(2);

    private static Optional<Account> rebuildAccount(MemberId accountOwner, BaseEvent... events) {
        return eventRepositoryWith(events).rebuild(new Accounts()).getAccount(accountOwner);
    }

    private static EventsRepository eventRepositoryWith(BaseEvent... initialEvents) {
        return new EventsRepository() {
            private final List<BaseEvent> storedEvents = new LinkedList<>(List.of(initialEvents));

            @Override
            public void appendPendingEventsFrom(EventsSource eventsSource) {
                storedEvents.addAll(eventsSource.getPendingEvents());
                eventsSource.clearPendingEvents();
            }

            @Override
            public Stream<BaseEvent> streamAllEvents() {
                return storedEvents.stream();
            }
        };
    }

    static ListAssert<BaseEvent> assertThatPendingEvents(EventsSource eventsSource) {
        return Assertions.assertThat(eventsSource.getPendingEvents());
    }

    static ListAssert<BaseEvent> assertThatStoredEvents(EventsRepository eventsRepository) {
        return Assertions.assertThat(eventsRepository.streamAllEvents());
    }

    @DisplayName("read model tests")
    @Nested
    class ReadTests {
        @DisplayName("it should rebuild CreateAccount event")
        @Test
        void itShouldRebuildCreateAccountEvent() {
            Accounts actual = eventRepositoryWith(new AccountCreatedEvent(david, MoneyAmount.of(100)),
                    new AccountCreatedEvent(john, MoneyAmount.ZERO)).rebuild(new Accounts());

            assertThat(actual.getAccount(david)).isPresent().get().extracting("balance").isEqualTo(MoneyAmount.of(100));
            assertThat(actual.getAccount(john)).isPresent().get().extracting("balance").isEqualTo(MoneyAmount.ZERO);
            assertThatPendingEvents(actual).isEmpty();
        }

        @DisplayName("it should rebuild transfer money event")
        @Test
        void itShouldRebuildTransferMoneyState() {
            Accounts actual = eventRepositoryWith(
                    new AccountCreatedEvent(john, MoneyAmount.of(100)),
                    new AccountCreatedEvent(david, MoneyAmount.ZERO),
                    new TransferedAmountEvent(john, david, MoneyAmount.of(33))).rebuild(new Accounts());

            assertThat(actual.getAccount(david)).isPresent().get().extracting("balance").isEqualTo(MoneyAmount.of(33));
            assertThat(actual.getAccount(john)).isPresent().get().extracting("balance").isEqualTo(MoneyAmount.of(67));
            assertThatPendingEvents(actual).isEmpty();
        }

        @DisplayName("it should rebuild deposit money event")
        @Test
        void itShouldRebuildDepositMoneyState() {
            Accounts actual = eventRepositoryWith(
                    new AccountCreatedEvent(david, MoneyAmount.ZERO),
                    new DepositedAmountEvent(david, MoneyAmount.of(33))).rebuild(new Accounts());

            assertThat(actual.getAccount(david)).isPresent().get().extracting("balance").isEqualTo(MoneyAmount.of(33));
            assertThatPendingEvents(actual).isEmpty();
        }

        @DisplayName("it should rebuild withdraw money event")
        @Test
        void itShouldRebuildWithdrawMoneyState() {
            Accounts actual = eventRepositoryWith(
                    new AccountCreatedEvent(david, MoneyAmount.of(100)),
                    new WithdrawnAmountEvent(david, MoneyAmount.of(33))).rebuild(new Accounts());

            assertThat(actual.getAccount(david)).isPresent().get().extracting("balance").isEqualTo(MoneyAmount.of(67));
            assertThatPendingEvents(actual).isEmpty();
        }

    }

    @DisplayName("Deposit money - write model tests")
    @Nested
    class DepositMoney {
        @DisplayName("it should create Deposit event")
        @Test
        void itShouldCreateDepositMoneyEvent() {
            Account actual = rebuildAccount(david,
                    new AccountCreatedEvent(david, MoneyAmount.of(10))).orElseThrow();

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
            Account account = rebuildAccount(david,
                    new AccountCreatedEvent(david, MoneyAmount.of(10))).orElseThrow();

            assertThatThrownBy(() -> account.withdraw(MoneyAmount.of(30))).isInstanceOf(IllegalStateException.class)
                    .hasMessage("Insufficient money to withdraw");
        }

        @DisplayName("it should create Withdraw event")
        @Test
        void itShouldCreateDepositMoneyEvent() {
            Account actual = rebuildAccount(david,
                    new AccountCreatedEvent(david, MoneyAmount.of(10))).orElseThrow();

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
            TransferMoneyUseCase useCase = new TransferMoneyUseCase(eventRepositoryWith(new AccountCreatedEvent(john,
                    MoneyAmount.of(100))));

            assertThatThrownBy(() -> useCase.transferMoney(john, david, MoneyAmount.of(100)))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("Target account not found");
        }

        @DisplayName("it should throw exception when source account doesn't exist")
        @Test
        void itShouldThrowExceptionWhenSourceAccountDoesntExist() {
            TransferMoneyUseCase useCase = new TransferMoneyUseCase(eventRepositoryWith(new AccountCreatedEvent(david,
                    MoneyAmount.of(200))));

            assertThatThrownBy(() -> useCase.transferMoney(john, david, MoneyAmount.of(100)))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("Source account not found");
        }


        @DisplayName("it should throw exception when source account doesn't have sufficient funds for transfer")
        @Test
        void itShouldThrowExceptionWhenSourceAccountDoesntHaveSufficientFunds() {
            Account account = rebuildAccount(john, new AccountCreatedEvent(john, MoneyAmount.of(10))).orElseThrow();

            assertThatThrownBy(() -> account.transferTo(david, MoneyAmount.of(100)))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("Insufficient funds on source account");
        }


        @DisplayName("it should throw exception when transfering ZERO amount")
        @Test
        void itShouldThrowExceptionWhenTransferingZeroAmount() {
            Account account = rebuildAccount(john, new AccountCreatedEvent(john, MoneyAmount.of(100))).orElseThrow();

            assertThatThrownBy(() -> account.transferTo(david, MoneyAmount.ZERO))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("Cannot transfer zero money amount");
        }

        @DisplayName("it should create TransferMoneyEvent")
        @Test
        void itShouldCreateTransferMoneyEvent() {
            Account account = rebuildAccount(john, new AccountCreatedEvent(john, MoneyAmount.of(100))).orElseThrow();

            account.transferTo(david, MoneyAmount.of(75));

            assertThatPendingEvents(account)
                    .usingRecursiveComparison()
                    .ignoringFields("createdAt", "sequenceId")
                    .isEqualTo(List.of(new TransferedAmountEvent(john, david, MoneyAmount.of(75))));
        }

    }

}