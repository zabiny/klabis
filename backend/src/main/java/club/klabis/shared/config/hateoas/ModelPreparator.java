package club.klabis.shared.config.hateoas;

import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;

public interface ModelPreparator<DOMAIN, DTO> {
    DTO toResponseDto(DOMAIN domain);

    default void addLinks(EntityModel<DTO> resource, DOMAIN domain) {
    }

    default void addLinks(CollectionModel<EntityModel<DTO>> resources) {
    }
}
