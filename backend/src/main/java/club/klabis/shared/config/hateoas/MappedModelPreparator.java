package club.klabis.shared.config.hateoas;

import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;

import java.util.function.Function;

public class MappedModelPreparator<D, O> implements ModelPreparator<D, O> {
    private final Function<D, Link> selfLinkGenerator;
    private final Function<D, O> modelMaper;

    public MappedModelPreparator(Function<D, O> modelMapper, Function<D, Link> selfLinkGenerator) {
        this.selfLinkGenerator = selfLinkGenerator;
        this.modelMaper = modelMapper;
    }

    @Override
    public void addLinks(EntityModel<O> resource, D domain) {
        ModelPreparator.super.addLinks(resource, domain);

        resource.add(selfLinkGenerator.apply(domain));
    }


    @Override
    public O toResponseDto(D d) {
        return modelMaper.apply(d);
    }
}
