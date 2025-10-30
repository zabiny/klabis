package club.klabis.finance.infrastructure.restapi;

import club.klabis.finance.domain.Account;
import club.klabis.finance.domain.Accounts;
import club.klabis.members.MemberId;
import club.klabis.shared.config.hateoas.AbstractRepresentationModelMapper;
import club.klabis.shared.config.restapi.ApiController;
import club.klabis.shared.config.security.HasMemberGrant;
import com.dpolach.eventsourcing.EventsRepository;
import org.springdoc.core.converters.models.PageableAsQueryParam;
import org.springframework.core.ResolvableType;
import org.springframework.data.domain.Pageable;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@ApiController(openApiTagName = "Finance", securityScopes = {"openapi"})
public class FinanceAccountsController {

    private final EventsRepository eventsRepository;
    private final AbstractRepresentationModelMapper<Account, AccountReponse> accountReponseMapper;

    public FinanceAccountsController(EventsRepository eventsRepository, AbstractRepresentationModelMapper<Account, AccountReponse> accountReponseMapper) {
        this.eventsRepository = eventsRepository;
        this.accountReponseMapper = accountReponseMapper;
    }

    @GetMapping(path = "/finance/{accountId}")
    @HasMemberGrant(memberId = "#accountId")
    public ResponseEntity<EntityModel<AccountReponse>> getAccount(@PathVariable("accountId") MemberId accountId) {
        Accounts accounts = eventsRepository.rebuild(new Accounts());

        return accounts.getAccount(accountId)
                .map(accountReponseMapper::toResponseModel)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new AccountNotFoundException(accountId));
    }

    @GetMapping(path = "/finance/{accountId}/transactions")
    @HasMemberGrant(memberId = "#accountId")
    @PageableAsQueryParam
    public ResponseEntity<PagedModel<TransactionItemResponse>> getTransactions(@PathVariable("accountId") MemberId accountId, Pageable page) {
        return ResponseEntity.ok(PagedModel.empty(ResolvableType.forType(TransactionItemResponse.class)));
    }

    private class AccountNotFoundException extends RuntimeException {
        public AccountNotFoundException(MemberId memberId) {
            super(String.format("Account with id %s not found", memberId));
        }
    }
}
