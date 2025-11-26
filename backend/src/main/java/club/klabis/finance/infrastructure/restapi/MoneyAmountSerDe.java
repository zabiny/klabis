package club.klabis.finance.infrastructure.restapi;

import club.klabis.finance.domain.MoneyAmount;
import org.springframework.boot.jackson.JacksonComponent;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;

import java.math.BigDecimal;

@JacksonComponent
class MoneyAmountSerDe extends ValueSerializer<MoneyAmount> {
    @Override
    public void serialize(MoneyAmount value, JsonGenerator gen, SerializationContext serializers) {
        serializers.findValueSerializer(BigDecimal.class).serialize(value.amount(), gen, serializers);
    }
}
