package com.klabis.finance.infrastructure.restapi;

import com.klabis.common.mvc.MvcComponent;
import com.klabis.common.ui.ModelWithDomainPostprocessor;
import com.klabis.common.users.Authority;
import com.klabis.common.users.HasAuthority;
import com.klabis.finance.application.ChargePort;
import com.klabis.finance.application.DepositPort;
import com.klabis.finance.application.ReversePort;
import com.klabis.finance.domain.TransactionId;
import com.klabis.finance.domain.MemberAccount;
import com.klabis.finance.domain.MemberAccountRepository;
import com.klabis.finance.domain.Transaction;
import com.klabis.members.ActingUser;
import com.klabis.members.MemberId;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.jmolecules.architecture.hexagonal.PrimaryAdapter;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.net.URI;
import java.time.LocalDate;
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

    MemberAccountController(DepositPort depositPort, ChargePort chargePort,
                            ReversePort reversePort,
                            MemberAccountRepository memberAccountRepository) {
        this.depositPort = depositPort;
        this.chargePort = chargePort;
        this.reversePort = reversePort;
        this.memberAccountRepository = memberAccountRepository;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public ResponseEntity<EntityModel<MemberAccountResource>> getAccount(
            @PathVariable UUID memberId,
            @ActingUser com.klabis.common.users.UserId currentUserId) {
        MemberId id = new MemberId(memberId);
        MemberAccount account = memberAccountRepository.findById(id)
                .orElseThrow(() -> new com.klabis.finance.application.MemberAccountNotFoundException(id));
        MemberAccountResource resource = MemberAccountResource.from(account);
        return ResponseEntity.ok(entityModelWithDomain(resource, account));
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

    @GetMapping("/transactions/{txId}")
    public ResponseEntity<Void> getTransaction(
            @PathVariable UUID memberId,
            @PathVariable UUID txId) {
        return ResponseEntity.ok().build();
    }

    private URI buildTransactionUri(UUID memberId, UUID txId) {
        return klabisLinkTo(methodOn(MemberAccountController.class).getTransaction(memberId, txId))
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
class MemberAccountPostprocessor extends ModelWithDomainPostprocessor<MemberAccountResource, MemberAccount> {

    @Override
    public void process(EntityModel<MemberAccountResource> model, MemberAccount account) {
        klabisLinkTo(methodOn(MemberAccountController.class).getAccount(account.getId().uuid(), null))
                .map(link -> link.withSelfRel()
                        .andAffordances(klabisAfford(
                                methodOn(MemberAccountController.class).deposit(account.getId().uuid(), null, null)))
                        .andAffordances(klabisAfford(
                                methodOn(MemberAccountController.class).charge(account.getId().uuid(), null, null))))
                .ifPresent(model::add);
    }
}
