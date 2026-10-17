package com.klabis.sync.infrastructure.restapi;

import com.klabis.common.mvc.MvcComponent;
import com.klabis.common.ui.ModelWithDomainPostprocessor;
import org.springframework.hateoas.EntityModel;

@MvcComponent
class SyncRecordPostprocessor extends ModelWithDomainPostprocessor<SyncRecordSummaryResponse, DescribedRecord> {

    @Override
    public void process(EntityModel<SyncRecordSummaryResponse> dtoModel, DescribedRecord described) {
        SyncEntityLinks.addTo(dtoModel, described.view().target(), described.description());
    }
}
