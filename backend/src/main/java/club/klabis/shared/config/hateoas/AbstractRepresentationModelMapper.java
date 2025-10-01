package club.klabis.shared.config.hateoas;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.PagedModel;
import org.springframework.hateoas.server.SimpleRepresentationModelAssembler;

public abstract class AbstractRepresentationModelMapper<DOMAIN, DTO> implements SimpleRepresentationModelAssembler<DTO> {

    private PagedResourcesAssembler<DTO> pagedResourcesAssembler;

    @Autowired
    public void setPagedMapper(PagedResourcesAssembler<DTO> pagedResourcesAssembler) {
        this.pagedResourcesAssembler = pagedResourcesAssembler;
    }

    public PagedModel<EntityModel<DTO>> toPagedResponse(Page<DOMAIN> events) {
        return toPagedModel(events.map(this::toResponse));
    }

    public PagedModel<EntityModel<DTO>> toPagedModel(Page<DTO> page) {
        PagedModel<EntityModel<DTO>> result = pagedResourcesAssembler.toModel(page, this);
        addLinks(result);
        return result;
    }

    public EntityModel<DTO> toResponseModel(DOMAIN response) {
        return toModel(toResponse(response));
    }

    abstract public DTO toResponse(DOMAIN event);


}
