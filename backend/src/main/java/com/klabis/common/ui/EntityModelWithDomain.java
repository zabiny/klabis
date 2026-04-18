package com.klabis.common.ui;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.springframework.hateoas.EntityModel;

public class EntityModelWithDomain<T, D> extends EntityModel<T> {
    @JsonIgnore
    private final D domainItem;

    protected EntityModelWithDomain(T dto, D domainItem) {
        super(dto);
        this.domainItem = domainItem;
    }

    public D getDomainItem() {
        return domainItem;
    }
}
