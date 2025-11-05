package club.klabis.shared.config.hateoas;

import club.klabis.shared.config.restapi.KlabisPrincipal;
import com.fasterxml.jackson.annotation.JsonIgnore;

public record RootModel(@JsonIgnore KlabisPrincipal klabisPrincipal) {

}
