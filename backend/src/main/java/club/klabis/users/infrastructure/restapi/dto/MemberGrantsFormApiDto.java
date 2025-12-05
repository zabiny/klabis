package club.klabis.users.infrastructure.restapi.dto;

import club.klabis.shared.config.hateoas.KlabisInputTypes;
import club.klabis.shared.config.security.ApplicationGrant;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.soabase.recordbuilder.core.RecordBuilder;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import org.springframework.hateoas.InputType;

import java.util.Collection;

/**
 * Data for form setting member grants
 */

@Schema(name = "MemberGrantsForm", description = "Data for form setting member grants")
@JsonTypeName("MemberGrantsForm")
@RecordBuilder
public record MemberGrantsFormApiDto(
        @InputType(KlabisInputTypes.CHECKBOX_INPUT_TYPE)
        @Valid
        Collection<ApplicationGrant> grants

) {

}

