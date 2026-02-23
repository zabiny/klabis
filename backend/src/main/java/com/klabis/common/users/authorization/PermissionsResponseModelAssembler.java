package com.klabis.common.users.authorization;

import com.klabis.common.mvc.MvcComponent;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport;

import java.util.Set;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

/**
 * HATEOAS assembler for PermissionsResponse.
 */
@MvcComponent
class PermissionsResponseModelAssembler
        extends RepresentationModelAssemblerSupport<PermissionsResponse, PermissionsResponseModel> {

    public PermissionsResponseModelAssembler() {
        super(PermissionController.class, PermissionsResponseModel.class);
    }

    @Override
    public PermissionsResponseModel toModel(PermissionsResponse entity) {
        PermissionsResponseModel model = instantiateModel(entity);

        // Add self link
        Link selfLink = linkTo(methodOn(PermissionController.class)
                .getUserPermissions(entity.userId().uuid()))
                .withSelfRel();
        model.add(selfLink);

        return model;
    }

    @Override
    protected PermissionsResponseModel instantiateModel(PermissionsResponse entity) {
        return new PermissionsResponseModel(entity.userId(), Set.copyOf(entity.authorities()));
    }
}
