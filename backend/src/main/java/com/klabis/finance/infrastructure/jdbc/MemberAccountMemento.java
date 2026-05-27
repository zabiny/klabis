package com.klabis.finance.infrastructure.jdbc;

import com.klabis.finance.domain.MemberAccount;
import com.klabis.finance.domain.Money;
import com.klabis.finance.domain.Transaction;
import com.klabis.members.MemberId;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.annotation.Version;
import org.springframework.data.domain.AfterDomainEventPublication;
import org.springframework.data.domain.DomainEvents;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.MappedCollection;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Currency;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Table("member_account")
class MemberAccountMemento implements Persistable<UUID> {

    @Id
    @Column("member_id")
    private UUID memberId;

    @Column("balance_amount")
    private BigDecimal balanceAmount;

    @Column("balance_currency")
    private String balanceCurrency;

    @CreatedDate
    @Column("created_at")
    private Instant createdAt;

    @Version
    @Column("version")
    private Long version;

    @MappedCollection(idColumn = "member_account_id")
    private Set<TransactionMemento> transactions = new HashSet<>();

    @Transient
    private MemberAccount account;

    @Transient
    private boolean isNew = true;

    protected MemberAccountMemento() {
    }

    static MemberAccountMemento from(MemberAccount account) {
        MemberAccountMemento memento = new MemberAccountMemento();
        memento.memberId = account.getId().uuid();
        memento.balanceAmount = account.getBalance().amount();
        memento.balanceCurrency = account.getBalance().currency().getCurrencyCode();
        memento.transactions = new HashSet<>();
        account.getTransactions().forEach(tx -> memento.transactions.add(TransactionMemento.from(tx)));
        memento.account = account;
        memento.isNew = (account.getAuditMetadata() == null);
        return memento;
    }

    MemberAccount toMemberAccount() {
        MemberId id = new MemberId(memberId);
        Money balance = Money.of(balanceAmount, Currency.getInstance(balanceCurrency));
        List<Transaction> txList = transactions == null ? List.of() :
                transactions.stream().map(TransactionMemento::toTransaction).toList();
        return MemberAccount.reconstruct(id, balance, txList);
    }

    @DomainEvents
    List<Object> getDomainEvents() {
        return account != null ? account.getDomainEvents() : List.of();
    }

    @AfterDomainEventPublication
    void clearDomainEvents() {
        if (account != null) {
            account.clearDomainEvents();
        }
    }

    @Override
    public boolean isNew() {
        return isNew;
    }

    @Override
    public UUID getId() {
        return memberId;
    }
}
