package com.klabis.finance.infrastructure.restapi;

import com.klabis.common.mvc.MvcComponent;
import com.klabis.common.ui.HalResponseContext;
import com.klabis.common.ui.ModelWithDomainPostprocessor;
import com.klabis.finance.application.ChargePort;
import com.klabis.finance.application.DepositPort;
import com.klabis.finance.application.MemberAccountNotFoundException;
import com.klabis.finance.application.ReversePort;
import com.klabis.finance.application.TransactionQueryPort;
import com.klabis.finance.application.TransactionWithReversal;
import com.klabis.finance.domain.MemberAccountRepository;
import com.klabis.finance.domain.Money;
import com.klabis.finance.domain.Transaction;
import com.klabis.finance.domain.TransactionId;
import com.klabis.finance.domain.TransactionType;
import com.klabis.members.ActingUser;
import com.klabis.members.CurrentUserData;
import com.klabis.members.MemberId;
import com.klabis.members.infrastructure.restapi.MembersApi;
import org.jmolecules.architecture.hexagonal.PrimaryAdapter;
import org.springframework.core.convert.ConversionService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static com.klabis.common.ui.HalFormsSupport.klabisAfford;
import static com.klabis.common.ui.HalFormsSupport.klabisLinkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@PrimaryAdapter
@RestController
@RequestMapping(produces = MediaTypes.HAL_FORMS_JSON_VALUE)
class MemberAccountController implements FinanceApi {

    private final DepositPort depositPort;
    private final ChargePort chargePort;
    private final ReversePort reversePort;
    private final MemberAccountRepository memberAccountRepository;
    private final TransactionQueryPort transactionQueryPort;
    private final ConversionService conversionService;

    MemberAccountController(DepositPort depositPort, ChargePort chargePort,
                            ReversePort reversePort,
                            MemberAccountRepository memberAccountRepository,
                            TransactionQueryPort transactionQueryPort,
                            ConversionService conversionService) {
        this.depositPort = depositPort;
        this.chargePort = chargePort;
        this.reversePort = reversePort;
        this.memberAccountRepository = memberAccountRepository;
        this.transactionQueryPort = transactionQueryPort;
        this.conversionService = conversionService;
    }

    @Transactional(readOnly = true)
    @Override
    public ResponseEntity<MemberAccountResource> getAccount(
            UUID memberId,
            @ActingUser CurrentUserData currentUser) {
        MemberId id = new MemberId(memberId);
        Money balance = memberAccountRepository.findBalanceById(id)
                .orElseThrow(() -> new MemberAccountNotFoundException(id));
        MemberAccountResource resource = conversionService.convert(
                new MemberAccountResourceConverter.MemberBalance(id, balance), MemberAccountResource.class);
        HalResponseContext.setDomain(id);
        return ResponseEntity.ok(resource);
    }

    @Transactional(readOnly = true)
    @Override
    public ResponseEntity<Page<TransactionResource>> listTransactions(
            UUID memberId,
            LocalDate occurredAtFrom,
            LocalDate occurredAtTo,
            String type,
            Pageable pageable,
            @ActingUser CurrentUserData currentUser) {
        MemberId id = new MemberId(memberId);
        TransactionType typeFilter = type != null ? TransactionType.valueOf(type) : null;

        Page<TransactionWithReversal> page = transactionQueryPort.findTransactionsWithReversals(
                new TransactionQueryPort.TransactionQuery(id, occurredAtFrom, occurredAtTo, typeFilter, pageable));

        HalResponseContext.setDomainList(page.getContent().stream()
                .map(twr -> new AccountTransaction(id, twr))
                .toList());
        return ResponseEntity.ok(page.map(twr -> conversionService.convert(twr.transaction(), TransactionResource.class)));
    }

    @Transactional(readOnly = true)
    @Override
    public ResponseEntity<TransactionResource> getTransaction(
            UUID memberId,
            UUID txId,
            @ActingUser CurrentUserData currentUser) {
        MemberId id = new MemberId(memberId);
        Transaction tx = transactionQueryPort.findTransaction(id, new TransactionId(txId));

        TransactionWithReversal twr = memberAccountRepository.findReversalOf(tx.getId())
                .map(reversal -> TransactionWithReversal.withReversal(tx, reversal.getId()))
                .orElseGet(() -> TransactionWithReversal.withoutReversal(tx));

        HalResponseContext.setDomain(new AccountTransaction(id, twr));
        return ResponseEntity.ok(conversionService.convert(tx, TransactionResource.class));
    }

    @Override
    public ResponseEntity<Void> deposit(
            UUID memberId,
            DepositRequest request,
            @ActingUser com.klabis.common.users.UserId currentUserId) {
        LocalDate occurredAt = request.occurredAt() != null ? request.occurredAt() : LocalDate.now();
        Transaction tx = depositPort.deposit(new DepositPort.DepositCommand(
                new MemberId(memberId), request.amount(), occurredAt, request.note(), currentUserId));
        return ResponseEntity.created(buildTransactionUri(memberId, tx.getId().value())).build();
    }

