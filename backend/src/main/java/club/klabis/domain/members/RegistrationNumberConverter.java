package club.klabis.domain.members;

import org.mapstruct.Mapper;
import org.springframework.core.convert.converter.Converter;

@Mapper(componentModel = "spring")
public class RegistrationNumberConverter implements Converter<String, RegistrationNumber> {

    public RegistrationNumber convert(String number) {
        return RegistrationNumber.ofRegistrationId(number);
    }

}
