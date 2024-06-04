package club.klabis.domain.members;

import org.mapstruct.Mapper;
import org.springframework.core.convert.converter.Converter;

@Mapper(componentModel = "spring")
public class RegistrationNumberReverseConverter implements Converter<RegistrationNumber, String> {

    public String convert(RegistrationNumber number) {
        return number.toRegistrationId();
    }

}
