package club.klabis.finance.infrastructure.restapi;

import club.klabis.finance.domain.Account;
import club.klabis.shared.config.hateoas.ModelPreparator;
import club.klabis.shared.config.mapstruct.DomainToDtoMapperConfiguration;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.data.domain.Pageable;
import org.springframework.hateoas.EntityModel;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@Mapper(config = DomainToDtoMapperConfiguration.class, componentModel = "spring")
public abstract class AccountResponseModelMapper implements ModelPreparator<Account, AccountReponse> {

    @Mapping(target = "ownerId", source = "owner")
    @Override
    public abstract AccountReponse toResponseDto(Account account);


    @Override
    public void addLinks(EntityModel<AccountReponse> resource, Account account) {
        resource.add(linkTo(methodOn(FinanceAccountsController.class).getTransactions(resource.getContent()
                .ownerId(), Pageable.ofSize(10).first())).withRel("transactions"));
    }
}
