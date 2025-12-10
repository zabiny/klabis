package club.klabis.shared.config.hateoas;

import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;

import java.util.function.Function;

public class IdentityModelPreparator<DOMAIN> implements ModelPreparator<DOMAIN, DOMAIN> {

    private final Function<DOMAIN, Link> selfLinkGenerator;

    public IdentityModelPreparator(Function<DOMAIN, Link> selfLinkGenerator) {
        this.selfLinkGenerator = selfLinkGenerator;
    }

    @Override
    public DOMAIN toResponseDto(DOMAIN domain) {
        return domain;
    }

    @Override
    public void addLinks(EntityModel<DOMAIN> resource, DOMAIN domain) {
        ModelPreparator.super.addLinks(resource, domain);

        resource.add(selfLinkGenerator.apply(domain));
    }
}
