package com.klabis.sync.infrastructure.restapi;

import com.klabis.common.mvc.MvcComponent;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.PagedModel;
import org.springframework.hateoas.server.RepresentationModelProcessor;

import java.util.List;

import static com.klabis.common.ui.HalFormsSupport.klabisLinkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@MvcComponent
class SyncAttemptListPostprocessor implements RepresentationModelProcessor<PagedModel<EntityModel<SyncAttemptResponse>>> {

    @Override
    public PagedModel<EntityModel<SyncAttemptResponse>> process(PagedModel<EntityModel<SyncAttemptResponse>> model) {
        klabisLinkTo(methodOn(SyncApi.class).listSyncRecords(List.of("CONFLICT", "FAILED")))
                .ifPresent(link -> model.add(link.withRel("recordsNeedingAttention")));
        return model;
    }
}
