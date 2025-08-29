package club.klabis.shared.config.ddd.forms;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.models.media.Schema;
import org.springframework.boot.jackson.JsonMixin;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonMixin(type = Schema.class)
class SchemaJsonMixin {
}
