package com.klabis.groups.traininggroup.infrastructure.restapi;

import com.klabis.common.mvc.MvcComponent;
import com.klabis.groups.common.domain.TrainingGroupFilter;
import com.klabis.groups.traininggroup.domain.TrainingGroupRepository;
import com.klabis.members.MemberId;
import com.klabis.members.MemberResource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.server.RepresentationModelProcessor;

import static com.klabis.common.ui.HalFormsSupport.klabisLinkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@MvcComponent
public class MemberTrainingGroupLinkProcessor implements RepresentationModelProcessor<EntityModel<MemberResource>> {

    private final TrainingGroupRepository trainingGroupRepository;

    @Autowired
    MemberTrainingGroupLinkProcessor(@Lazy TrainingGroupRepository trainingGroupRepository) {
        this.trainingGroupRepository = trainingGroupRepository;
    }

    @Override
    public EntityModel<MemberResource> process(EntityModel<MemberResource> model) {
        MemberId memberId = model.getContent().memberId();
        trainingGroupRepository.findOne(TrainingGroupFilter.all().withMemberIs(memberId))
                .ifPresent(group -> klabisLinkTo(methodOn(TrainingGroupController.class).getTrainingGroup(group.getId().uuid(), null))
                        .map(link -> link.withRel("trainingGroup"))
                        .ifPresent(model::add));
        return model;
    }
}
