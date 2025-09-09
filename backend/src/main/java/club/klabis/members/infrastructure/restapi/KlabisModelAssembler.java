package club.klabis.members.infrastructure.restapi;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.hateoas.PagedModel;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.hateoas.server.RepresentationModelAssembler;

public abstract class KlabisModelAssembler<T, O extends RepresentationModel<?>> implements RepresentationModelAssembler<T, O> {

    public PagedModel<O> toPagedModel(Page<T> data, Pageable pageable) {
        return PagedModel.of(data.map(this::toModel).getContent(), metadata(data));
    }

    private static PagedModel.PageMetadata metadata(Page<?> items) {
        return new PagedModel.PageMetadata(items.getSize(),
                items.getNumber(),
                items.getTotalElements(),
                items.getTotalPages());
    }


}
