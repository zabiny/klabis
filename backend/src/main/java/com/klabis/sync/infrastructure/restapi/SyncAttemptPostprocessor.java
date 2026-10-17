package com.klabis.sync.infrastructure.restapi;

import com.klabis.common.mvc.MvcComponent;
import com.klabis.common.ui.ModelWithDomainPostprocessor;
import com.klabis.sync.application.AttemptView;
import org.springframework.hateoas.EntityModel;

import static com.klabis.common.ui.HalFormsSupport.klabisLinkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

/**
 * Every attempt links to the current state of its entity, so the actions (synchronise, resolve,
 * reset) are offered from there and never duplicated on the history.
 */
@MvcComponent
class SyncAttemptPostprocessor extends ModelWithDomainPostprocessor<SyncAttemptResponse, AttemptView> {

    @Override
    public void process(EntityModel<SyncAttemptResponse> dtoModel, AttemptView view) {
        SyncEntityTypeParam entityType = SyncEntityTypeParam.fromValue(view.target().entityType().pathSegment());

        klabisLinkTo(methodOn(SyncApi.class).getSyncState(entityType, view.target().entityId()))
                .ifPresent(link -> dtoModel.add(link.withRel("syncState")));
    }
}
