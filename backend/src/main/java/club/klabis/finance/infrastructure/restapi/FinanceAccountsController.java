package club.klabis.finance.infrastructure.restapi;

import club.klabis.finance.application.AccountNotFoundException;
import club.klabis.finance.application.AccountsService;
import club.klabis.finance.application.DepositAction;
import club.klabis.finance.domain.Account;
import club.klabis.finance.domain.TransactionHistory;
import club.klabis.members.MemberId;
import club.klabis.members.infrastructure.restapi.dto.MembersApiResponse;
import club.klabis.shared.config.hateoas.HalResourceAssembler;
import club.klabis.shared.config.hateoas.ModelAssembler;
import club.klabis.shared.config.hateoas.ModelPreparator;
import club.klabis.shared.config.restapi.ApiController;
import club.klabis.shared.config.security.ApplicationGrant;
import club.klabis.shared.config.security.HasGrant;
import club.klabis.shared.config.security.HasMemberGrant;
import org.springdoc.core.converters.models.PageableAsQueryParam;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.server.ExposesResourceFor;
import org.springframework.hateoas.server.RepresentationModelProcessor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

import static club.klabis.shared.config.hateoas.forms.KlabisHateoasImprovements.linkIfAuthorized;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@ExposesResourceFor(Account.class)
@ApiController(path = "/member/{memberId}/finance-account", openApiTagName = "Finance", securityScopes = {"openapi"})
public class FinanceAccountsController {

    private final AccountsService accountsService;
    private final ModelAssembler<Account, AccountReponse> accountReponseMapper;
    private final ModelAssembler<TransactionHistory.TransactionItem, TransactionItemResponse> transactionItemResponseMapper;

    public FinanceAccountsController(AccountsService accountsService, ModelPreparator<Account, AccountReponse> accountReponseMapper, PagedResourcesAssembler<Account> pagedAssembler, PagedResourcesAssembler<TransactionHistory.TransactionItem> historyItemPaged) {
        this.accountsService = accountsService;
        this.accountReponseMapper = new HalResourceAssembler<>(accountReponseMapper, pagedAssembler);
        this.transactionItemResponseMapper = new HalResourceAssembler<>(new TransactionHistoryModelPreparator(),
                historyItemPaged);
    }

    @GetMapping
    @HasMemberGrant(memberId = "#memberId")
    public ResponseEntity<EntityModel<AccountReponse>> getAccount(@PathVariable("memberId") MemberId memberId) {
        return accountsService.getAccountForMember(memberId)
                .map(accountReponseMapper::toEntityResponse)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new AccountNotFoundException(memberId));
    }

    @GetMapping(path = "/transactions")
    @HasMemberGrant(memberId = "#memberId")
    @PageableAsQueryParam
    public CollectionModel<EntityModel<TransactionItemResponse>> getTransactions(@PathVariable("memberId") MemberId memberId) {
        return transactionItemResponseMapper.toCollectionModel(accountsService.getTransactionHistory(
                        memberId), TransactionItemResponse.class)
                .add(linkTo(methodOn(FinanceAccountsController.class).getTransactions(memberId)).withSelfRel());
    }

    @PutMapping(path = "/deposit")
    @HasGrant(ApplicationGrant.DEPOSIT_FINANCE)
    public ResponseEntity<Void> deposit(@PathVariable("memberId") MemberId memberId, @RequestBody DepositAction form) {
        accountsService.deposit(memberId, form);

        return ResponseEntity.ok().build();
    }

}

@Component
class TransactionHistoryModelPreparator implements ModelPreparator<TransactionHistory.TransactionItem, TransactionItemResponse> {

    @Override
    public TransactionItemResponse toResponseDto(TransactionHistory.TransactionItem transactionItem) {
        return new TransactionItemResponse(transactionItem.date(),
                transactionItem.amount().amount(),
                transactionItem.note());
    }

}

@Component
class MemberItemPostprocessor implements RepresentationModelProcessor<EntityModel<MembersApiResponse>> {

    @Override
    public EntityModel<MembersApiResponse> process(EntityModel<MembersApiResponse> model) {
        linkIfAuthorized(methodOn(FinanceAccountsController.class).getAccount(model.getContent().id()))
                .map(link -> link.withRel("finances"))
                .ifPresent(model::add);

        return model;
    }
}