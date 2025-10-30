package club.klabis.finance.infrastructure.restapi;

import club.klabis.finance.domain.MoneyAmount;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import org.springframework.boot.jackson.JsonComponent;

import java.io.IOException;
import java.math.BigDecimal;

@JsonComponent
class MoneyAmountSerDe extends JsonSerializer<MoneyAmount> {
    @Override
    public void serialize(MoneyAmount value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        serializers.findValueSerializer(BigDecimal.class).serialize(value.amount(), gen, serializers);
    }
}
