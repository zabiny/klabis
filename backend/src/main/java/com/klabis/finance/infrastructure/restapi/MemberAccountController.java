package com.klabis.finance.infrastructure.restapi;

import com.klabis.common.mvc.MvcComponent;
import com.klabis.common.ui.ModelWithDomainPostprocessor;
import com.klabis.common.users.Authority;
import com.klabis.common.users.HasAuthority;
import com.klabis.finance.application.ChargePort;
import com.klabis.finance.application.DepositPort;
import com.klabis.finance.application.MemberAccountNotFoundException;
import com.klabis.finance.application.ReversePort;
import com.klabis.finance.application.TransactionNotFoundException;
import com.klabis.finance.application.TransactionQueryPort;
import com.klabis.finance.domain.MemberAccount;
import com.klabis.finance.domain.MemberAccountRepository;
import com.klabis.finance.domain.Money;
import com.klabis.finance.domain.Transaction;
import com.klabis.finance.domain.TransactionId;
import com.klabis.finance.domain.TransactionType;
import com.klabis.members.ActingUser;
import com.klabis.members.CurrentUserData;
import com.klabis.members.MemberId;
import com.klabis.members.infrastructure.restapi.MemberController;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.jmolecules.architecture.hexagonal.PrimaryAdapter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.net.URI;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static com.klabis.common.ui.HalFormsSupport.entityModelWithDomain;
import static com.klabis.common.ui.HalFormsSupport.klabisAfford;
import static com.klabis.common.ui.HalFormsSupport.klabisLinkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@PrimaryAdapter
@RestController
@RequestMapping(value = "/api/members/{memberId}/account", produces = MediaTypes.HAL_FORMS_JSON_VALUE)
@Tag(name = "Finance", description = "Member financial account API")
class MemberAccountController {

    private final DepositPort depositPort;
    private final ChargePort chargePort;
    private final ReversePort reversePort;
    private final MemberAccountRepository memberAccountRepository;
    private final TransactionQueryPort transactionQueryPort;
    private final PagedResourcesAssembler<Transaction> pagedResourcesAssembler;