    @Override
    public ResponseEntity<Void> charge(
            UUID memberId,
            ChargeRequest request,
            @ActingUser com.klabis.common.users.UserId currentUserId) {
        LocalDate occurredAt = request.occurredAt() != null ? request.occurredAt() : LocalDate.now();
        Transaction tx = chargePort.charge(new ChargePort.ChargeCommand(
                new MemberId(memberId), request.amount(), occurredAt, request.note(), currentUserId));
        return ResponseEntity.created(buildTransactionUri(memberId, tx.getId().value())).build();
    }

    @Override
    public ResponseEntity<Void> reverse(
            UUID memberId,
            UUID txId,
            ReverseRequest request,
            @ActingUser com.klabis.common.users.UserId currentUserId) {
        LocalDate occurredAt = request.occurredAt() != null ? request.occurredAt() : LocalDate.now();
        Transaction tx = reversePort.reverse(new ReversePort.ReverseCommand(
                new MemberId(memberId), new TransactionId(txId), request.note(), occurredAt, currentUserId));
        return ResponseEntity.created(buildTransactionUri(memberId, tx.getId().value())).build();
    }

    private static URI buildTransactionUri(UUID memberId, UUID txId) {
        return klabisLinkTo(methodOn(FinanceApi.class).getTransaction(memberId, txId, null))
                .map(link -> link.toUri())
                .orElseGet(() -> URI.create("/api/members/" + memberId + "/account/transactions/" + txId));
    }
}

/**
 * Transaction.class carries no memberId of its own — the path parameter is the only source of
 * truth for which account it belongs to — so the postprocessor needs both paired together.
 */
record AccountTransaction(MemberId memberId, TransactionWithReversal transactionWithReversal) {
}

@MvcComponent
class MemberAccountPostprocessor extends ModelWithDomainPostprocessor<MemberAccountResource, MemberId> {

    @Override
    public void process(EntityModel<MemberAccountResource> model, MemberId memberId) {
        klabisLinkTo(methodOn(FinanceApi.class).getAccount(memberId.uuid(), null))
                .map(link -> link.withSelfRel()
                        .andAffordances(klabisAfford(
                                methodOn(FinanceApi.class).deposit(memberId.uuid(), null, null)))
                        .andAffordances(klabisAfford(
                                methodOn(FinanceApi.class).charge(memberId.uuid(), null, null))))
                .ifPresent(model::add);

        klabisLinkTo(methodOn(FinanceApi.class).listTransactions(
                memberId.uuid(), null, null, null, Pageable.unpaged(), null))
                .ifPresent(link -> model.add(link.withRel("transactions")));

        klabisLinkTo(methodOn(MembersApi.class).getMember(memberId.uuid(), null))
                .ifPresent(link -> model.add(link.withRel("accountOwner")));
    }
}

@MvcComponent
class TransactionPostprocessor extends ModelWithDomainPostprocessor<TransactionResource, AccountTransaction> {

    @Override
    public void process(EntityModel<TransactionResource> model, AccountTransaction accountTransaction) {
        TransactionWithReversal twr = accountTransaction.transactionWithReversal();
        Transaction tx = twr.transaction();
        UUID memberId = accountTransaction.memberId().uuid();
        UUID txId = tx.getId().value();
        boolean canReverse = FinanceSecurityHelper.callerHasFinanceManage();
        Optional<UUID> reversedByTxId = twr.reversedBy().map(TransactionId::value);

        klabisLinkTo(methodOn(FinanceApi.class).getTransaction(memberId, txId, null))
                .map(link -> {
                    if (reversedByTxId.isEmpty() && !tx.isReversal() && canReverse) {
                        return link.withSelfRel()
                                .andAffordances(klabisAfford(
                                        methodOn(FinanceApi.class).reverse(memberId, txId, null, null)));
                    }
                    return link.withSelfRel();
                })
                .ifPresent(model::add);

        reversedByTxId.ifPresent(id ->
                klabisLinkTo(methodOn(FinanceApi.class).getTransaction(memberId, id, null))
                        .ifPresent(link -> model.add(link.withRel("reversedBy"))));

        if (tx.isReversal()) {
            UUID originalTxId = tx.getReversesTransactionId().value();
            klabisLinkTo(methodOn(FinanceApi.class).getTransaction(memberId, originalTxId, null))
                    .ifPresent(link -> model.add(link.withRel("reverses")));
        }

        klabisLinkTo(methodOn(MembersApi.class).getMember(tx.getRecordedBy().uuid(), null))
                .ifPresent(link -> model.add(link.withRel("recordedBy")));

        FinanceLinks.accountLink(memberId).ifPresent(model::add);
    }
}
