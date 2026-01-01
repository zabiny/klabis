package club.klabis.finance.infrastructure.restapi;

import club.klabis.members.MemberId;
import club.klabis.shared.config.hateoas.RootModel;
import club.klabis.shared.config.restapi.KlabisPrincipal;
import org.springframework.core.annotation.Order;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.server.RepresentationModelProcessor;
import org.springframework.stereotype.Component;

import java.util.Optional;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@Component
@Order(3)
class FinanceRootPostprocessor implements RepresentationModelProcessor<EntityModel<RootModel>> {
    Optional<MemberId> getMemberId(EntityModel<RootModel> rootModel) {
        return Optional.ofNullable(rootModel.getContent().klabisPrincipal()).map(KlabisPrincipal::memberId);
    }

    @Override
    public EntityModel<RootModel> process(EntityModel<RootModel> model) {
        getMemberId(model).ifPresent(memberId -> {
            model.add(linkTo(methodOn(FinanceAccountsController.class).getAccount(memberId)).withRel("finance"));
        });

        return model;
    }

}
