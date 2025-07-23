package club.klabis.members.domain;

import org.mapstruct.Mapper;
import org.mapstruct.extensions.spring.DelegatingConverter;
import org.springframework.core.convert.converter.Converter;
import org.springframework.lang.NonNull;

@Mapper(componentModel = "spring")
public class RegistrationNumberToStringConverter implements Converter<RegistrationNumber, String> {

    @DelegatingConverter
    public RegistrationNumber reverseConvert(@NonNull String number) {
        return RegistrationNumber.ofRegistrationId(number);
    }

    public String convert(@NonNull RegistrationNumber number) {
        return number.toRegistrationId();
    }
}
