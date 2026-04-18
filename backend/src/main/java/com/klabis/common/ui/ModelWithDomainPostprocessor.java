package com.klabis.common.ui;

import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.server.RepresentationModelProcessor;

public abstract class ModelWithDomainPostprocessor<T, D> implements RepresentationModelProcessor<EntityModelWithDomain<T, D>> {
    @Override
    public EntityModelWithDomain<T, D> process(EntityModelWithDomain<T, D> model) {
        process((EntityModel<T>) model, model.getDomainItem());
        return model;
    }

    public abstract void process(EntityModel<T> dtoModel, D domain);
}
