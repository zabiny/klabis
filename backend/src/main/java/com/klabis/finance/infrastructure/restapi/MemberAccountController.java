package com.klabis.finance.infrastructure.restapi;

import com.klabis.common.mvc.MvcComponent;
import com.klabis.common.ui.HalFormsSupport;
import com.klabis.common.ui.ModelWithDomainPostprocessor;
import com.klabis.common.users.Authority;
import com.klabis.common.users.HasAuthority;
import com.klabis.finance.application.DepositPort;
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
    private final MemberAccountRepository memberAccountRepository;

    MemberAccountController(DepositPort depositPort, MemberAccountRepository memberAccountRepository) {
        this.depositPort = depositPort;
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
            @Valid @RequestBody TransactionRequest request,
            @ActingUser com.klabis.common.users.UserId currentUserId) {
        DepositPort.DepositCommand command = new DepositPort.DepositCommand(
                new MemberId(memberId),
                request.amount(),
                request.occurredAt() != null ? request.occurredAt() : LocalDate.now(),
                request.note(),
                currentUserId
        );
        Transaction tx = depositPort.deposit(command);
        URI location = buildTransactionUri(memberId, tx.getId().value());
        return ResponseEntity.created(location).build();
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

    record TransactionRequest(
            @NotNull String type,
            @NotNull @Positive BigDecimal amount,
            LocalDate occurredAt,
            String note
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
                                methodOn(MemberAccountController.class).deposit(account.getId().uuid(), null, null))))
                .ifPresent(model::add);
    }
}
