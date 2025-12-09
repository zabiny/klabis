package club.klabis.finance.infrastructure.restapi;

import club.klabis.finance.domain.Account;
import club.klabis.shared.config.hateoas.ModelPreparator;
import club.klabis.shared.config.mapstruct.DomainToDtoMapperConfiguration;
import club.klabis.shared.config.security.ApplicationGrant;
import club.klabis.shared.config.security.KlabisSecurityService;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.IanaLinkRelations;

import static club.klabis.shared.config.hateoas.forms.KlabisHateoasImprovements.affordBetter;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@Mapper(config = DomainToDtoMapperConfiguration.class, componentModel = "spring", uses = KlabisSecurityService.class)
public abstract class AccountResponseModelMapper implements ModelPreparator<Account, AccountReponse> {

    @Autowired
    private KlabisSecurityService klabisSecurityService;

    @Mapping(target = "ownerId", source = "owner")
    @Override
    public abstract AccountReponse toResponseDto(Account account);


    @Override
    public void addLinks(EntityModel<AccountReponse> resource, Account account) {
        resource.add(linkTo(methodOn(FinanceAccountsController.class).getAccount(account.getOwner())).withSelfRel());

        resource.add(linkTo(methodOn(FinanceAccountsController.class).getTransactions(resource.getContent()
                .ownerId())).withRel("transactions"));

        if (klabisSecurityService.hasGrant(ApplicationGrant.DEPOSIT_FINANCE)) {
            resource.mapLink(IanaLinkRelations.SELF,
                    link -> link.andAffordance(affordBetter(methodOn(FinanceAccountsController.class).deposit(account.getOwner(),
                            null))));
        }
    }

}
