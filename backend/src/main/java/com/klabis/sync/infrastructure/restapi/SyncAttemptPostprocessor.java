package com.klabis.sync.infrastructure.restapi;

import com.klabis.common.mvc.MvcComponent;
import com.klabis.common.ui.ModelWithDomainPostprocessor;
import com.klabis.sync.application.AttemptView;
import org.springframework.hateoas.EntityModel;

/**
 * Every attempt links to the current state of its entity, so the actions (synchronise, resolve,
 * reset) are offered from there and never duplicated on the history.
 */
@MvcComponent
class SyncAttemptPostprocessor extends ModelWithDomainPostprocessor<SyncAttemptResponse, Described<AttemptView>> {

    @Override
    public void process(EntityModel<SyncAttemptResponse> dtoModel, Described<AttemptView> described) {
        SyncEntityLinks.addTo(dtoModel, described.view().target(), described.description());
    }
}
