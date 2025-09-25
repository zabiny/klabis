package club.klabis.shared.config.hateoas;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.PagedModel;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.hateoas.server.RepresentationModelAssembler;

import java.util.ArrayList;
import java.util.Collection;

public abstract class AbstractRepresentationModelMapper<DOMAIN, DTO extends RepresentationModel<DTO>> implements RepresentationModelAssembler<DOMAIN, DTO> {

    private PagedResourcesAssembler<DOMAIN> pagedResourcesAssembler;

    @Autowired
    public void setPagedMapper(PagedResourcesAssembler<DOMAIN> pagedResourcesAssembler) {
        this.pagedResourcesAssembler = pagedResourcesAssembler;
    }

    public abstract DTO mapDataFromDomain(DOMAIN domain);

    public Collection<Link> createItemLinks(DOMAIN domain) {
        return new ArrayList<>();
    }

    private Collection<Link> createCollectionLinks() {
        return new ArrayList<>();
    }


    @Override
    public final DTO toModel(DOMAIN domain) {
        DTO response = mapDataFromDomain(domain);

        createItemLinks(domain).forEach(response::add);

        return response;
    }

    @Override
    public final CollectionModel<DTO> toCollectionModel(Iterable<? extends DOMAIN> entities) {
        CollectionModel<DTO> result = RepresentationModelAssembler.super.toCollectionModel(entities);

        createCollectionLinks().forEach(result::add);

        return result;
    }

    public final PagedModel<DTO> toPagedModel(Page<DOMAIN> events) {
        PagedModel<DTO> result = pagedResourcesAssembler.toModel(events, this);

        createCollectionLinks().forEach(result::add);

        return result;
    }


}
