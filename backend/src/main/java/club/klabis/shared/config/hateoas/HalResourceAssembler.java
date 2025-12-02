package club.klabis.shared.config.hateoas;


import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.PagedModel;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class HalResourceAssembler<D, T> implements ModelAssembler<D, T> {

    private final ModelPreparator<D, T> preparator;
    private final PagedResourcesAssembler<D> pagedResourcesAssembler;

    public HalResourceAssembler(ModelPreparator<D, T> preparator, PagedResourcesAssembler<D> pagedResourcesAssembler) {
        this.preparator = preparator;
        this.pagedResourcesAssembler = pagedResourcesAssembler;
    }

    @Override
    public PagedModel<EntityModel<T>> toPagedResponse(Page<D> pageOfDomain) {
        PagedModel<EntityModel<T>> result = pagedResourcesAssembler.toModel(pageOfDomain, this::toEntityResponse);
        preparator.addLinks(result);
        return result;
    }

    @Override
    public EntityModel<T> toEntityResponse(D domain) {
        EntityModel<T> response = EntityModel.of(preparator.toResponseDto(domain));
        preparator.addLinks(response, domain);
        return response;
    }

    @Override
    public CollectionModel<EntityModel<T>> toCollectionModel(Iterable<? extends D> events) {
        CollectionModel<EntityModel<T>> result = StreamSupport.stream(events.spliterator(), false) //
                .map(this::toEntityResponse)
                .collect(Collectors.collectingAndThen(Collectors.toList(), CollectionModel::of));
        preparator.addLinks(result);
        return result;
    }

    @Override
    public Pageable toDomainPageable(Pageable dtoPageable) {
        List<Sort.Order> updatedSorts = dtoPageable.getSort()
                .stream()
                .map(s -> s.withProperty(preparator.toDomainPropertyName(s.getProperty())))
                .toList();
        return PageRequest.of(dtoPageable.getPageNumber(), dtoPageable.getPageSize(), Sort.by(updatedSorts));
    }
}
