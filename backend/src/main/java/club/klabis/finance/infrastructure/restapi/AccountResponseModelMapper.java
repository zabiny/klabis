package club.klabis.finance.infrastructure.restapi;

import club.klabis.finance.domain.Account;
import club.klabis.shared.config.hateoas.AbstractRepresentationModelMapper;
import club.klabis.shared.config.mapstruct.DomainToDtoMapperConfiguration;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.data.domain.Pageable;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@Mapper(config = DomainToDtoMapperConfiguration.class, componentModel = "spring")
public abstract class AccountResponseModelMapper extends AbstractRepresentationModelMapper<Account, AccountReponse> {

    @Mapping(target = "ownerId", source = "owner")
    @Override
    public abstract AccountReponse toResponse(Account event);

    @Override
    public void addLinks(EntityModel<AccountReponse> resource) {
        resource.add(linkTo(methodOn(FinanceAccountsController.class).getTransactions(resource.getContent()
                .ownerId(), Pageable.ofSize(10).first())).withRel("transactions"));
    }

    @Override
    public void addLinks(CollectionModel<EntityModel<AccountReponse>> resources) {

    }
}
