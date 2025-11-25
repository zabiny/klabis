package club.klabis.shared.config.hateoas;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.PagedModel;

public interface ModelAssembler<DOMAIN, DTO> {

    PagedModel<EntityModel<DTO>> toPagedResponse(Page<DOMAIN> events);

    EntityModel<DTO> toEntityResponse(DOMAIN events);

    CollectionModel<EntityModel<DTO>> toCollectionModel(Iterable<? extends DOMAIN> events);

    // can be used to translate attribute names (sort) from DTO to Domain
    Pageable toDomainPageable(Pageable dtoPageable);
}
