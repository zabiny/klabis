package com.klabis.sync.infrastructure.restapi;

import com.klabis.common.mvc.MvcComponent;
import com.klabis.common.ui.ModelWithDomainPostprocessor;
import org.springframework.hateoas.EntityModel;

/**
 * Every attempt links to the current state of its entity, so the actions (synchronise, resolve,
 * reset) are offered from there and never duplicated on the history.
 */
@MvcComponent
class SyncAttemptPostprocessor extends ModelWithDomainPostprocessor<SyncAttemptResponse, DescribedAttempt> {

    @Override
    public void process(EntityModel<SyncAttemptResponse> dtoModel, DescribedAttempt described) {
        SyncEntityLinks.addTo(dtoModel, described.view().target(), described.description());
    }
}
