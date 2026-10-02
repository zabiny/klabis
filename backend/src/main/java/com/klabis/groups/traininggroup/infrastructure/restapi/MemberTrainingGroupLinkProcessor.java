package com.klabis.groups.traininggroup.infrastructure.restapi;

import com.klabis.common.mvc.MvcComponent;
import com.klabis.groups.infrastructure.restapi.TrainingGroupsApi;
import com.klabis.groups.traininggroup.application.TrainingGroupManagementPort;
import com.klabis.members.MemberId;
import com.klabis.members.infrastructure.restapi.MemberDetailsResponse;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.server.RepresentationModelProcessor;

import static com.klabis.common.ui.HalFormsSupport.klabisLinkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@MvcComponent
public class MemberTrainingGroupLinkProcessor implements RepresentationModelProcessor<EntityModel<MemberDetailsResponse>> {

    private final TrainingGroupManagementPort trainingGroupManagementPort;

    MemberTrainingGroupLinkProcessor(TrainingGroupManagementPort trainingGroupManagementPort) {
        this.trainingGroupManagementPort = trainingGroupManagementPort;
    }

    @Override
    public EntityModel<MemberDetailsResponse> process(EntityModel<MemberDetailsResponse> model) {
        MemberId memberId = new MemberId(model.getContent().id());
        trainingGroupManagementPort.findTrainingGroupOfMember(memberId)
                .ifPresent(group -> klabisLinkTo(methodOn(TrainingGroupsApi.class).getTrainingGroup(group.getId().uuid(), null))
                        .map(link -> link.withRel("trainingGroup"))
                        .ifPresent(model::add));
        return model;
    }
}
