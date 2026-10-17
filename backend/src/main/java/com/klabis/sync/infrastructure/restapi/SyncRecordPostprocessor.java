package com.klabis.sync.infrastructure.restapi;

import com.klabis.common.mvc.MvcComponent;
import com.klabis.common.ui.ModelWithDomainPostprocessor;
import com.klabis.sync.application.RecordView;
import org.springframework.hateoas.EntityModel;

@MvcComponent
class SyncRecordPostprocessor extends ModelWithDomainPostprocessor<SyncRecordSummaryResponse, Described<RecordView>> {

    @Override
    public void process(EntityModel<SyncRecordSummaryResponse> dtoModel, Described<RecordView> described) {
        SyncEntityLinks.addTo(dtoModel, described.view().target(), described.description());
    }
}