    MemberAccountController(DepositPort depositPort, ChargePort chargePort,
                            ReversePort reversePort,
                            MemberAccountRepository memberAccountRepository,
                            TransactionQueryPort transactionQueryPort,
                            PagedResourcesAssembler<Transaction> pagedResourcesAssembler) {
        this.depositPort = depositPort;
        this.chargePort = chargePort;
        this.reversePort = reversePort;
        this.memberAccountRepository = memberAccountRepository;
        this.transactionQueryPort = transactionQueryPort;
        this.pagedResourcesAssembler = pagedResourcesAssembler;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public ResponseEntity<EntityModel<MemberAccountResource>> getAccount(
            @PathVariable UUID memberId,
            @ActingUser CurrentUserData currentUser) {
        MemberId id = new MemberId(memberId);
        checkAccountAccess(id, currentUser);
        Money balance = memberAccountRepository.findBalanceById(id)
                .orElseThrow(() -> new MemberAccountNotFoundException(id));
        MemberAccountResource resource = MemberAccountResource.fromBalance(id, balance);
        return ResponseEntity.ok(entityModelWithDomain(resource, id));
    }

    @GetMapping("/transactions")
    @Transactional(readOnly = true)
    public ResponseEntity<PagedModel<EntityModel<TransactionResource>>> listTransactions(
            @PathVariable UUID memberId,
            @ActingUser CurrentUserData currentUser,
            @RequestParam(required = false) LocalDate occurredAtFrom,
            @RequestParam(required = false) LocalDate occurredAtTo,
            @RequestParam(required = false) TransactionType type,
            @PageableDefault(size = 20, sort = "occurredAt", direction = Sort.Direction.DESC)
            Pageable pageable) {
        MemberId id = new MemberId(memberId);
        checkAccountAccess(id, currentUser);

        Page<Transaction> page = transactionQueryPort.findTransactions(
                new TransactionQueryPort.TransactionQuery(id, occurredAtFrom, occurredAtTo, type, pageable));

        List<TransactionId> pageIds = page.getContent().stream().map(Transaction::getId).toList();
        Map<TransactionId, TransactionId> reversalsByOriginal = memberAccountRepository.findReversalsOf(pageIds);
        boolean canReverse = currentUser.hasAuthority(Authority.FINANCE_MANAGE);
        Optional<Link> accountLink = FinanceLinks.accountLink(memberId);

        PagedModel<EntityModel<TransactionResource>> model = pagedResourcesAssembler.toModel(
                page,
                tx -> {
                    EntityModel<TransactionResource> item = EntityModel.of(TransactionResource.from(tx));
                    TransactionId reversalId = reversalsByOriginal.get(tx.getId());
                    UUID reversedByTxId = reversalId != null ? reversalId.value() : null;
                    addTransactionLinks(item, memberId, tx, reversedByTxId, canReverse, accountLink);
                    return item;
                });
        return ResponseEntity.ok(model);
    }

    @GetMapping("/transactions/{txId}")
    @Transactional(readOnly = true)
    public ResponseEntity<EntityModel<TransactionResource>> getTransaction(
            @PathVariable UUID memberId,
            @PathVariable UUID txId,
            @ActingUser CurrentUserData currentUser) {
        MemberId id = new MemberId(memberId);
        checkAccountAccess(id, currentUser);
        MemberAccount account = memberAccountRepository.findById(id)
                .orElseThrow(() -> new MemberAccountNotFoundException(id));
        Transaction tx = account.getTransactions().stream()
                .filter(t -> t.getId().value().equals(txId))
                .findFirst()
                .orElseThrow(() -> new TransactionNotFoundException(txId));

        Transaction reversal = memberAccountRepository.findReversalOf(tx.getId()).orElse(null);
        UUID reversedByTxId = reversal != null ? reversal.getId().value() : null;

        EntityModel<TransactionResource> model = EntityModel.of(TransactionResource.from(tx));
        addTransactionLinks(model, memberId, tx, reversedByTxId,
                currentUser.hasAuthority(Authority.FINANCE_MANAGE),
                FinanceLinks.accountLink(memberId));
        return ResponseEntity.ok(model);
    }

    @PostMapping("/transactions")
    @HasAuthority(Authority.FINANCE_MANAGE)
    public ResponseEntity<Void> deposit(
            @PathVariable UUID memberId,
            @Valid @RequestBody DepositRequest request,
            @ActingUser com.klabis.common.users.UserId currentUserId) {
        LocalDate occurredAt = request.occurredAt() != null ? request.occurredAt() : LocalDate.now();
        Transaction tx = depositPort.deposit(new DepositPort.DepositCommand(
                new MemberId(memberId), request.amount(), occurredAt, request.note(), currentUserId));
        return ResponseEntity.created(buildTransactionUri(memberId, tx.getId().value())).build();
    }

    @PostMapping("/transactions/charge")
    @HasAuthority(Authority.FINANCE_MANAGE)
    public ResponseEntity<Void> charge(
            @PathVariable UUID memberId,
            @Valid @RequestBody ChargeRequest request,
            @ActingUser com.klabis.common.users.UserId currentUserId) {
        LocalDate occurredAt = request.occurredAt() != null ? request.occurredAt() : LocalDate.now();
        Transaction tx = chargePort.charge(new ChargePort.ChargeCommand(
                new MemberId(memberId), request.amount(), occurredAt, request.note(), currentUserId));
        return ResponseEntity.created(buildTransactionUri(memberId, tx.getId().value())).build();
    }

    @PostMapping("/transactions/{txId}/reverse")
    @HasAuthority(Authority.FINANCE_MANAGE)
    public ResponseEntity<Void> reverse(
            @PathVariable UUID memberId,
            @PathVariable UUID txId,
            @Valid @RequestBody ReverseRequest request,
            @ActingUser com.klabis.common.users.UserId currentUserId) {
        LocalDate occurredAt = request.occurredAt() != null ? request.occurredAt() : LocalDate.now();
        Transaction tx = reversePort.reverse(new ReversePort.ReverseCommand(
                new MemberId(memberId), new TransactionId(txId), request.note(), occurredAt, currentUserId));
        return ResponseEntity.created(buildTransactionUri(memberId, tx.getId().value())).build();
    }

    private void checkAccountAccess(MemberId memberId, CurrentUserData currentUser) {
        boolean isOwner = currentUser.memberId() != null && currentUser.memberId().equals(memberId);
        boolean hasFinanceManage = currentUser.hasAuthority(Authority.FINANCE_MANAGE);
        if (!isOwner && !hasFinanceManage) {
            throw new AccessDeniedException(
                    "Access denied: not account owner and missing FINANCE:MANAGE authority");
        }
    }

    private void addTransactionLinks(EntityModel<TransactionResource> model, UUID memberId,
                                     Transaction tx, UUID reversedByTxId,
                                     boolean canReverse, Optional<Link> accountLink) {
        UUID txId = tx.getId().value();
        klabisLinkTo(methodOn(MemberAccountController.class).getTransaction(memberId, txId, null))
                .map(link -> {
                    if (reversedByTxId == null && !tx.isReversal() && canReverse) {
                        return link.withSelfRel()
                                .andAffordances(klabisAfford(
                                        methodOn(MemberAccountController.class).reverse(memberId, txId, null, null)));
                    }
                    return link.withSelfRel();
                })
                .ifPresent(model::add);

        if (reversedByTxId != null) {
            klabisLinkTo(methodOn(MemberAccountController.class).getTransaction(memberId, reversedByTxId, null))
                    .ifPresent(link -> model.add(link.withRel("reversedBy")));
        }

        if (tx.isReversal()) {
            UUID originalTxId = tx.getReversesTransactionId().value();
            klabisLinkTo(methodOn(MemberAccountController.class).getTransaction(memberId, originalTxId, null))
                    .ifPresent(link -> model.add(link.withRel("reverses")));
        }

        klabisLinkTo(methodOn(MemberController.class).getMember(tx.getRecordedBy().uuid(), null))
                .ifPresent(link -> model.add(link.withRel("recordedBy")));

        accountLink.ifPresent(model::add);
    }

    private URI buildTransactionUri(UUID memberId, UUID txId) {
        return klabisLinkTo(methodOn(MemberAccountController.class).getTransaction(memberId, txId, null))
                .map(link -> link.toUri())
                .orElseGet(() -> URI.create("/api/members/" + memberId + "/account/transactions/" + txId));
    }

    record DepositRequest(
            @NotNull @Positive BigDecimal amount,
            LocalDate occurredAt,
            String note
    ) {
    }

    record ChargeRequest(
            @NotNull @Positive BigDecimal amount,
            LocalDate occurredAt,
            String note
    ) {
    }

    record ReverseRequest(
            String note,
            LocalDate occurredAt
    ) {
    }
}

@MvcComponent
class MemberAccountPostprocessor extends ModelWithDomainPostprocessor<MemberAccountResource, MemberId> {

    @Override
    public void process(EntityModel<MemberAccountResource> model, MemberId memberId) {
        klabisLinkTo(methodOn(MemberAccountController.class).getAccount(memberId.uuid(), null))
                .map(link -> link.withSelfRel()
                        .andAffordances(klabisAfford(
                                methodOn(MemberAccountController.class).deposit(memberId.uuid(), null, null)))
                        .andAffordances(klabisAfford(
                                methodOn(MemberAccountController.class).charge(memberId.uuid(), null, null))))
                .ifPresent(model::add);

        klabisLinkTo(methodOn(MemberAccountController.class).listTransactions(
                memberId.uuid(), null, null, null, null, Pageable.unpaged()))
                .ifPresent(link -> model.add(link.withRel("transactions")));

        klabisLinkTo(methodOn(MemberController.class).getMember(memberId.uuid(), null))
                .ifPresent(link -> model.add(link.withRel("accountOwner")));
    }
}
