package club.klabis.shared.config.hateoas;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.PagedModel;

import java.util.function.Function;

public interface ModelAssembler<DOMAIN, DTO> {

    PagedModel<EntityModel<DTO>> toPagedResponse(Page<DOMAIN> events);

    EntityModel<DTO> toEntityResponse(DOMAIN events);

    CollectionModel<EntityModel<DTO>> toCollectionModel(Iterable<? extends DOMAIN> events);

    // can be used to translate attribute names (sort) from DTO to Domain
    Pageable toDomainPageable(Pageable dtoPageable);

    static <D> ModelAssembler<D, D> identityAssembler(Function<D, Link> selfLinkGenerator, PagedResourcesAssembler<D> pagedResourcesAssembler) {
        return mappingAssembler(Function.identity(), selfLinkGenerator, pagedResourcesAssembler);
    }

    static <D, O> ModelAssembler<D, O> mappingAssembler(Function<D, O> modelMapper, Function<D, Link> selfLinkGenerator, PagedResourcesAssembler<D> pagedResourcesAssembler) {
        ModelPreparator<D, O> modelPreparator = new MappedModelPreparator<>(modelMapper, selfLinkGenerator);
        return new HalResourceAssembler<>(modelPreparator, pagedResourcesAssembler);
    }
}
