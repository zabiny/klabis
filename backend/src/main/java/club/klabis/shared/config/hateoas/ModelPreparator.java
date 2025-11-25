package club.klabis.shared.config.hateoas;

import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;

public interface ModelPreparator<DOMAIN, DTO> {
    DTO toResponseDto(DOMAIN domain);


    /**
     * When DOMAIN is converted to DTO, there may be some attribute cahnges. That affects how UI sees and prepares Sort in Pageable. This method can be used to convert "DTO" attribute names there into "DOMAIN" attribute names.
     *
     * @param dtoPropertyName name of the property from DTO
     * @return matching property name in Domain object
     */
    default String toDomainPropertyName(String dtoPropertyName) {
        return dtoPropertyName;
    }

    default void addLinks(EntityModel<DTO> resource, DOMAIN domain) {
    }

    default void addLinks(CollectionModel<EntityModel<DTO>> resources) {
    }
}
